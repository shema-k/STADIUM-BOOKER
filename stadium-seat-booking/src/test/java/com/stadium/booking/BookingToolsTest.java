package com.stadium.booking;

import static com.stadium.booking.TestRunner.assertEquals;
import static com.stadium.booking.TestRunner.assertFalse;
import static com.stadium.booking.TestRunner.assertTrue;
import static com.stadium.booking.TestRunner.freshDatabase;
import static com.stadium.booking.TestRunner.suite;
import static com.stadium.booking.TestRunner.test;

import java.util.List;

/** Seat holds, the staff PIN, tickets, export and payment records. */
final class BookingToolsTest {
    private BookingToolsTest() {
    }

    private static BookingService serviceWithBooking() throws Exception {
        BookingService service = new BookingService(new BookingStore(freshDatabase("tools")));
        StadiumEvent event = StadiumData.getEvent("namboole-01");
        service.selectEvent(event);
        service.book("Alice Ssali", "alice@example.co.ug", "+256700000001",
                service.allocateSpreadSeats(List.of(service.getSeat(new SeatKey("A", 1, 1)))));
        return service;
    }

    static void register() {
        suite("Seat holds, staff access and tickets");

        test("a held seat is blocked for another customer", () -> {
            BookingService service = new BookingService(new BookingStore(freshDatabase("hold")));
            StadiumEvent event = StadiumData.getEvent("namboole-01");
            service.selectEvent(event);
            SeatHoldService holds = new SeatHoldService(service);
            SeatKey key = new SeatKey("A", 1, 1);

            assertEquals(null, holds.hold(key, event, "customer-one"),
                    "the first hold should succeed");
            assertTrue(holds.isHeldByAnother(key, event, "customer-two"),
                    "a second customer must be blocked");
            assertFalse(holds.isHeldByAnother(key, event, "customer-one"),
                    "the holder is not blocked from their own hold");
            assertTrue(holds.hold(key, event, "customer-two") != null,
                    "a refused hold must explain itself");
        });

        test("a hold expires and frees the seat", () -> {
            BookingService service = new BookingService(new BookingStore(freshDatabase("hold")));
            StadiumEvent event = StadiumData.getEvent("namboole-01");
            service.selectEvent(event);
            SeatHoldService holds = new SeatHoldService(service);
            SeatKey key = new SeatKey("A", 1, 1);
            holds.hold(key, event, "customer-one");
            assertTrue(holds.secondsRemaining(key) > 0, "a fresh hold has time left");
            // Simulate the clock running out by expiring everything.
            holds.releaseAll();
            assertEquals(0L, holds.secondsRemaining(key), "a released hold has no time left");
            assertFalse(holds.isHeldByAnother(key, event, "customer-two"),
                    "once released the seat is free again");
        });

        test("a hold does not survive a booking becoming unavailable", () -> {
            BookingService service = new BookingService(new BookingStore(freshDatabase("hold")));
            StadiumEvent event = StadiumData.getEvent("namboole-01");
            service.selectEvent(event);
            SeatHoldService holds = new SeatHoldService(service);
            SeatKey key = new SeatKey("A", 1, 1);
            holds.hold(key, event, "customer-one");
            service.book("Someone Else", "x@example.co.ug", "+256700000009",
                    List.of(service.getSeat(key)));
            assertTrue(holds.hold(key, event, "customer-two") != null,
                    "a seat that is now booked cannot be held");
        });




        test("a ticket carries the booking details", () -> {
            BookingService service = serviceWithBooking();
            Booking booking = service.getBookings().get(0);
            StadiumEvent event = StadiumData.getEvent(booking.getEventId());
            String ticket = TicketBuilder.text(booking,
                    StadiumData.getStadium(booking.getStadiumId()), event);
            assertTrue(ticket.contains(booking.getReference()), "ticket shows the reference");
            assertTrue(ticket.contains(booking.getCustomerName()), "ticket names the customer");
            assertTrue(ticket.contains(booking.getSeatDisplay()), "ticket lists the seats");
            assertTrue(ticket.contains(BookingService.formatMoney(booking.getTotal())),
                    "ticket shows the total");
        });

        test("the check code is stable and six characters", () -> {
            BookingService service = serviceWithBooking();
            Booking booking = service.getBookings().get(0);
            String code = TicketBuilder.checkCode(booking);
            assertEquals(6, code.length(), "check code length");
            assertEquals(code, TicketBuilder.checkCode(booking), "check code is stable");
        });

        test("CSV export quotes values and has a header", () -> {
            BookingService service = serviceWithBooking();
            String csv = TicketBuilder.csv(service.getBookings());
            String[] lines = csv.trim().split("\n");
            assertEquals(TicketBuilder.csvHeader(), lines[0], "header row");
            assertTrue(lines.length >= 2, "one row per booking");
            assertTrue(lines[1].startsWith("\""), "values are quoted");
        });

        test("cash payment needs no provider", () -> {
            PaymentRecord payment = PaymentRecord.cashAtVenue(89_860);
            assertEquals(PaymentRecord.Method.CASH_AT_VENUE, payment.getMethod(), "method");
            assertFalse(payment.isSimulated(), "cash is a real record");
            assertTrue(payment.getReference().length() == 6, "reference length");
        });

        test("mobile money is clearly marked as simulated", () -> {
            PaymentRecord payment = PaymentRecord.simulatedMobileMoney(89_860, "+256700000000");
            assertTrue(payment.isSimulated(), "must not pretend money moved");
            assertTrue(payment.getReference().startsWith("SIM-"), "reference marks the simulation");
            assertTrue(payment.describe().contains("simulated"), "the description says so");
            assertFalse(PaymentRecord.isRealProviderConfigured(),
                    "no provider credentials ship with this build");
        });
    }
}
