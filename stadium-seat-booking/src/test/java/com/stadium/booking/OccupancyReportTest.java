package com.stadium.booking;

import static com.stadium.booking.TestRunner.assertClose;
import static com.stadium.booking.TestRunner.assertEquals;
import static com.stadium.booking.TestRunner.assertTrue;
import static com.stadium.booking.TestRunner.freshDatabase;
import static com.stadium.booking.TestRunner.suite;
import static com.stadium.booking.TestRunner.test;

import java.util.List;

/**
 * The occupancy report. The figures are what a venue manager budgets on, so the
 * capacity a row is measured against has to be the seats on sale across every
 * event at that ground, not one event's seat count.
 */
final class OccupancyReportTest {
    private OccupancyReportTest() {
    }

    private static BookingService serviceWithBookings() throws Exception {
        BookingService service = new BookingService(new BookingStore(freshDatabase("occupancy")));
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
        return service;
    }

    private static OccupancyReport.Row rowFor(OccupancyReport report, String stadiumId) {
        return report.getRows().stream()
                .filter(row -> row.getStadium().getId().equals(stadiumId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no row for " + stadiumId));
    }

    static void register() {
        suite("Occupancy report");

        test("every venue appears, in directory order", () -> {
            OccupancyReport report = OccupancyReport.compute(null);
            assertEquals(StadiumData.getStadiums().size(), report.getRows().size(),
                    "one row per venue");
            assertEquals(StadiumData.getStadiums().get(0).getId(),
                    report.getRows().get(0).getStadium().getId(), "first venue first");
        });

        test("capacity is counted across every event at a venue", () -> {
            OccupancyReport report = OccupancyReport.compute(null);
            Stadium namboole = StadiumData.getStadium("namboole");
            OccupancyReport.Row row = rowFor(report, "namboole");
            int events = StadiumData.getEvents("namboole").size();
            assertEquals(events, row.getEvents(), "event count");
            // The whole point: eight events means eight times the seats on sale,
            // not the single-event figure, or a busy ground looks empty.
            assertEquals(namboole.getSeatCount() * events, row.getSeatCapacity(),
                    "capacity across all events");
        });

        test("rows sum to the totals", () -> {
            OccupancyReport report = OccupancyReport.compute(serviceWithBookings());
            int capacity = report.getRows().stream().mapToInt(OccupancyReport.Row::getSeatCapacity).sum();
            int booked = report.getRows().stream().mapToInt(OccupancyReport.Row::getSeatsBooked).sum();
            int events = report.getRows().stream().mapToInt(OccupancyReport.Row::getEvents).sum();
            assertEquals(report.getTotalCapacity(), capacity, "capacity total matches the rows");
            assertEquals(report.getTotalBooked(), booked, "booked total matches the rows");
            assertEquals(report.getTotalEvents(), events, "event total matches the rows");
            assertEquals(StadiumData.getEvents().size(), report.getTotalEvents(),
                    "every event is counted once");
        });

        test("booked seats are counted, spread across sections", () -> {
            OccupancyReport report = OccupancyReport.compute(serviceWithBookings());
            OccupancyReport.Row row = rowFor(report, "namboole");
            assertEquals(3, row.getSeatsBooked(), "two seats plus one seat");
            int bySection = row.getSeatsInSection("A") + row.getSeatsInSection("B")
                    + row.getSeatsInSection("C") + row.getSeatsInSection("D");
            assertEquals(row.getSeatsBooked(), bySection, "section counts add up");
            assertTrue(row.getSeatsInSection("D") == 0, "nothing was booked in section D");
        });

        test("cancelled bookings free their seats in the report", () -> {
            BookingService service = serviceWithBookings();
            OccupancyReport.Row before = rowFor(OccupancyReport.compute(service), "namboole");
            Booking booking = service.getBookings().get(0);
            service.cancel(booking.getReference());
            OccupancyReport.Row after = rowFor(OccupancyReport.compute(service), "namboole");
            assertEquals(before.getSeatsBooked() - booking.getSeats().size(),
                    after.getSeatsBooked(), "a cancellation reduces the count");
        });

        test("vacancy is capacity minus booked", () -> {
            OccupancyReport.Row row = rowFor(OccupancyReport.compute(serviceWithBookings()), "namboole");
            assertEquals(row.getSeatCapacity() - row.getSeatsBooked(), row.getSeatsAvailable(),
                    "available seats");
            assertClose(row.getSeatsAvailable() * 100.0 / row.getSeatCapacity(),
                    row.getVacancyPercentage(), 0.0001, "vacancy percentage");
        });

        test("an empty database reads as fully vacant", () -> {
            BookingService service = new BookingService(new BookingStore(freshDatabase("empty")));
            OccupancyReport report = OccupancyReport.compute(service);
            assertEquals(0, report.getTotalBooked(), "nothing booked");
            assertClose(100.0, report.getOverallVacancyPercentage(), 0.0001, "fully vacant");
        });

        test("booked seats never exceed capacity", () -> {
            OccupancyReport report = OccupancyReport.compute(serviceWithBookings());
            for (OccupancyReport.Row row : report.getRows()) {
                assertTrue(row.getSeatsBooked() <= row.getSeatCapacity(),
                        "overbooked: " + row.getStadium().getName());
                assertTrue(row.getSeatsAvailable() >= 0,
                        "negative vacancy at " + row.getStadium().getName());
                assertTrue(row.getVacancyPercentage() >= 0.0
                                && row.getVacancyPercentage() <= 100.0,
                        "vacancy out of range at " + row.getStadium().getName());
            }
        });
    }
}
