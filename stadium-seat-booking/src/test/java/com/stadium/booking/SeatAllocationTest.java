package com.stadium.booking;

import static com.stadium.booking.TestRunner.assertEquals;
import static com.stadium.booking.TestRunner.assertTrue;
import static com.stadium.booking.TestRunner.freshDatabase;
import static com.stadium.booking.TestRunner.suite;
import static com.stadium.booking.TestRunner.test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** The rule that a reservation is spread across the four sections. */
final class SeatAllocationTest {
    private SeatAllocationTest() {
    }

    private static Map<String, Long> bySection(List<Seat> seats) {
        return seats.stream().collect(Collectors.groupingBy(
                seat -> seat.getKey().getSection(), LinkedHashMap::new, Collectors.counting()));
    }

    private static List<Seat> pickConsecutive(BookingService service, int count) {
        List<Seat> seats = new ArrayList<>();
        for (int n = 1; n <= count; n++) {
            seats.add(service.getSeat(new SeatKey("A", 1, n)));
        }
        return seats;
    }

    static void register() {
        suite("Section spread");

        test("one to four seats use one per section", () -> {
            for (int count = 1; count <= 4; count++) {
                BookingService service = new BookingService(new BookingStore(freshDatabase("spread")));
                service.selectEvent(StadiumData.getEvent("namboole-01"));
                List<Seat> spread = service.allocateSpreadSeats(pickConsecutive(service, count));
                assertEquals(count, spread.size(), "seat count for " + count);
                assertEquals(count, bySection(spread).size(),
                        count + " seats must use " + count + " sections, got " + bySection(spread));
            }
        });

        test("five and six seats are shared evenly", () -> {
            for (int count = 5; count <= 6; count++) {
                BookingService service = new BookingService(new BookingStore(freshDatabase("spread")));
                service.selectEvent(StadiumData.getEvent("namboole-01"));
                Map<String, Long> split = bySection(service.allocateSpreadSeats(pickConsecutive(service, count)));
                assertEquals(4, split.size(), count + " seats must use all four sections");
                int total = split.values().stream().mapToInt(Long::intValue).sum();
                assertEquals(count, total, "seats accounted for");
                int max = split.values().stream().mapToInt(Long::intValue).max().orElse(0);
                int min = split.values().stream().mapToInt(Long::intValue).min().orElse(0);
                assertTrue(max - min <= 1, count + " seats must be split evenly, got " + split);
            }
        });

        test("a booking never lands entirely in one section", () -> {
            BookingService service = new BookingService(new BookingStore(freshDatabase("spread")));
            service.selectEvent(StadiumData.getEvent("namboole-01"));
            List<Seat> spread = service.allocateSpreadSeats(pickConsecutive(service, 6));
            assertTrue(bySection(spread).size() >= 4, "six seats must span four sections");
        });

        test("no duplicate or unavailable seat is allocated", () -> {
            BookingService service = new BookingService(new BookingStore(freshDatabase("spread")));
            StadiumEvent event = StadiumData.getEvent("namboole-01");
            service.selectEvent(event);
            List<Seat> spread = service.allocateSpreadSeats(pickConsecutive(service, 6));
            java.util.Set<SeatKey> seen = new java.util.HashSet<>();
            for (Seat seat : spread) {
                assertTrue(seen.add(seat.getKey()), "duplicate seat " + seat.getKey().display());
                assertTrue(service.isSeatSelectable(seat.getKey()),
                        "allocated an unavailable seat " + seat.getKey().display());
            }
        });

        test("the spread allocation is what gets booked", () -> {
            BookingService service = new BookingService(new BookingStore(freshDatabase("spread")));
            StadiumEvent event = StadiumData.getEvent("namboole-01");
            service.selectEvent(event);
            List<Seat> spread = service.allocateSpreadSeats(pickConsecutive(service, 6));
            Booking booking = service.book("Spread User", "s@example.co.ug", "+256700000000", spread);
            assertEquals(spread.size(), booking.getSeats().size(), "booked seat count");
            for (Seat seat : spread) {
                assertTrue(service.getBookedSeatKeys(event).contains(seat.getKey()),
                        "not booked: " + seat.getKey().display());
            }
        });

        test("seats already taken are skipped", () -> {
            BookingService service = new BookingService(new BookingStore(freshDatabase("spread")));
            StadiumEvent event = StadiumData.getEvent("namboole-01");
            service.selectEvent(event);
            service.book("First Customer", "first@example.co.ug", "+256700000001",
                    service.allocateSpreadSeats(pickConsecutive(service, 4)));
            service.selectEvent(event);
            List<Seat> second = service.allocateSpreadSeats(pickConsecutive(service, 2));
            for (Seat seat : second) {
                assertTrue(!service.getBookedSeatKeys(event).contains(seat.getKey()),
                        "re-allocated the taken seat " + seat.getKey().display());
            }
        });
    }
}
