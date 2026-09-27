package com.stadium.booking;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Renders a booking as a printable ticket and as a spreadsheet-friendly row.
 *
 * <p>The ticket carries the reference, event, venue, seats and total, plus a
 * simple check code derived from the booking so a paper or exported ticket can
 * be matched back to the database by eye.
 */
public final class TicketBuilder {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault());

    private static String stamp(Instant createdAt) {
        return createdAt == null ? "" : STAMP.format(createdAt);
    }

    private TicketBuilder() {
    }

    /**
     * A short check code built from the reference and seat list, so two tickets
     * can be compared without a barcode scanner.
     */
    public static String checkCode(Booking booking) {
        String seed = booking.getReference() + "|" + booking.getSeatDisplay();
        int hash = 0;
        for (int index = 0; index < seed.length(); index++) {
            hash = 31 * hash + seed.charAt(index);
        }
        String value = Integer.toHexString(Math.abs(hash) + 0x5A17).toUpperCase(Locale.ENGLISH);
        while (value.length() < 6) {
            value = "0" + value;
        }
        return value.substring(0, 6);
    }

    /** The ticket as plain text, suitable for printing or writing to a file. */
    public static String text(Booking booking, Stadium stadium, StadiumEvent event) {
        StringBuilder ticket = new StringBuilder();
        String rule = "-".repeat(58);
        ticket.append(rule).append('\n');
        ticket.append("  STADIUM SELECT  •  E-TICKET").append('\n');
        ticket.append(rule).append('\n');
        ticket.append(String.format("  Reference    : %s%n", booking.getReference()));
        ticket.append(String.format("  Check code   : %s-%s%n",
                booking.getReference(), checkCode(booking)));
        ticket.append('\n');
        ticket.append(String.format("  Event        : %s%n", booking.getEvent()));
        if (event != null) {
            ticket.append(String.format("  Venue        : %s, %s%n",
                    stadium == null ? "" : stadium.getName(), stadium == null ? "" : stadium.getCity()));
            ticket.append(String.format("  Date         : %s%n", event.getDate().format(DATE)));
            ticket.append(String.format("  Starts       : %s   (Doors %s)%n",
                    event.getStartTime().format(TIME), event.getDoorsLabel()));
        } else {
            ticket.append(String.format("  Venue        : %s%n",
                    stadium == null ? "—" : stadium.getName()));
            if (booking.getEventDate() != null && booking.getEventStartTime() != null) {
                ticket.append(String.format("  Date         : %s%n", booking.getEventDate().format(DATE)));
                ticket.append(String.format("  Starts       : %s%n",
                        booking.getEventStartTime().format(TIME)));
            }
        }
        ticket.append('\n');
        ticket.append(String.format("  Seats        : %s%n", booking.getSeatDisplay()));
        ticket.append(String.format("  Seat count   : %d%n", booking.getSeats().size()));
        ticket.append('\n');
        ticket.append(String.format("  Booked by    : %s%n", booking.getCustomerName()));
        ticket.append(String.format("  Contact      : %s%n", booking.getEmail()));
        ticket.append(String.format("  Phone        : %s%n", booking.getPhone()));
        ticket.append('\n');
        ticket.append(String.format("  Seat subtotal: %s%n",
                BookingService.formatMoney(booking.getTotal() - BookingService.BOOKING_FEE)));
        ticket.append(String.format("  Booking fee  : %s%n",
                BookingService.formatMoney(BookingService.BOOKING_FEE)));
        ticket.append(String.format("  TOTAL PAID   : %s%n", BookingService.formatMoney(booking.getTotal())));
        ticket.append('\n');
        ticket.append(String.format("  Status       : %s%n", booking.getStatus().name()));
        ticket.append(String.format("  Booked on    : %s%n", stamp(booking.getCreatedAt())));
        ticket.append('\n');
        ticket.append(rule).append('\n');
        ticket.append("  Please arrive before the doors close. This ticket is the only").append('\n');
        ticket.append("  record of your seat; quote the reference at the entrance.").append('\n');
        ticket.append(rule).append('\n');
        return ticket.toString();
    }

    /** One CSV row, with quotes so commas inside values cannot break the columns. */
    public static String csvRow(Booking booking) {
        return String.join(",",
                quote(booking.getReference()),
                quote(booking.getEvent()),
                quote(booking.getStadiumId()),
                quote(booking.getSeatDisplay()),
                String.valueOf(booking.getSeats().size()),
                quote(BookingService.formatMoney(booking.getTotal())),
                quote(booking.getCustomerName()),
                quote(booking.getEmail()),
                quote(booking.getPhone()),
                quote(booking.getStatus().name()),
                quote(stamp(booking.getCreatedAt())));
    }

    public static String csvHeader() {
        return String.join(",",
                "Reference", "Event", "VenueId", "Seats", "SeatCount", "Total",
                "Customer", "Email", "Phone", "Status", "BookedOn");
    }

    /** A CSV export of every booking, newest first. */
    public static String csv(List<Booking> bookings) {
        StringBuilder sheet = new StringBuilder(csvHeader()).append('\n');
        for (Booking booking : bookings) {
            sheet.append(csvRow(booking)).append('\n');
        }
        return sheet.toString();
    }

    private static String quote(String value) {
        String safe = value == null ? "" : value;
        return '"' + safe.replace("\"", "\"\"") + '"';
    }
}
