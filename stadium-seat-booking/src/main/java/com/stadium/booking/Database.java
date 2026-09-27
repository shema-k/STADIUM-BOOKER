package com.stadium.booking;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Shared access to the embedded H2 file.
 *
 * <p>Bookings and staff accounts live in the same file, so the connection
 * plumbing is here rather than duplicated. This is also the one place that knows
 * whether the file needs a password, because whether one is required has to be
 * read from a sidecar file: once a database is protected, nothing inside it can
 * be opened to find out.
 */
public final class Database implements AutoCloseable {
    /** Appended to the database file name to record that a password is required. */
    public static final String ENCRYPTION_MARKER_SUFFIX = ".encrypted";

    private final Path file;
    private final String jdbcUrl;
    private char[] password;

    public Database(Path file) {
        this(file, null);
    }

    /**
     * @param password the database password, or null for an unencrypted file
     */
    public Database(Path file, char[] password) {
        this.file = file;
        this.password = password == null ? null : password.clone();
        this.jdbcUrl = buildUrl(file, this.password);
    }

    private static String buildUrl(Path databaseFile, char[] password) {
        if (databaseFile == null) {
            return null;
        }
        String path = databaseFile.toAbsolutePath().toString();
        if (path.endsWith(".dat")) {
            path = path.substring(0, path.length() - 4);
        }
        StringBuilder url = new StringBuilder("jdbc:h2:file:")
                .append(path.replace('\\', '/'))
                .append(";DB_CLOSE_ON_EXIT=FALSE");
        if (password != null && password.length > 0) {
            url.append(";PASSWORD=").append(new String(password));
        }
        return url.toString();
    }

    public Path getFile() {
        return file;
    }

    public boolean isEncrypted() {
        return password != null && password.length > 0;
    }

    /**
     * The sidecar file recording that this database needs a password to open.
     */
    public static Path markerFor(Path databaseFile) {
        return databaseFile.resolveSibling(databaseFile.getFileName() + ENCRYPTION_MARKER_SUFFIX);
    }

    /**
     * Whether the file at this path is marked as needing a password.
     *
     * <p>Checked before opening, because an encrypted file cannot be opened
     * without the password and so cannot report its own state.
     */
    public static boolean requiresPassword(Path databaseFile) {
        return databaseFile != null && Files.exists(markerFor(databaseFile));
    }

    public Connection open() throws IOException, SQLException {
        if (jdbcUrl == null) {
            throw new IOException("No booking database path configured");
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Connection connection = DriverManager.getConnection(jdbcUrl, "sa",
                password == null ? "" : new String(password));
        if (password != null) {
            // H2 only applies the password on first open, so every later connection
            // would otherwise create a second, unprotected database beside the first.
            try (java.sql.Statement statement = connection.createStatement()) {
                statement.execute("SET PASSWORD " + quote(new String(password)));
            } catch (SQLException exception) {
                // An already-encrypted database refuses this, which is fine and expected.
            }
        }
        return connection;
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    /** Forgets the password held in memory. */
    @Override
    public void close() {
        if (password != null) {
            java.util.Arrays.fill(password, '\0');
            password = null;
        }
    }
}
