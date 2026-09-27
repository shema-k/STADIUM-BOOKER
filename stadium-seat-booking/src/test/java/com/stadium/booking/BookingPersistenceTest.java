package com.stadium.booking;

import static com.stadium.booking.TestRunner.assertEquals;
import static com.stadium.booking.TestRunner.assertThrows;
import static com.stadium.booking.TestRunner.assertTrue;
import static com.stadium.booking.TestRunner.freshDatabase;
import static com.stadium.booking.TestRunner.suite;
import static com.stadium.booking.TestRunner.test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Persistence, and the regression tests for the defect where a confirmed
 * booking could silently disappear from the database.
 */
final class BookingPersistenceTest {
    private BookingPersistenceTest() {
    }

    static void register() {
        suite("Database persistence");

        test("a booking survives reopening the database", () -> {
            Path file = freshDatabase("persist");
            StadiumEvent event = StadiumData.getEvent("namboole-01");
            BookingService first = new BookingService(new BookingStore(file));
            first.selectEvent(event);
            Booking saved = first.book("Alice Ssali", "alice@example.co.ug", "+256700000001",
                    List.of(first.getSeat(new SeatKey("A", 1, 1))));

            BookingService reopened = new BookingService(new BookingStore(file));
            assertEquals(1, reopened.getBookings().size(), "booking count after reopen");
            Booking loaded = reopened.getBookings().get(0);
            assertEquals(saved.getReference(), loaded.getReference(), "reference");
            assertEquals("Alice Ssali", loaded.getCustomerName(), "customer name");
            assertEquals("alice@example.co.ug", loaded.getEmail(), "email");
            assertEquals("A1-01", loaded.getSeatDisplay(), "seats");
            assertClose(saved.getTotal(), loaded.getTotal(), "total");
        });

        test("cancelling is persisted and releases the seat", () -> {
            Path file = freshDatabase("cancel");
            StadiumEvent event = StadiumData.getEvent("namboole-01");
            BookingService service = new BookingService(new BookingStore(file));
            service.selectEvent(event);
            Booking booking = service.book("Bob Mugisha", "bob@example.co.ug", "+256700000002",
                    List.of(service.getSeat(new SeatKey("A", 2, 2))));

            service.cancel(booking.getReference());

            BookingService reopened = new BookingService(new BookingStore(file));
            assertEquals(BookingStatus.CANCELLED, reopened.getBookings().get(0).getStatus(),
                    "cancelled status must persist");
            assertTrue(reopened.getBookedSeatKeys(event).isEmpty(),
                    "a cancelled booking must free its seat");
        });

        test("REGRESSION: two customers saving at once must not lose a booking", () -> {
            Path file = freshDatabase("concurrent");
            // Two independent application instances share one database, as happens
            // when two people book at the same time.
            BookingStore store = new BookingStore(file);
            BookingService customerA = new BookingService(store);
            BookingService customerB = new BookingService(store);
            StadiumEvent event = StadiumData.getEvent("namboole-01");
            customerA.selectEvent(event);
            customerB.selectEvent(event);

            customerA.book("Alice", "alice@example.co.ug", "+256700000001",
                    List.of(customerA.getSeat(new SeatKey("A", 1, 1))));
            customerB.book("Bob", "bob@example.co.ug", "+256700000002",
                    List.of(customerB.getSeat(new SeatKey("A", 1, 2))));

            BookingService reopened = new BookingService(new BookingStore(file));
            assertEquals(2, reopened.getBookings().size(),
                    "both confirmed bookings must survive; one was being lost");
            assertEquals(2, reopened.getBookedSeatKeys(event).size(), "both seats must be taken");
        });

        test("REGRESSION: the same seat cannot be sold twice", () -> {
            Path file = freshDatabase("doubleseat");
            StadiumEvent event = StadiumData.getEvent("namboole-01");
            BookingStore store = new BookingStore(file);
            BookingService first = new BookingService(store);
            first.selectEvent(event);
            first.book("Alice", "alice@example.co.ug", "+256700000001",
                    List.of(first.getSeat(new SeatKey("A", 1, 1))));

            // A second instance that has not seen the first booking tries the same seat.
            BookingService second = new BookingService(new BookingStore(file));
            second.selectEvent(event);
            assertThrows("a seat already sold must be refused",
                    () -> second.book("Bob", "bob@example.co.ug", "+256700000002",
                            List.of(second.getSeat(new SeatKey("A", 1, 1)))));

            BookingService reopened = new BookingService(new BookingStore(file));
            assertEquals(1, reopened.getBookings().size(), "the rejected booking must not be stored");
        });

        test("references are unique", () -> {
            Path file = freshDatabase("refs");
            StadiumEvent event = StadiumData.getEvent("namboole-01");
            BookingService service = new BookingService(new BookingStore(file));
            service.selectEvent(event);
            Set<String> references = new HashSet<>();
            for (int n = 1; n <= 5; n++) {
                Booking booking = service.book("Repeat User", "r@example.co.ug", "+256700000000",
                        List.of(service.getSeat(new SeatKey("A", n, 1))));
                assertTrue(references.add(booking.getReference()),
                        "duplicate reference " + booking.getReference());
            }
        });

        test("a failed save does not leave a phantom booking", () -> {
            Path file = freshDatabase("phantom");
            StadiumEvent event = StadiumData.getEvent("namboole-01");
            BookingStore store = new BookingStore(file);
            BookingService service = new BookingService(store);
            service.selectEvent(event);
            // Occupy a seat behind the service's back so the save will be rejected.
            BookingService other = new BookingService(new BookingStore(file));
            other.selectEvent(event);
            other.book("Other Person", "other@example.co.ug", "+256700000003",
                    List.of(other.getSeat(new SeatKey("B", 3, 3))));

            // Force a failure by closing over a booking that duplicates an existing seat.
            assertThrows("the second sale of a seat must be rejected",
                    () -> service.book("Second Person", "second@example.co.ug", "+256700000004",
                            List.of(service.getSeat(new SeatKey("B", 3, 3)))));
            assertTrue(service.getBookings().stream().noneMatch(b -> "Second Person".equals(b.getCustomerName())),
                    "a rejected booking must not stay in memory either");
        });
    }

    private static void assertClose(double expected, double actual, String message) {
        TestRunner.assertClose(expected, actual, 0.01, message);
    }
}
