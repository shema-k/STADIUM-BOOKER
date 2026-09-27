package com.stadium.booking;

import static com.stadium.booking.TestRunner.assertClose;
import static com.stadium.booking.TestRunner.assertEquals;
import static com.stadium.booking.TestRunner.assertFalse;
import static com.stadium.booking.TestRunner.assertThrows;
import static com.stadium.booking.TestRunner.assertTrue;
import static com.stadium.booking.TestRunner.freshDatabase;
import static com.stadium.booking.TestRunner.suite;
import static com.stadium.booking.TestRunner.test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Choosing seats now and booking them later.
 *
 * <p>A saved selection is deliberately not a booking: nothing is sold and the seats
 * are not held, so the tests here are mostly about it going stale honestly rather
 * than about the arithmetic.
 */
final class SavedSelectionTest {
    private SavedSelectionTest() {
    }

    private static BookingService service() throws Exception {
        return new BookingService(new BookingStore(freshDatabase("saved")));
    }

    private static List<Seat> pick(BookingService service, int... numbers) {
        List<Seat> seats = new java.util.ArrayList<>();
        for (int number : numbers) {
            seats.add(service.getSeat(new SeatKey("A", 1, number)));
        }
        return seats;
    }

    static void register() {
        suite("Saved seat selections");

        test("there is nowhere to save a selection before this", () -> {
            // A fresh database has no saved selections, which is the state the
            // complaint was about: there was no way to keep a choice at all.
            assertTrue(service().getSavedSelections().isEmpty(), "nothing saved to begin with");
        });

        test("a chosen set of seats can be saved", () -> {
            BookingService service = service();
            service.selectEvent(StadiumData.getEvent("namboole-01"));
            List<Seat> seats = pick(service, 1, 2, 3);
            BookingStore.SavedSelection saved = service.saveSelection("Cranes with friends", seats);
            assertEquals("Cranes with friends", saved.getLabel(), "label");
            assertEquals(3, saved.getSeats().size(), "three seats");
            assertEquals(1, service.getSavedSelections().size(), "one saved selection");
        });

        test("a saved selection records the event it belongs to", () -> {
            BookingService service = service();
            StadiumEvent event = StadiumData.getEvent("namboole-01");
            service.selectEvent(event);
            BookingStore.SavedSelection saved = service.saveSelection("Mine",
                    pick(service, 5));
            assertEquals(event.getId(), saved.getEventId(), "the event");
            assertEquals(event.getStadiumId(), saved.getStadiumId(), "the venue");
            assertEquals(event.getHeadline(), saved.getEventName(), "the event name, so it reads "
                    + "even if the event later changes");
        });

        test("a saved selection carries the price the seats were worth", () -> {
            BookingService service = service();
            service.selectEvent(StadiumData.getEvent("namboole-01"));
            List<Seat> seats = pick(service, 1, 2);
            BookingStore.SavedSelection saved = service.saveSelection("Mine", seats);
            assertClose(service.getTotalCharge(seats), saved.getTotal(), 0.01,
                    "the saved total matches the price outline");
        });

        test("saved selections survive closing and reopening the database", () -> {
            Path file = freshDatabase("savedpersist");
            BookingService first = new BookingService(new BookingStore(file));
            first.selectEvent(StadiumData.getEvent("namboole-01"));
            BookingStore.SavedSelection saved = first.saveSelection("For the derby",
                    pick(first, 7, 8));
            BookingService reopened = new BookingService(new BookingStore(file));
            assertEquals(1, reopened.getSavedSelections().size(), "still there");
            assertEquals(saved.getId(), reopened.getSavedSelections().get(0).getId(), "same one");
            assertEquals(2, reopened.getSavedSelections().get(0).getSeats().size(),
                    "with both seats");
        });

        test("newest is listed first", () -> {
            BookingService service = service();
            service.selectEvent(StadiumData.getEvent("namboole-01"));
            service.saveSelection("First", pick(service, 1));
            service.saveSelection("Second", pick(service, 2));
            List<BookingStore.SavedSelection> saved = service.getSavedSelections();
            assertEquals(2, saved.size(), "both saved");
            assertTrue(saved.get(0).getCreatedAt().compareTo(saved.get(1).getCreatedAt()) >= 0,
                    "newest first");
        });

        test("an empty selection is refused", () -> {
            BookingService service = service();
            service.selectEvent(StadiumData.getEvent("namboole-01"));
            assertThrows("nothing to save must be refused",
                    () -> service.saveSelection("Empty", List.of()));
        });

        test("a fresh service can save without choosing an event first", () -> {
            // The service starts on the first upcoming event, so there is always
            // something to save against and no dead end for the user.
            BookingService service = new BookingService(new BookingStore(
                    freshDatabase("savednoevent")));
            assertTrue(service.getActiveEvent() != null, "an event is selected to begin with");
            assertEquals("namboole-01", service.saveSelection("Straight away",
                    pick(service, 1)).getEventId(), "saved against that event");
        });

        test("a label is given one if none was typed", () -> {
            BookingService service = service();
            service.selectEvent(StadiumData.getEvent("namboole-01"));
            assertEquals("My seats", service.saveSelection("   ", pick(service, 1)).getLabel(),
                    "a blank label falls back to something readable");
            assertEquals("My seats", service.saveSelection(null, pick(service, 2)).getLabel(),
                    "and so does no label at all");
        });

        test("a long label is trimmed rather than refused", () -> {
            BookingService service = service();
            service.selectEvent(StadiumData.getEvent("namboole-01"));
            String long_ = "x".repeat(400);
            BookingStore.SavedSelection saved = service.saveSelection(long_, pick(service, 1));
            assertEquals(120, saved.getLabel().length(), "trimmed to fit");
        });

        test("a fresh selection can still be booked", () -> {
            BookingService service = service();
            service.selectEvent(StadiumData.getEvent("namboole-01"));
            BookingStore.SavedSelection saved = service.saveSelection("Mine",
                    pick(service, 10, 11));
            assertEquals(null, bookingService_canBeUsed(service, saved),
                    "nothing has happened to it, so it is still good");
        });

        test("a saved selection is not a booking", () -> {
            // The distinction matters: nothing is sold and nothing is held.
            BookingService service = service();
            service.selectEvent(StadiumData.getEvent("namboole-01"));
            service.saveSelection("Mine", pick(service, 20, 21));
            assertEquals(0, service.getBookings().size(), "no booking was created");
            assertEquals(0, service.getBookedSeatKeys(StadiumData.getEvent("namboole-01")).size(),
                    "and the seats are still on sale to anyone");
        });

        test("a saved selection goes stale when somebody buys its seats", () -> {
            BookingService service = service();
            StadiumEvent event = StadiumData.getEvent("namboole-01");
            service.selectEvent(event);
            BookingStore.SavedSelection saved = service.saveSelection("Mine",
                    pick(service, 30, 31));
            assertEquals(null, bookingService_canBeUsed(service, saved), "fine to begin with");

            service.book("Someone Else", "x@example.co.ug", "+256700000001",
                    List.of(service.getSeat(new SeatKey("A", 1, 30))));

            String problem = bookingService_canBeUsed(service, saved);
            assertTrue(problem != null, "it can no longer be booked");
            assertTrue(problem.contains("A1-30"),
                    "and it says which seat went: " + problem);
        });

        test("a saved selection goes stale when its event is cancelled", () -> {
            BookingService service = service();
            // namboole-04 has the rain-postponement notice against it.
            StadiumEvent postponed = StadiumData.getEvent("namboole-04");
            assertFalse(service.isBookingOpen(postponed), "the event is blocked");
            service.selectEvent(postponed);
            List<Seat> seats = new java.util.ArrayList<>();
            for (int n = 1; n <= 3; n++) {
                seats.add(service.getSeat(new SeatKey("A", 1, n)));
            }
            BookingStore.SavedSelection saved = service.saveSelection("Rain or shine", seats);
            String problem = bookingService_canBeUsed(service, saved);
            assertTrue(problem != null, "it cannot be booked");
            assertTrue(problem.toLowerCase(java.util.Locale.ENGLISH).contains("postponed")
                            || problem.toLowerCase(java.util.Locale.ENGLISH).contains("cancel"),
                    "and the reason is given: " + problem);
        });

        test("a saved selection is dropped from the event once discarded", () -> {
            BookingService service = service();
            service.selectEvent(StadiumData.getEvent("namboole-01"));
            BookingStore.SavedSelection saved = service.saveSelection("Mine",
                    pick(service, 40));
            assertTrue(service.deleteSavedSelection(saved.getId()), "discarded");
            assertTrue(service.getSavedSelections().isEmpty(), "gone");
            assertFalse(service.deleteSavedSelection(saved.getId()),
                    "discarding it twice does nothing");
        });

        test("several selections for one event can be kept side by side", () -> {
            BookingService service = service();
            service.selectEvent(StadiumData.getEvent("namboole-01"));
            service.saveSelection("Option A", pick(service, 1, 2));
            service.saveSelection("Option B", pick(service, 3, 4));
            Set<String> seats = service.getSavedSelections().stream()
                    .flatMap(selection -> selection.getSeats().stream())
                    .map(SeatKey::display).collect(Collectors.toSet());
            assertEquals(4, seats.size(), "four different seats: " + seats);
        });

        test("a restored selection takes the seats that are still free", () -> {
            // The seat map has to cope with a selection that has partly gone stale.
            BookingService service = service();
            service.selectEvent(StadiumData.getEvent("namboole-01"));
            service.book("Someone Else", "x@example.co.ug", "+256700000001",
                    List.of(service.getSeat(new SeatKey("A", 1, 2))));
            List<Seat> restored = new java.util.ArrayList<>();
            restored.add(service.getSeat(new SeatKey("A", 1, 1)));
            restored.add(service.getSeat(new SeatKey("A", 1, 3)));
            BookingStore.SavedSelection saved = service.saveSelection("Partly stale", restored);

            SeatMapPanel map = new SeatMapPanel(service, seat -> { }, () -> { },
                    message -> { });
            int taken = map.setSelectedKeys(
                    List.of(new SeatKey("A", 1, 1), new SeatKey("A", 1, 2),
                            new SeatKey("A", 1, 3)));
            assertEquals(2, taken, "the two that are still free: " + taken);
            Set<String> chosen = new HashSet<>();
            for (Seat seat : map.getSelectedSeats()) {
                chosen.add(seat.getKey().display());
            }
            assertFalse(chosen.contains("A1-02"), "the sold seat is not chosen");
            assertEquals(2, chosen.size(), "and only the free ones are");
            assertEquals(2, saved.getSeats().size(), "the saved list itself is untouched");
        });

        test("restoring nothing selects nothing", () -> {
            BookingService service = service();
            SeatMapPanel map = new SeatMapPanel(service, seat -> { }, () -> { },
                    message -> { });
            assertEquals(0, map.setSelectedKeys(List.of()), "an empty list");
            assertEquals(0, map.setSelectedKeys(null), "and a missing one");
        });
    }

    /** Saves, then asks whether it could still be booked today. */
    private static String bookingService_canBeUsed(BookingService service,
                                                  BookingStore.SavedSelection saved) {
        return service.whySelectionCannotBeUsed(saved);
    }
}
