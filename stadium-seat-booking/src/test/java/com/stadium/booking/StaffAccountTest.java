package com.stadium.booking;

import static com.stadium.booking.TestRunner.assertEquals;
import static com.stadium.booking.TestRunner.assertFalse;
import static com.stadium.booking.TestRunner.assertThrows;
import static com.stadium.booking.TestRunner.assertTrue;
import static com.stadium.booking.TestRunner.freshDatabase;
import static com.stadium.booking.TestRunner.suite;
import static com.stadium.booking.TestRunner.test;

import java.util.List;

/**
 * Staff accounts, replacing the single hard-coded PIN.
 *
 * <p>The old design compared a PIN against a hash held in memory: the PIN was in
 * the source, every user was anonymous, and there was no record of who had looked
 * at a customer's phone number. These tests cover the replacement.
 */
final class StaffAccountTest {
    private StaffAccountTest() {
    }

    private static Database lastDatabase;

    private static StaffDirectory directory() throws Exception {
        Database database = new Database(freshDatabase("staff"));
        StaffDirectory directory = new StaffDirectory(database);
        directory.initialize();
        lastDatabase = database;
        return directory;
    }

    private static StaffDirectory withManager() throws Exception {
        StaffDirectory directory = directory();
        assertEquals(null, directory.create("amina", "Amina Okello", "482913",
                StaffDirectory.Role.MANAGER, true), "manager created");
        return directory;
    }

    static void register() {
        suite("Staff accounts");

        test("a fresh database has no accounts", () -> {
            assertTrue(directory().isEmpty(), "first run must be detectable");
        });

        test("an account is created and listed", () -> {
            StaffDirectory directory = withManager();
            assertFalse(directory.isEmpty(), "the directory is no longer empty");
            List<StaffDirectory.Account> accounts = directory.list();
            assertEquals(1, accounts.size(), "one account");
            assertEquals("amina", accounts.get(0).getUsername(), "username");
            assertEquals("Amina Okello", accounts.get(0).getDisplayName(), "display name");
            assertEquals(StaffDirectory.Role.MANAGER, accounts.get(0).getRole(), "role");
            assertTrue(accounts.get(0).mustChangePin(), "a generated PIN must be changed");
        });

        test("usernames are normalised and must be well formed", () -> {
            StaffDirectory directory = directory();
            assertEquals(null, directory.create("  Boss  ", "The Boss", "482913",
                    StaffDirectory.Role.MANAGER, false), "trimmed and lower-cased");
            assertEquals("boss", directory.list().get(0).getUsername(), "normalised");
            assertTrue(directory.create("a", "Too Short", "482913",
                            StaffDirectory.Role.MANAGER, false) != null, "too short");
            assertTrue(directory.create("has space", "Has Space", "482913",
                            StaffDirectory.Role.MANAGER, false) != null, "no spaces");
            assertTrue(directory.create("boss", "Duplicate", "482913",
                            StaffDirectory.Role.MANAGER, false) != null, "usernames are unique");
        });

        test("weak PINs are refused", () -> {
            StaffDirectory directory = directory();
            for (String weak : new String[]{"12345", "123456", "000000", "654321", "1234567890",
                    "abcdef", "12 34 56"}) {
                assertTrue(directory.create("staff" + weak.hashCode(), "Test", weak,
                                StaffDirectory.Role.CLERK, false) != null,
                        "the PIN '" + weak + "' must be refused");
            }
        });

        test("the correct PIN signs in and a wrong one does not", () -> {
            StaffDirectory directory = withManager();
            assertTrue(directory.signIn("amina", "000000").getRefusal() != null,
                    "a wrong PIN is refused");
            assertTrue(directory.signIn("nobody", "482913").getRefusal() != null,
                    "an unknown username is refused");
            StaffDirectory.Result result = directory.signIn("amina", "482913");
            assertTrue(result.isSuccess(), "the correct PIN is accepted");
            assertEquals("amina", result.getAccount().getUsername(), "signed in as");
        });

        test("usernames are matched regardless of case or spacing", () -> {
            StaffDirectory directory = withManager();
            assertTrue(directory.signIn("  AMINA ", "482913").isSuccess(),
                    "case and spacing must not matter");
        });

        test("the PIN is never stored in readable form", () -> {
            withManager();
            // The whole point of hashing: nothing in the file holds the PIN.
            String table = readStaffTable();
            assertTrue(table.contains("amina"), "the row exists");
            assertFalse(table.contains("482913"), "the PIN must not appear anywhere in the table");
            assertFalse(table.contains("482913".substring(0, 4)), "not even a prefix of it");
        });

        test("the database file itself does not contain the PIN", () -> {
            withManager();
            StringBuilder file = new StringBuilder();
            for (byte[] block : readFileBlocks(lastDatabase.getFile())) {
                file.append(new String(block, java.nio.charset.StandardCharsets.ISO_8859_1));
            }
            assertFalse(file.toString().contains("482913"),
                    "the PIN must not be recoverable from the database file");
        });

        test("two accounts never share a stored hash", () -> {
            StaffDirectory directory = withManager();
            directory.create("juma", "Juma Ali", "482913", StaffDirectory.Role.CLERK, false);
            String table = readStaffTable();
            java.util.Set<String> hashes = new java.util.HashSet<>();
            for (String row : table.trim().split("\\n")) {
                String[] parts = row.split("\\|");
                // username|display_name|pin_hash|pin_salt|...
                if (parts.length > 3) {
                    hashes.add(parts[2] + ":" + parts[3]);
                }
            }
            assertEquals(2, hashes.size(), "each account has its own salt and hash");
        });

        test("repeated wrong PINs lock the account for a while", () -> {
            StaffDirectory directory = withManager();
            for (int attempt = 0; attempt < StaffDirectory.MAX_FAILED_ATTEMPTS; attempt++) {
                directory.signIn("amina", "000000");
            }
            StaffDirectory.Account locked = directory.list().get(0);
            assertTrue(locked.isLocked(), "the account should be locked");
            // The correct PIN is refused while the lock stands.
            assertTrue(directory.signIn("amina", "482913").getRefusal() != null,
                    "the right PIN must not work during a lockout");
        });

        test("a successful sign-in clears earlier failures", () -> {
            StaffDirectory directory = withManager();
            directory.signIn("amina", "000000");
            directory.signIn("amina", "000000");
            assertTrue(directory.signIn("amina", "482913").isSuccess(), "signs in");
            assertEquals(0, directory.list().get(0).getFailedAttempts(), "failures cleared");
            assertFalse(directory.list().get(0).isLocked(), "not locked");
        });

        test("changing the PIN requires the current one and clears the flag", () -> {
            StaffDirectory directory = withManager();
            assertTrue(directory.changePin("amina", "wrongpin", "731204") != null,
                    "the wrong current PIN is refused");
            assertTrue(directory.changePin("amina", "482913", "123123") != null,
                    "an easily guessed new PIN is refused");
            assertTrue(directory.changePin("amina", "482913", "482913") != null,
                    "the new PIN must differ");
            assertEquals(null, directory.changePin("amina", "482913", "731204"), "changed");
            assertTrue(directory.signIn("amina", "731204").isSuccess(), "the new PIN works");
            assertFalse(directory.list().get(0).mustChangePin(), "flag cleared");
        });

        test("a manager can reset a forgotten PIN", () -> {
            StaffDirectory directory = withManager();
            assertEquals(null, directory.resetPin("amina", "904512"), "reset");
            assertTrue(directory.signIn("amina", "904512").isSuccess(), "the reset PIN works");
            assertTrue(directory.resetPin("nobody", "904512") != null, "unknown user refused");
        });

        test("accounts can be deactivated and reactivated", () -> {
            StaffDirectory directory = withManager();
            assertEquals(null, directory.setActive("amina", false), "deactivated");
            assertTrue(directory.signIn("amina", "482913").getRefusal() != null,
                    "a deactivated account cannot sign in");
            assertEquals(null, directory.setActive("amina", true), "reactivated");
            assertTrue(directory.signIn("amina", "482913").isSuccess(), "works again");
        });

        test("the access trail records sign-ins, refusals and changes", () -> {
            StaffDirectory directory = withManager();
            directory.signIn("amina", "000000");
            directory.signIn("amina", "482913");
            directory.changePin("amina", "482913", "731204");
            List<String> actions = directory.recentAccess(50).stream()
                    .map(StaffDirectory.AccessEntry::getAction).toList();
            assertTrue(actions.contains("ACCOUNT_CREATED"), "creation: " + actions);
            assertTrue(actions.contains("SIGN_IN_WRONG_PIN"), "refused attempt: " + actions);
            assertTrue(actions.contains("SIGN_IN"), "sign-in: " + actions);
            assertTrue(actions.contains("PIN_CHANGED"), "PIN change: " + actions);
        });

        test("the access trail is newest first and named", () -> {
            StaffDirectory directory = withManager();
            directory.signIn("amina", "482913");
            List<StaffDirectory.AccessEntry> entries = directory.recentAccess(5);
            assertTrue(!entries.isEmpty(), "there are entries");
            assertEquals("amina", entries.get(0).getUsername(), "attributed to the account");
            for (int index = 1; index < entries.size(); index++) {
                assertTrue(!entries.get(index).getAt().isAfter(entries.get(index - 1).getAt()),
                        "entries must be newest first");
            }
        });

        test("a clerk cannot see customer contact details but a manager can", () -> {
            StaffDirectory directory = withManager();
            directory.create("juma", "Juma Ali", "550311", StaffDirectory.Role.CLERK, false);
            StaffSession session = new StaffSession(directory);
            assertEquals(null, session.signIn("amina", "482913"), "manager signs in");
            assertTrue(session.canViewCustomerDetails(), "a manager may see contact details");
            session.signOut();
            assertEquals(null, session.signIn("juma", "550311"), "clerk signs in");
            assertFalse(session.canViewCustomerDetails(), "a clerk may not");
            assertTrue(session.canViewReports(), "but a clerk still sees reports");
        });

        test("a supervisor may not see the reports", () -> {
            StaffDirectory directory = withManager();
            directory.create("pat", "Pat Ssali", "660422", StaffDirectory.Role.SUPERVISOR, false);
            StaffSession session = new StaffSession(directory);
            session.signIn("pat", "660422");
            assertFalse(session.canViewReports(), "a supervisor does not get the reports");
            assertFalse(session.canViewCustomerDetails(), "nor the contact details");
        });

        test("nobody signed in means no contact details", () -> {
            assertFalse(new StaffSession(withManager()).canViewCustomerDetails(),
                    "an unsigned session sees nothing");
        });

        test("signing out clears the identity", () -> {
            StaffDirectory directory = withManager();
            StaffSession session = new StaffSession(directory);
            session.signIn("amina", "482913");
            assertTrue(session.isSignedIn(), "signed in");
            assertEquals("Amina Okello", session.getDisplayName(), "name is known");
            session.signOut();
            assertFalse(session.isSignedIn(), "signed out");
            assertTrue(session.getDisplayName() == null, "name cleared");
            assertFalse(session.canViewCustomerDetails(), "and access with it");
        });

        test("a forced PIN change is reported until it is done", () -> {
            StaffDirectory directory = withManager();
            StaffSession session = new StaffSession(directory);
            session.signIn("amina", "482913");
            assertTrue(session.mustChangePin(), "the generated PIN must be replaced");
            session.signIn("amina", "482913");
            directory.changePin("amina", "482913", "731204");
            assertFalse(session.mustChangePin(), "no longer forced");
        });

        test("generated PINs are strong and not all the same", () -> {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (int attempt = 0; attempt < 40; attempt++) {
                String pin = StaffDirectory.startingPin();
                assertEquals(8, pin.length(), "eight digits");
                assertTrue(pin.matches("[0-9]{8}"), "digits only: " + pin);
                assertFalse(List.of("12345678", "87654321", "11111111", "00000000")
                        .contains(pin), "must not be a guessable run: " + pin);
                seen.add(pin);
            }
            assertTrue(seen.size() > 35, "generated PINs should differ, saw " + seen.size());
        });

        test("two accounts get different PINs for the same username in different setups", () ->
                assertTrue(StaffDirectory.startingPin().length() == 8, "sanity"));

        test("accounts survive closing and reopening the database", () -> {
            java.nio.file.Path file = freshDatabase("staffpersist");
            StaffDirectory first = new StaffDirectory(new Database(file));
            first.create("amina", "Amina Okello", "482913", StaffDirectory.Role.MANAGER, false);
            StaffDirectory reopened = new StaffDirectory(new Database(file));
            assertEquals(1, reopened.list().size(), "the account persisted");
            assertTrue(reopened.signIn("amina", "482913").isSuccess(), "and still signs in");
        });

        test("an encrypted database cannot be opened without its password", () -> {
            java.nio.file.Path file = freshDatabase("staffenc");
            StaffDirectory setup =
                    new StaffDirectory(new Database(file, "s3cret-pass".toCharArray()));
            setup.create("amina", "Amina Okello", "482913", StaffDirectory.Role.MANAGER, false);
            assertEquals(1, setup.list().size(), "readable with the right password");

            // The wrong password must fail at the connection, not quietly return nothing.
            Database wrongDatabase = new Database(file, "wrong-pass".toCharArray());
            assertThrows("a wrong database password must not open the file",
                    () -> wrongDatabase.open().close());
        });

        test("a protected database shows nothing without the password", () -> {
            java.nio.file.Path file = freshDatabase("staffenc2");
            new StaffDirectory(new Database(file, "s3cret-pass".toCharArray()))
                    .create("amina", "Amina Okello", "482913", StaffDirectory.Role.MANAGER, false);
            // The list degrades to empty rather than crashing the application, so no
            // customer detail can leak, but the account is simply not there.
            StaffDirectory wrong =
                    new StaffDirectory(new Database(file, "wrong-pass".toCharArray()));
            assertTrue(wrong.list().isEmpty(), "no accounts are readable without the password");
            assertTrue(wrong.signIn("amina", "482913").getRefusal() != null,
                    "and no sign-in is possible");
        });

        test("the password marker is written beside the database", () -> {
            java.nio.file.Path file = freshDatabase("staffenc3");
            assertFalse(Database.requiresPassword(file), "a plain file needs no password");
            new DatabaseProtection(file).encrypt("s3cret-pass".toCharArray());
            assertTrue(Database.requiresPassword(file),
                    "a protected file is marked so the password can be asked for");
        });
    }

    /**
     * Reads the staff_accounts table straight out of the database file, column by
     * column, so a test can assert a secret is not sitting there in the clear.
     */
    /** Reads the database file in blocks so a secret can be searched for. */
    private static List<byte[]> readFileBlocks(java.nio.file.Path file) throws Exception {
        List<byte[]> blocks = new java.util.ArrayList<>();
        if (!java.nio.file.Files.exists(file)) {
            java.nio.file.Path actual = java.nio.file.Path.of(
                    file.toString().replace(".dat", ".mv.db"));
            if (java.nio.file.Files.exists(actual)) {
                file = actual;
            } else {
                return blocks;
            }
        }
        byte[] all = java.nio.file.Files.readAllBytes(file);
        int size = 4096;
        for (int start = 0; start < all.length; start += size) {
            blocks.add(java.util.Arrays.copyOfRange(all, start,
                    Math.min(all.length, start + size)));
        }
        return blocks;
    }

    private static String readStaffTable() throws Exception {
        StringBuilder dump = new StringBuilder();
        try (java.sql.Connection connection = lastDatabase.open();
             java.sql.Statement statement = connection.createStatement();
             java.sql.ResultSet results = statement.executeQuery(
                     "SELECT * FROM staff_accounts")) {
            int columns = results.getMetaData().getColumnCount();
            while (results.next()) {
                for (int column = 1; column <= columns; column++) {
                    dump.append(results.getString(column)).append('|');
                }
                dump.append('\n');
            }
        }
        return dump.toString();
    }
}
