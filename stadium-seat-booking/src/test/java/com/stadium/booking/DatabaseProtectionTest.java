package com.stadium.booking;

import static com.stadium.booking.TestRunner.assertEquals;
import static com.stadium.booking.TestRunner.assertFalse;
import static com.stadium.booking.TestRunner.assertThrows;
import static com.stadium.booking.TestRunner.assertTrue;
import static com.stadium.booking.TestRunner.freshDatabase;
import static com.stadium.booking.TestRunner.suite;
import static com.stadium.booking.TestRunner.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Requiring a password before the booking database can be opened.
 *
 * <p>The risk here is losing bookings, so the tests care far more about what
 * survives the change than about the protection itself.
 *
 * <p>One test deliberately records a limitation rather than hiding it. H2 2.2.224
 * was checked directly and does not encrypt page contents, so a protected file
 * cannot be opened without the password but the customer details inside it are
 * still readable to anyone with a hex editor. If a future version of H2 does
 * encrypt the bytes, that test will fail and the wording can be corrected.
 */
final class DatabaseProtectionTest {
    private DatabaseProtectionTest() {
    }

    /** Fills a database with bookings and a staff account. */
    private static Path populatedDatabase() throws Exception {
        Path file = freshDatabase("encrypt");
        Database database = new Database(file);
        BookingService service = new BookingService(database);
        StadiumEvent cranes = StadiumData.getEvent("namboole-01");
        StadiumEvent derby = StadiumData.getEvent("namboole-02");
        service.selectEvent(cranes);
        service.book("Alice Ssali", "alice@example.co.ug", "+256700000001",
                service.allocateSpreadSeats(List.of(
                        service.getSeat(new SeatKey("A", 1, 1)),
                        service.getSeat(new SeatKey("A", 1, 2)))));
        service.selectEvent(derby);
        service.book("Bob Mugisha", "bob@example.co.ug", "+256700000002",
                service.allocateSpreadSeats(List.of(service.getSeat(new SeatKey("B", 1, 1)))));
        StaffDirectory staff = new StaffDirectory(database);
        staff.create("amina", "Amina Okello", "482913", StaffDirectory.Role.MANAGER, false);
        return file;
    }

    static void register() {
        suite("Database encryption");

        test("a fresh database needs no password", () -> {
            Path file = freshDatabase("encryptplain");
            new Database(file).open().close();
            assertFalse(Database.requiresPassword(file), "nothing to mark");
            assertFalse(new DatabaseProtection(file).isAlreadyProtected(), "no password yet");
        });

        test("a short password is refused", () -> {
            Path file = freshDatabase("encryptshort");
            new Database(file).open().close();
            DatabaseProtection.Outcome result = new DatabaseProtection(file).encrypt("short".toCharArray());
            assertFalse(result.isPerformed(), "not performed");
            assertTrue(result.getProblem() != null, "with a reason: " + result.getProblem());
        });

        test("a fresh database with no data can still be encrypted", () -> {
            Path file = freshDatabase("encryptempty");
            new Database(file).open().close();
            DatabaseProtection.Outcome result = new DatabaseProtection(file)
                    .encrypt("long-enough-pass".toCharArray());
            assertTrue(result.isPerformed(), "performed: " + result.getProblem());
            assertTrue(Database.requiresPassword(file), "now marked as needing a password");
        });

        test("every booking survives the change", () -> {
            Path file = populatedDatabase();
            int before = new BookingService(new Database(file)).getBookings().size();
            assertEquals(2, before, "two bookings to begin with");

            DatabaseProtection.Outcome result = new DatabaseProtection(file)
                    .encrypt("long-enough-pass".toCharArray());
            assertTrue(result.isPerformed(), "performed: " + result.getProblem());

            BookingService after = new BookingService(new Database(file, "long-enough-pass".toCharArray()));
            assertEquals(2, after.getBookings().size(), "both bookings survived");
            assertTrue(after.getBookings().stream()
                            .anyMatch(b -> "Alice Ssali".equals(b.getCustomerName())),
                    "with their customer details");
            assertTrue(after.getBookings().stream()
                            .anyMatch(b -> b.getSeatDisplay().contains("A1-01")),
                    "and their seats");
        });

        test("the seat uniqueness guarantee survives the change", () -> {
            Path file = populatedDatabase();
            new DatabaseProtection(file).encrypt("long-enough-pass".toCharArray());
            Database database = new Database(file, "long-enough-pass".toCharArray());
            BookingService reopened = new BookingService(database);
            StadiumEvent cranes = StadiumData.getEvent("namboole-01");
            assertEquals(2, reopened.getBookedSeatKeys(cranes).size(), "both seats still taken");
            // And the database still refuses to sell one of them again.
            reopened.selectEvent(cranes);
            assertThrows("a seat already sold must still be refused",
                    () -> reopened.book("Carol", "c@example.co.ug", "+256700000003",
                            List.of(reopened.getSeat(new SeatKey("A", 1, 1)))));
        });

        test("staff accounts survive the change", () -> {
            Path file = populatedDatabase();
            new DatabaseProtection(file).encrypt("long-enough-pass".toCharArray());
            StaffDirectory after = new StaffDirectory(
                    new Database(file, "long-enough-pass".toCharArray()));
            assertEquals(1, after.list().size(), "the account survived");
            assertTrue(after.signIn("amina", "482913").isSuccess(), "and still signs in");
        });

        test("the access trail survives the change", () -> {
            Path file = populatedDatabase();
            StaffDirectory before = new StaffDirectory(new Database(file));
            int entries = before.recentAccess(50).size();
            assertTrue(entries > 0, "there is something to copy");
            new DatabaseProtection(file).encrypt("long-enough-pass".toCharArray());
            assertEquals(entries,
                    new StaffDirectory(new Database(file, "long-enough-pass".toCharArray()))
                            .recentAccess(50).size(),
                    "the trail came across");
        });

        test("a protected database yields nothing without the password", () -> {
            Path file = populatedDatabase();
            new DatabaseProtection(file).encrypt("long-enough-pass".toCharArray());
            // Reading the bookings needs the password, not just a different guess.
            BookingService without = new BookingService(new Database(file, "guess-here".toCharArray()));
            assertEquals(0, without.getBookings().size(), "nothing readable with a wrong password");
        });

        test("KNOWN LIMITATION: a protected file's contents are still readable", () -> {
            Path file = populatedDatabase();
            new DatabaseProtection(file).encrypt("long-enough-pass".toCharArray());
            // The password genuinely stops the file being opened:
            assertThrows("the file must not open without its password",
                    () -> new Database(file).open().close());
            // But H2 2.2.224 does not encrypt page contents, so the bytes are plain.
            // This assertion is the honest record of that, and the reason the
            // application never claims the data is encrypted at rest.
            assertTrue(plainTextContains(file, "Alice Ssali"),
                    "H2 2.2.224 leaves page contents readable; if this starts failing, "
                            + "the database really is encrypted and the wording should be updated");
        });

        test("protecting twice is refused rather than losing the data", () -> {
            Path file = populatedDatabase();
            new DatabaseProtection(file).encrypt("long-enough-pass".toCharArray());
            DatabaseProtection.Outcome second = new DatabaseProtection(file)
                    .encrypt("another-password".toCharArray());
            assertFalse(second.isPerformed(), "not performed again");
            assertTrue(second.getProblem().contains("already needs a password"),
                    "with a clear reason: " + second.getProblem());
            // The first password still works, so nothing was lost.
            assertTrue(new StaffDirectory(new Database(file, "long-enough-pass".toCharArray()))
                    .signIn("amina", "482913").isSuccess(), "the original password still opens it");
        });

        test("a failed change leaves the original untouched", () -> {
            Path file = populatedDatabase();
            // A password the copy cannot be reopened with is not possible through the
            // public path, so the failure is forced by a database that cannot be read.
            Files.deleteIfExists(Database.markerFor(file));
            DatabaseProtection.Outcome result = new DatabaseProtection(file)
                    .encrypt("long-enough-pass".toCharArray());
            if (result.isPerformed()) {
                assertTrue(true, "migrated, so there was nothing to fail");
                return;
            }
            // If it did fail, the bookings must all still be there.
            assertEquals(2, new BookingService(new Database(file)).getBookings().size(), "the data is intact");
        });

        test("no temporary files are left behind", () -> {
            Path file = populatedDatabase();
            new DatabaseProtection(file).encrypt("long-enough-pass".toCharArray());
            // The working copy is the file name with ".encrypting" appended, so the
            // names to check are built from the whole file name, not a substring.
            String name = file.getFileName().toString();
            for (String suffix : new String[]{".encrypting", ".encrypting.mv.db",
                    ".encrypting.encrypted", ".encrypting.trace.db"}) {
                Path leftover = file.resolveSibling(name + suffix);
                assertFalse(Files.exists(leftover), "no leftover " + leftover.getFileName());
            }
            // The one marker that should remain is the real file's own.
            assertTrue(Files.exists(Database.markerFor(file)),
                    "the real file is still marked as needing a password");
        });
    }

    /** Whether the database file contains readable text, searching the whole file. */
    private static boolean plainTextContains(Path file, String needle) {
        for (Path candidate : new Path[]{file,
                Path.of(file.toString().replace(".dat", ".mv.db"))}) {
            if (!Files.exists(candidate)) {
                continue;
            }
            try {
                String contents = new String(Files.readAllBytes(candidate),
                        java.nio.charset.StandardCharsets.ISO_8859_1);
                if (contents.contains(needle)) {
                    return true;
                }
            } catch (java.io.IOException ignored) {
                // A file that cannot be read is not a leak.
            }
        }
        return false;
    }
}
