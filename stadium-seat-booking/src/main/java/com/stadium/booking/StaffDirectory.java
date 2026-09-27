package com.stadium.booking;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Named staff accounts, stored in the same database as the bookings.
 *
 * <p>An earlier version used a single hard-coded PIN, which meant the PIN was in
 * the source, every user was anonymous, and there was no record of who had
 * looked at a customer's phone number. Accounts are persisted here instead: each
 * has a name, a PIN kept only as a salted hash, and every sign-in and refusal is
 * written to an access log.
 *
 * <p>The PIN hash is stretched with PBKDF2 rather than a single SHA-256 round, so
 * a stolen database cannot be attacked quickly with a word list.
 */
public final class StaffDirectory {
    /** Failed sign-ins allowed before an account is locked. */
    public static final int MAX_FAILED_ATTEMPTS = 5;
    /** How long an account stays locked after too many failures. */
    public static final java.time.Duration LOCKOUT = java.time.Duration.ofMinutes(15);
    /** Shortest PIN that will be accepted. */
    public static final int MINIMUM_PIN_LENGTH = 6;

    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int PBKDF2_KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** What a staff member is allowed to do. */
    public enum Role {
        /** Can see customer contact details, book, cancel and export. */
        MANAGER("Manager"),
        /** Can see bookings and occupancy but not full customer contact details. */
        CLERK("Clerk"),
        /** Can only be signed in to confirm a booking, no reports. */
        SUPERVISOR("Supervisor");

        private final String label;

        Role(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** One staff account. */
    public static final class Account {
        private final String username;
        private final String displayName;
        private final Role role;
        private final boolean mustChangePin;
        private final boolean active;
        private final int failedAttempts;
        private final Instant lockedUntil;
        private final Instant lastUsedAt;

        Account(String username, String displayName, Role role, boolean mustChangePin,
                boolean active, int failedAttempts, Instant lockedUntil, Instant lastUsedAt) {
            this.username = username;
            this.displayName = displayName;
            this.role = role;
            this.mustChangePin = mustChangePin;
            this.active = active;
            this.failedAttempts = failedAttempts;
            this.lockedUntil = lockedUntil;
            this.lastUsedAt = lastUsedAt;
        }

        public String getUsername() {
            return username;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Role getRole() {
            return role;
        }

        /** True while the account is still on its generated starting PIN. */
        public boolean mustChangePin() {
            return mustChangePin;
        }

        public boolean isActive() {
            return active;
        }

        public int getFailedAttempts() {
            return failedAttempts;
        }

        public boolean isLocked() {
            return lockedUntil != null && lockedUntil.isAfter(Instant.now());
        }

        public Instant getLockedUntil() {
            return lockedUntil;
        }

        public Instant getLastUsedAt() {
            return lastUsedAt;
        }
    }

    /** What happened when someone tried to sign in. */
    public static final class Result {
        private final Account account;
        private final String refusal;

        private Result(Account account, String refusal) {
            this.account = account;
            this.refusal = refusal;
        }

        static Result success(Account account) {
            return new Result(account, null);
        }

        static Result failure(String refusal) {
            return new Result(null, refusal);
        }

        public boolean isSuccess() {
            return refusal == null;
        }

        /** Null when the sign-in succeeded. */
        public String getRefusal() {
            return refusal;
        }

        public Account getAccount() {
            return account;
        }
    }

    private final Database database;

    public StaffDirectory(Database database) {
        this.database = database;
    }

    public StaffDirectory(Path file) {
        this(new Database(file));
    }

    // ------------------------------------------------------------------
    // Schema
    // ------------------------------------------------------------------

    public void initialize() throws IOException {
        try (java.sql.Connection connection = database.open()) {
            initialize(connection);
        } catch (SQLException exception) {
            throw new IOException("Could not prepare the staff tables", exception);
        }
    }

    private void initialize(java.sql.Connection connection) throws SQLException {
        try (java.sql.Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS staff_accounts ("
                    + "username VARCHAR(64) PRIMARY KEY, "
                    + "display_name VARCHAR(120) NOT NULL, "
                    + "pin_hash VARCHAR(128) NOT NULL, "
                    + "pin_salt VARCHAR(64) NOT NULL, "
                    + "role VARCHAR(20) NOT NULL, "
                    + "must_change_pin BOOLEAN NOT NULL, "
                    + "active BOOLEAN NOT NULL, "
                    + "failed_attempts INT NOT NULL, "
                    + "locked_until BIGINT, "
                    + "last_used_at BIGINT, "
                    + "created_at BIGINT NOT NULL)");
            // Access is attributable: who signed in, when, and what they looked at.
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS staff_access_log ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "username VARCHAR(64) NOT NULL, "
                    + "action VARCHAR(40) NOT NULL, "
                    + "detail VARCHAR(400), "
                    + "at BIGINT NOT NULL)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_access_log_at "
                    + "ON staff_access_log(at)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_access_log_user "
                    + "ON staff_access_log(username)");
        }
    }

    // ------------------------------------------------------------------
    // Accounts
    // ------------------------------------------------------------------

    /**
     * Whether the directory has any accounts yet, which is how first run is
     * detected.
     */
    public boolean isEmpty() {
        try (java.sql.Connection connection = database.open()) {
            initialize(connection);
            try (java.sql.Statement statement = connection.createStatement();
                 ResultSet results = statement.executeQuery(
                         "SELECT COUNT(*) FROM staff_accounts")) {
                return results.next() && results.getInt(1) == 0;
            }
        } catch (SQLException | IOException exception) {
            return true;
        }
    }

    /**
     * Creates an account.
     *
     * @param mustChangePin true for a generated starting PIN the owner must replace
     * @return null on success, or the reason it was refused
     */
    public String create(String username, String displayName, String pin, Role role,
                         boolean mustChangePin) {
        String cleanUsername = username == null ? "" : username.trim().toLowerCase(Locale.ENGLISH);
        if (cleanUsername.isEmpty()) {
            return "Enter a username";
        }
        if (!cleanUsername.matches("[a-z0-9._-]{3,32}")) {
            return "Username must be 3 to 32 characters, using letters, numbers, dot, "
                    + "dash or underscore";
        }
        if (displayName == null || displayName.trim().length() < 2) {
            return "Enter the staff member's name";
        }
        String pinProblem = checkPin(pin);
        if (pinProblem != null) {
            return pinProblem;
        }
        String salt = newSalt();
        String hash = hash(pin, salt);
        try (java.sql.Connection connection = database.open()) {
            initialize(connection);
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO staff_accounts (username, display_name, pin_hash, pin_salt, "
                            + "role, must_change_pin, active, failed_attempts, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, TRUE, 0, ?)")) {
                insert.setString(1, cleanUsername);
                insert.setString(2, displayName.trim());
                insert.setString(3, hash);
                insert.setString(4, salt);
                insert.setString(5, role.name());
                insert.setBoolean(6, mustChangePin);
                insert.setLong(7, Instant.now().toEpochMilli());
                insert.executeUpdate();
            }
            this.lastCreatedPin = pin;
            this.lastCreatedUsername = cleanUsername;
            log(cleanUsername, "ACCOUNT_CREATED", role.name());
            return null;
        } catch (SQLException | IOException exception) {
            return messageFor(exception, "Could not create the account");
        }
    }

    private String checkPin(String pin) {
        if (pin == null || pin.isBlank()) {
            return "Enter a PIN";
        }
        if (pin.length() < MINIMUM_PIN_LENGTH) {
            return "The PIN must be at least " + MINIMUM_PIN_LENGTH + " characters";
        }
        if (!pin.matches("[0-9]{6,12}")) {
            return "The PIN must be 6 to 12 digits";
        }
        if (isObviousPin(pin)) {
            return "That PIN is too easy to guess, choose another";
        }
        return null;
    }

    private static boolean isObviousPin(String pin) {
        String value = pin.toLowerCase(Locale.ENGLISH);
        if (value.matches("^(.)\\1+$")) {
            return true;
        }
        if (value.matches("^\\d+$")) {
            boolean ascending = true;
            boolean descending = true;
            for (int index = 1; index < value.length(); index++) {
                int difference = value.charAt(index) - value.charAt(index - 1);
                ascending &= difference == 1;
                descending &= difference == -1;
            }
            if (ascending || descending) {
                return true;
            }
        }
        return List.of("123456", "1234567", "12345678", "123456789", "1234567890",
                "654321", "111111", "000000", "121212", "112233", "696969", "131313",
                "123123", "321321", "147258", "159753").contains(value);
    }

    public List<Account> list() {
        List<Account> accounts = new ArrayList<>();
        try (java.sql.Connection connection = database.open()) {
            initialize(connection);
            try (java.sql.Statement statement = connection.createStatement();
                 ResultSet results = statement.executeQuery(
                         "SELECT username, display_name, role, must_change_pin, active, "
                                 + "failed_attempts, locked_until, last_used_at "
                                 + "FROM staff_accounts ORDER BY display_name")) {
                while (results.next()) {
                    accounts.add(read(results));
                }
            }
        } catch (SQLException | IOException exception) {
            return accounts;
        }
        return accounts;
    }

    private Account read(ResultSet results) throws SQLException {
        return new Account(
                results.getString("username"),
                results.getString("display_name"),
                Role.valueOf(results.getString("role")),
                results.getBoolean("must_change_pin"),
                results.getBoolean("active"),
                results.getInt("failed_attempts"),
                toInstant(results.getLong("locked_until")),
                toInstant(results.getLong("last_used_at")));
    }

    private static Instant toInstant(long millis) {
        return millis <= 0 ? null : Instant.ofEpochMilli(millis);
    }

    /**
     * Checks a sign-in attempt.
     *
     * <p>Every attempt is recorded, successful or not, so there is a trail of who
     * has been trying.
     */
    public Result signIn(String username, String pin) {
        String cleanUsername = username == null ? "" : username.trim().toLowerCase(Locale.ENGLISH);
        if (cleanUsername.isEmpty()) {
            log("", "SIGN_IN_NO_USERNAME", "");
            return Result.failure("Enter your username");
        }
        try (java.sql.Connection connection = database.open()) {
            initialize(connection);
            Account account = load(connection, cleanUsername);
            if (account == null) {
                log(cleanUsername, "SIGN_IN_UNKNOWN_USER", "");
                return Result.failure("That username is not recognised");
            }
            if (!account.isActive()) {
                log(cleanUsername, "SIGN_IN_INACTIVE", "");
                return Result.failure("That account has been deactivated");
            }
            if (account.isLocked()) {
                log(cleanUsername, "SIGN_IN_LOCKED", "");
                return Result.failure("Too many wrong attempts. Try again in "
                        + minutesUntil(account.getLockedUntil()) + " minutes");
            }
            String salt = saltFor(connection, cleanUsername);
            if (pin == null || pin.isEmpty()
                    || !storedHash(connection, cleanUsername).equals(hash(pin, salt))) {
                return failure(connection, cleanUsername);
            }
            try (PreparedStatement clear = connection.prepareStatement(
                    "UPDATE staff_accounts SET failed_attempts = 0, locked_until = NULL, "
                            + "last_used_at = ? WHERE username = ?")) {
                clear.setLong(1, Instant.now().toEpochMilli());
                clear.setString(2, cleanUsername);
                clear.executeUpdate();
            }
            log(cleanUsername, "SIGN_IN", account.getRole().name());
            return Result.success(new Account(account.getUsername(), account.getDisplayName(),
                    account.getRole(), account.mustChangePin(), true, 0, null, Instant.now()));
        } catch (SQLException | IOException exception) {
            return Result.failure("The staff list could not be read. Please try again.");
        }
    }

    private Result failure(java.sql.Connection connection, String username) throws SQLException {
        int attempts = failedAttempts(connection, username) + 1;
        Instant lockedUntil = attempts >= MAX_FAILED_ATTEMPTS
                ? Instant.now().plus(LOCKOUT) : null;
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE staff_accounts SET failed_attempts = ?, locked_until = ? "
                        + "WHERE username = ?")) {
            update.setInt(1, attempts);
            if (lockedUntil == null) {
                update.setNull(2, java.sql.Types.BIGINT);
            } else {
                update.setLong(2, lockedUntil.toEpochMilli());
            }
            update.setString(3, username);
            update.executeUpdate();
        }
        log(username, "SIGN_IN_WRONG_PIN", "attempt " + attempts);
        if (lockedUntil != null) {
            return Result.failure("Too many wrong attempts. This account is locked for "
                    + LOCKOUT.toMinutes() + " minutes");
        }
        int remaining = MAX_FAILED_ATTEMPTS - attempts;
        return Result.failure(remaining <= 2
                ? "That PIN is not correct. " + remaining + " attempt"
                        + (remaining == 1 ? "" : "s") + " left"
                : "That PIN is not correct");
    }

    private static long minutesUntil(Instant instant) {
        if (instant == null) {
            return 0;
        }
        return Math.max(1, java.time.Duration.between(Instant.now(), instant).toMinutes() + 1);
    }

    private Account load(java.sql.Connection connection, String username) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT username, display_name, role, must_change_pin, active, "
                        + "failed_attempts, locked_until, last_used_at FROM staff_accounts "
                        + "WHERE username = ?")) {
            select.setString(1, username);
            try (ResultSet results = select.executeQuery()) {
                return results.next() ? read(results) : null;
            }
        }
    }

    private int failedAttempts(java.sql.Connection connection, String username)
            throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT failed_attempts FROM staff_accounts WHERE username = ?")) {
            select.setString(1, username);
            try (ResultSet results = select.executeQuery()) {
                return results.next() ? results.getInt(1) : 0;
            }
        }
    }

    private String saltFor(java.sql.Connection connection, String username) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT pin_salt FROM staff_accounts WHERE username = ?")) {
            select.setString(1, username);
            try (ResultSet results = select.executeQuery()) {
                return results.next() ? results.getString(1) : "";
            }
        }
    }

    private String storedHash(java.sql.Connection connection, String username)
            throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT pin_hash FROM staff_accounts WHERE username = ?")) {
            select.setString(1, username);
            try (ResultSet results = select.executeQuery()) {
                return results.next() ? results.getString(1) : "";
            }
        }
    }

    /**
     * Replaces an account's PIN.
     *
     * @return null on success, or the reason it was refused
     */
    public String changePin(String username, String currentPin, String newPin) {
        String cleanUsername = username == null ? "" : username.trim().toLowerCase(Locale.ENGLISH);
        Result check = signIn(cleanUsername, currentPin);
        if (!check.isSuccess()) {
            return "Your current PIN is not correct";
        }
        if (newPin == null || newPin.isEmpty()) {
            return "Enter a new PIN";
        }
        if (newPin.equals(currentPin)) {
            return "The new PIN must be different from the old one";
        }
        String problem = checkPin(newPin);
        if (problem != null) {
            return problem;
        }
        String salt = newSalt();
        try (java.sql.Connection connection = database.open()) {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE staff_accounts SET pin_hash = ?, pin_salt = ?, "
                            + "must_change_pin = FALSE, failed_attempts = 0, locked_until = NULL "
                            + "WHERE username = ?")) {
                update.setString(1, hash(newPin, salt));
                update.setString(2, salt);
                update.setString(3, cleanUsername);
                update.executeUpdate();
            }
            log(cleanUsername, "PIN_CHANGED", "");
            return null;
        } catch (SQLException | IOException exception) {
            return "The new PIN could not be saved. Please try again.";
        }
    }

    /**
     * Sets another account's PIN without knowing the old one, for a manager who
     * has unlocked a forgotten account.
     */
    public String resetPin(String username, String newPin) {
        String cleanUsername = username == null ? "" : username.trim().toLowerCase(Locale.ENGLISH);
        String problem = checkPin(newPin);
        if (problem != null) {
            return problem;
        }
        String salt = newSalt();
        try (java.sql.Connection connection = database.open()) {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE staff_accounts SET pin_hash = ?, pin_salt = ?, "
                            + "must_change_pin = FALSE, failed_attempts = 0, locked_until = NULL "
                            + "WHERE username = ?")) {
                update.setString(1, hash(newPin, salt));
                update.setString(2, salt);
                update.setString(3, cleanUsername);
                if (update.executeUpdate() == 0) {
                    return "That username is not recognised";
                }
            }
            log(cleanUsername, "PIN_RESET", "");
            return null;
        } catch (SQLException | IOException exception) {
            return "The PIN could not be reset. Please try again.";
        }
    }

    /** Activates or deactivates an account. */
    public String setActive(String username, boolean active) {
        String cleanUsername = username == null ? "" : username.trim().toLowerCase(Locale.ENGLISH);
        try (java.sql.Connection connection = database.open()) {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE staff_accounts SET active = ? WHERE username = ?")) {
                update.setBoolean(1, active);
                update.setString(2, cleanUsername);
                if (update.executeUpdate() == 0) {
                    return "That username is not recognised";
                }
            }
            log(cleanUsername, active ? "ACCOUNT_ENABLED" : "ACCOUNT_DISABLED", "");
            return null;
        } catch (SQLException | IOException exception) {
            return "The account could not be updated. Please try again.";
        }
    }

    /**
     * A random starting PIN, generated rather than shipped as a default, so there
     * is no PIN in the source and no value every installation shares.
     */
    public static String startingPin() {
        StringBuilder pin = new StringBuilder();
        for (int digit = 0; digit < 8; digit++) {
            pin.append(RANDOM.nextInt(10));
        }
        String value = pin.toString();
        // A generated PIN can still land on something guessable, so nudge it.
        return isObviousPin(value) ? startingPin() : value;
    }

    private volatile String lastCreatedPin;

    /**
     * The PIN given to an account, so it can be shown once.
     *
     * @return the PIN, or null if the account did not just get one
     */
    public String lastCreatedPin(String username) {
        return username != null && username.equalsIgnoreCase(lastCreatedUsername)
                ? lastCreatedPin : null;
    }

    private volatile String lastCreatedUsername;

    /** Forgets the PIN once it has been shown, so it cannot be read again. */
    public void clearLastCreatedPin() {
        lastCreatedPin = null;
        lastCreatedUsername = null;
    }

    /** Changes an account's role. */
    public String changeRole(String username, Role role) {
        String cleanUsername = username == null ? "" : username.trim().toLowerCase(Locale.ENGLISH);
        if (role == null) {
            return "Choose a role";
        }
        try (java.sql.Connection connection = database.open()) {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE staff_accounts SET role = ? WHERE username = ?")) {
                update.setString(1, role.name());
                update.setString(2, cleanUsername);
                if (update.executeUpdate() == 0) {
                    return "That username is not recognised";
                }
            }
            log(cleanUsername, "ROLE_CHANGED", role.name());
            return null;
        } catch (SQLException | IOException exception) {
            return "The role could not be changed. Please try again.";
        }
    }

    // ------------------------------------------------------------------
    // Access log
    // ------------------------------------------------------------------

    /** Records that the signed-in staff member looked at something. */
    public void log(String username, String action, String detail) {
        try (java.sql.Connection connection = database.open()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO staff_access_log (username, action, detail, at) "
                            + "VALUES (?, ?, ?, ?)")) {
                insert.setString(1, username == null ? "" : username);
                insert.setString(2, action);
                insert.setString(3, detail == null ? "" : detail);
                insert.setLong(4, Instant.now().toEpochMilli());
                insert.executeUpdate();
            }
        } catch (SQLException | IOException exception) {
            // A missing log entry must never stop the application working.
        }
    }

    /** One row of the access log, newest first. */
    public static final class AccessEntry {
        private final String username;
        private final String action;
        private final String detail;
        private final Instant at;

        AccessEntry(String username, String action, String detail, Instant at) {
            this.username = username;
            this.action = action;
            this.detail = detail;
            this.at = at;
        }

        public String getUsername() {
            return username;
        }

        public String getAction() {
            return action;
        }

        public String getDetail() {
            return detail;
        }

        public Instant getAt() {
            return at;
        }
    }

    /** The most recent entries, newest first. */
    public List<AccessEntry> recentAccess(int limit) {
        List<AccessEntry> entries = new ArrayList<>();
        try (java.sql.Connection connection = database.open()) {
            initialize(connection);
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT username, action, detail, at FROM staff_access_log "
                            + "ORDER BY at DESC LIMIT ?")) {
                select.setInt(1, Math.max(1, limit));
                try (ResultSet results = select.executeQuery()) {
                    while (results.next()) {
                        entries.add(new AccessEntry(results.getString("username"),
                                results.getString("action"), results.getString("detail"),
                                Instant.ofEpochMilli(results.getLong("at"))));
                    }
                }
            }
        } catch (SQLException | IOException exception) {
            return entries;
        }
        return entries;
    }

    // ------------------------------------------------------------------
    // Password hashing
    // ------------------------------------------------------------------

    private static String newSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * A stretched, salted hash. PBKDF2 makes each guess expensive, so a stolen
     * database is not a quick dictionary attack away from every PIN.
     */
    private static String hash(String value, String salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(value.toCharArray(),
                    salt.getBytes(StandardCharsets.UTF_8), PBKDF2_ITERATIONS, PBKDF2_KEY_BITS);
            return Base64.getEncoder().encodeToString(factory.generateSecret(spec).getEncoded());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("PBKDF2 is required but unavailable", exception);
        }
    }

    private static String messageFor(Exception exception, String fallback) {
        String text = exception.getMessage();
        return text == null || text.isBlank() ? fallback : fallback + ": " + text;
    }
}
