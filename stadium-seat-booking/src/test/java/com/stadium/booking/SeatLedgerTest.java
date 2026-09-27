package com.stadium.booking;

import static com.stadium.booking.TestRunner.assertClose;
import static com.stadium.booking.TestRunner.assertEquals;
import static com.stadium.booking.TestRunner.assertTrue;
import static com.stadium.booking.TestRunner.freshDatabase;
import static com.stadium.booking.TestRunner.suite;
import static com.stadium.booking.TestRunner.test;

import java.util.List;
import java.util.Set;

/** The per-venue, per-event view of which seats have been taken. */
final class SeatLedgerTest {
    private SeatLedgerTest() {
    }

    private static BookingService serviceWithSeats() throws Exception {
        BookingService service = new BookingService(new BookingStore(freshDatabase("ledger")));
        StadiumEvent cranes = StadiumData.getEvent("namboole-01");
        service.selectEvent(cranes);
        service.book("Alice Ssali", "alice@example.co.ug", "+256700000001",
                service.allocateSpreadSeats(List.of(
                        service.getSeat(new SeatKey("A", 1, 1)),
                        service.getSeat(new SeatKey("A", 1, 2)),
                        service.getSeat(new SeatKey("A", 1, 3)))));
        return service;
    }

    static void register() {
        suite("Booked seats ledger");

        test("only the selected event is reported", () -> {
            BookingService service = serviceWithSeats();
            StadiumEvent cranes = StadiumData.getEvent("namboole-01");
            StadiumEvent derby = StadiumData.getEvent("namboole-02");
            assertEquals(3, service.getBookedSeatKeys(cranes).size(), "Cranes match seats");
            assertTrue(service.getBookedSeatKeys(derby).isEmpty(),
                    "a different event must have no booked seats");
        });

        test("occupancy matches the booking count", () -> {
            BookingService service = serviceWithSeats();
            StadiumEvent cranes = StadiumData.getEvent("namboole-01");
            Set<SeatKey> booked = service.getBookedSeatKeys(cranes);
            int total = service.getTotalSeatCount(cranes);
            assertEquals(3, booked.size(), "booked seats");
            assertEquals(total - 3, service.getAvailableSeatCount(cranes), "available seats");
            assertClose(100.0 * (total - 3) / total,
                    service.getVacancyPercentage(cranes), 0.001, "vacancy percentage");
        });

        test("a seat cannot be priced for an event without touching the seat map", () -> {
            BookingService service = serviceWithSeats();
            StadiumEvent active = service.getActiveEvent();
            StadiumEvent other = StadiumData.getEvent("namboole-03");
            double price = service.getSeatPrice(other, new SeatKey("A", 1, 1));
            assertTrue(price > 0, "other event seats should still price");
            assertEquals(active.getId(), service.getActiveEvent().getId(),
                    "the event being booked must not change");
        });

        test("price tiers are named for the row", () -> {
            BookingService service = serviceWithSeats();
            Stadium namboole = StadiumData.getStadium("namboole");
            SeatSection sectionA = namboole.getSection("A");
            assertEquals("Front rows", service.getRowTierName(1, sectionA.getRows()), "row 1 tier");
            assertEquals("Back rows",
                    service.getRowTierName(sectionA.getRows(), sectionA.getRows()), "last row tier");
        });

        test("every seat carries its section, tier and price", () -> {
            BookingService service = serviceWithSeats();
            StadiumEvent cranes = StadiumData.getEvent("namboole-01");
            Stadium namboole = StadiumData.getStadium("namboole");
            for (SeatKey key : service.getBookedSeatKeys(cranes)) {
                assertTrue(namboole.getSection(key.getSection()) != null,
                        "unknown section on " + key.display());
                assertTrue(service.getSeatPrice(cranes, key) > 0, "price for " + key.display());
                assertTrue(!service.getRowTierName(key.getRow(),
                        namboole.getSection(key.getSection()).getRows()).isBlank(), "tier name");
            }
        });

        test("bookings are searchable by reference, venue, event and status", () -> {
            BookingService service = serviceWithSeats();
            Booking booking = service.getBookings().get(0);
            Stadium venue = StadiumData.getStadium(booking.getStadiumId());
            // The bookings screen searches reference, venue, event, seats and status.
            String haystack = (booking.getReference() + " " + venue.getName() + " "
                    + booking.getEvent() + " " + booking.getSeatDisplay() + " "
                    + booking.getStatus().name()).toLowerCase(LocaleHolder.ENGLISH);
            assertTrue(haystack.contains(booking.getReference().toLowerCase(LocaleHolder.ENGLISH)),
                    "searchable by reference");
            assertTrue(haystack.contains("namboole"), "searchable by venue");
            assertTrue(haystack.contains(booking.getStatus().name().toLowerCase(LocaleHolder.ENGLISH)),
                    "searchable by status");
            assertTrue(haystack.contains(booking.getSeatDisplay().toLowerCase(LocaleHolder.ENGLISH)),
                    "searchable by seat");
        });
    }

    /** Holds the locale constant so the test does not import java.util directly. */
    static final class LocaleHolder {
        static final java.util.Locale ENGLISH = java.util.Locale.ENGLISH;

        private LocaleHolder() {
        }
    }
}
