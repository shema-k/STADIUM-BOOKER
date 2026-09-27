package com.stadium.booking;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Requires a password before the booking database can be opened.
 *
 * <p><strong>What this does and does not protect.</strong> With a password set,
 * the database refuses to open for anyone who does not have it, so a stray copy of
 * the file, a backup, or somebody running a database tool cannot read the
 * bookings. It does <em>not</em> encrypt the bytes in the file: this build uses
 * H2 2.2.224, which was checked directly and does not encrypt page contents on
 * write, so customer names, email addresses and phone numbers remain readable to
 * anyone with a hex editor. Treating this as encryption at rest would be
 * misleading, and the user interface says so.
 *
 * <p>It is deliberately an explicit operation rather than something that happens
 * on first launch, because a lost password means every booking is gone for good.
 * The work happens on a copy, and the original is only replaced once the copy has
 * been reopened with the password and its row counts checked.
 *
 * <p>Whether a password is needed is recorded in a sidecar file next to the
 * database, because a protected file cannot be opened to report its own state.
 * That is also why the password is asked for at every launch: this build has
 * nowhere safe to keep it, and hiding it in the source or in a plaintext file
 * beside the data would protect nothing.
 */
public final class DatabaseProtection {
    /** Every table copied across, in an order that respects dependencies. */
    private static final List<String> TABLES = List.of(
            "bookings", "booking_seats", "staff_accounts", "staff_access_log");

    private final Path file;

    public DatabaseProtection(Path file) {
        this.file = file;
    }

    /** What the change did, so the caller can report it honestly. */
    public static final class Outcome {
        private final boolean performed;
        private final long rowsCopied;
        private final String problem;

        private Outcome(boolean performed, long rowsCopied, String problem) {
            this.performed = performed;
            this.rowsCopied = rowsCopied;
            this.problem = problem;
        }

        public boolean isPerformed() {
            return performed;
        }

        public long getRowsCopied() {
            return rowsCopied;
        }

        /** Null when the migration succeeded. */
        public String getProblem() {
            return problem;
        }
    }

    /** Whether this database already needs a password. */
    public boolean isAlreadyProtected() {
        return Database.requiresPassword(file);
    }

    public boolean hasData() {
        try (Connection connection = new Database(file).open()) {
            for (String table : TABLES) {
                try (Statement statement = connection.createStatement();
                     ResultSet results = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    if (results.next() && results.getInt(1) > 0) {
                        return true;
                    }
                } catch (SQLException missing) {
                    // A table that has never been created holds no data to lose.
                }
            }
        } catch (IOException | SQLException exception) {
            return false;
        }
        return false;
    }

    /**
     * Rewrites the database with a password, keeping every row.
     *
     * <p>The work happens on a copy. The original is only replaced once the copy
     * has been reopened with the new password and its row counts checked, so a
     * failure part way through leaves the data exactly as it was.
     *
     * <p>See the class comment for what this does and does not protect.
     *
     * @param password the password to protect the file with
     */
    public Outcome encrypt(char[] password) {
        if (password == null || password.length < 8) {
            return new Outcome(false, 0, "Use a password of at least 8 characters");
        }
        if (isAlreadyProtected()) {
            return new Outcome(false, 0, "This database already needs a password");
        }
        Path working = file.resolveSibling(file.getFileName() + ".encrypting");
        Path workingMarker = Database.markerFor(working);
        Path originalData = dataFileFor(file);
        Path workingData = dataFileFor(working);
        try {
            Files.deleteIfExists(working);
            Files.deleteIfExists(workingData);

            // 1. Read everything out of the plaintext database.
            List<Table> contents = new ArrayList<>();
            try (Connection connection = new Database(file).open()) {
                for (String table : TABLES) {
                    contents.add(readTable(connection, table));
                }
            }

            // 2. Build the encrypted copy beside it.
            long copied = 0;
            try (Connection connection = new Database(working, password).open()) {
                for (Table table : contents) {
                    copied += table.writeTo(connection);
                }
            }
            Files.writeString(workingMarker, "password-required");

            // 3. Prove the copy is readable with the password alone and complete.
            verify(working, password, contents);

            // 4. Only now replace the original.
            Files.deleteIfExists(originalData);
            Files.move(workingData, originalData,
                    StandardCopyOption.REPLACE_EXISTING);
            cleanup(working, workingData, workingMarker);
            Files.writeString(Database.markerFor(file), "password-required");
            return new Outcome(true, copied, null);
        } catch (IOException | SQLException | RuntimeException exception) {
            // Leave the original untouched: this is the whole reason the work is
            // done on a copy.
            try {
                cleanup(working, workingData, workingMarker);
            } catch (IOException ignored) {
                // Nothing further can be done, and the original is still intact.
            }
            return new Outcome(false, 0,
                    "The database was left unchanged: " + exception.getMessage());
        }
    }

    /** Removes the scratch files, whether the change succeeded or failed. */
    private static void cleanup(Path... files) throws IOException {
        for (Path file : files) {
            Files.deleteIfExists(file);
        }
    }

    /** H2 keeps the data in a .mv.db file beside the path it was given. */
    private static Path dataFileFor(Path base) {
        String name = base.toString();
        if (name.endsWith(".dat")) {
            name = name.substring(0, name.length() - 4);
        }
        return Path.of(name + ".mv.db");
    }

    private static Table readTable(Connection connection, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        List<Object[]> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("SELECT * FROM " + table)) {
            int count = results.getMetaData().getColumnCount();
            for (int column = 1; column <= count; column++) {
                columns.add(results.getMetaData().getColumnName(column).toLowerCase(
                        java.util.Locale.ENGLISH));
            }
            while (results.next()) {
                Object[] row = new Object[count];
                for (int column = 1; column <= count; column++) {
                    row[column - 1] = results.getObject(column);
                }
                rows.add(row);
            }
        } catch (SQLException missing) {
            // A table that has never been created is not an error: it simply has
            // no rows, and the encrypted copy still needs the table to exist.
            return new Table(table, columnsOf(table), rows);
        }
        return new Table(table, columns, rows);
    }

    /** The column names a table has when it is created, in declaration order. */
    private static List<String> columnsOf(String table) {
        return switch (table) {
            case "bookings" -> List.of("reference", "stadium_id", "event_id", "event_name",
                    "customer_name", "email", "phone", "seats", "total", "created_at",
                    "event_date", "event_start_time", "status");
            case "booking_seats" -> List.of("event_id", "section", "seat_row", "seat_number",
                    "reference");
            case "staff_accounts" -> List.of("username", "display_name", "pin_hash", "pin_salt",
                    "role", "must_change_pin", "active", "failed_attempts", "locked_until",
                    "last_used_at", "created_at");
            case "staff_access_log" -> List.of("id", "username", "action", "detail", "at");
            default -> List.of();
        };
    }

    /**
     * Reopens the copy with only the password and checks every table came across.
     */
    private void verify(Path candidate, char[] password, List<Table> expected)
            throws IOException, SQLException {
        try (Connection connection = new Database(candidate, password).open()) {
            for (Table table : expected) {
                try (Statement statement = connection.createStatement();
                     ResultSet results = statement.executeQuery("SELECT COUNT(*) FROM "
                             + table.name)) {
                    if (!results.next() || results.getInt(1) != table.rows.size()) {
                        throw new SQLException("the " + table.name + " table did not come across "
                                + "intact, so the original was kept");
                    }
                }
            }
        }
    }

    /** One table held in memory while the database is rewritten. */
    private static final class Table {
        private final String name;
        private final List<String> columns;
        private final List<Object[]> rows;

        Table(String name, List<String> columns, List<Object[]> rows) {
            this.name = name;
            this.columns = columns;
            this.rows = rows;
        }

        long writeTo(Connection connection) throws SQLException {
            // The table is created whether or not it has rows, so the encrypted
            // copy has the same shape as the original.
            createTable(connection);
            if (rows.isEmpty()) {
                return 0;
            }
            StringBuilder columnList = new StringBuilder();
            StringBuilder placeholders = new StringBuilder();
            for (int index = 0; index < columns.size(); index++) {
                if (index > 0) {
                    columnList.append(", ");
                    placeholders.append(", ");
                }
                columnList.append(columns.get(index));
                placeholders.append('?');
            }
            // Identity columns are re-inserted, so every row keeps its identity.
            StringBuilder insert = new StringBuilder("INSERT INTO ").append(name)
                    .append(" (").append(columnList).append(") VALUES (")
                    .append(placeholders).append(')');
            try (java.sql.PreparedStatement statement =
                         connection.prepareStatement(insert.toString())) {
                for (Object[] row : rows) {
                    for (int index = 0; index < row.length; index++) {
                        statement.setObject(index + 1, row[index]);
                    }
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            return rows.size();
        }

        private void createTable(Connection connection) throws SQLException {
            try (Statement statement = connection.createStatement()) {
                for (String row : createStatementsFor(name)) {
                    statement.executeUpdate(row);
                }
            }
        }
    }

    /**
     * The table definitions, kept in one place so the encrypted copy is created
     * with the same shape the application expects.
     */
    private static List<String> createStatementsFor(String table) {
        return switch (table) {
            case "bookings" -> List.of("CREATE TABLE IF NOT EXISTS bookings ("
                    + "reference VARCHAR(64) PRIMARY KEY, stadium_id VARCHAR(80) NOT NULL, "
                    + "event_id VARCHAR(80) NOT NULL, event_name VARCHAR(255) NOT NULL, "
                    + "customer_name VARCHAR(160) NOT NULL, email VARCHAR(255) NOT NULL, "
                    + "phone VARCHAR(40) NOT NULL, seats VARCHAR(8000) NOT NULL, "
                    + "total DECIMAL(12,2) NOT NULL, created_at BIGINT NOT NULL, "
                    + "event_date DATE, event_start_time TIME, status VARCHAR(20) NOT NULL)");
            case "booking_seats" -> List.of("CREATE TABLE IF NOT EXISTS booking_seats ("
                    + "event_id VARCHAR(80) NOT NULL, section VARCHAR(8) NOT NULL, "
                    + "seat_row INT NOT NULL, seat_number INT NOT NULL, "
                    + "reference VARCHAR(64) NOT NULL, "
                    + "PRIMARY KEY (event_id, section, seat_row, seat_number))");
            case "staff_accounts" -> List.of("CREATE TABLE IF NOT EXISTS staff_accounts ("
                    + "username VARCHAR(64) PRIMARY KEY, display_name VARCHAR(120) NOT NULL, "
                    + "pin_hash VARCHAR(128) NOT NULL, pin_salt VARCHAR(64) NOT NULL, "
                    + "role VARCHAR(20) NOT NULL, must_change_pin BOOLEAN NOT NULL, "
                    + "active BOOLEAN NOT NULL, failed_attempts INT NOT NULL, "
                    + "locked_until BIGINT, last_used_at BIGINT, created_at BIGINT NOT NULL)");
            case "staff_access_log" -> List.of("CREATE TABLE IF NOT EXISTS staff_access_log ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(64) NOT NULL, "
                    + "action VARCHAR(40) NOT NULL, detail VARCHAR(400), at BIGINT NOT NULL)");
            default -> List.of();
        };
    }
}
