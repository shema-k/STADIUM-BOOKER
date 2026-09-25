package com.stadium.booking;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** A confirmed (or later cancelled) seat reservation. */
public final class Booking implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String reference;
    private final String stadiumId;
    private final String eventId;
    private final String event;
    private final String customerName;
    private final String email;
    private final String phone;
    private final ArrayList<SeatKey> seats;
    private final double total;
    private final Instant createdAt;
    private final LocalDate eventDate;
    private final LocalTime eventStartTime;
    private BookingStatus status;

    /** Compatibility constructor for bookings created by the original demo. */
    public Booking(String reference,
                   String event,
                   String customerName,
                   String email,
                   String phone,
                   List<SeatKey> seats,
                   double total,
                   Instant createdAt) {
        this(reference, "", "", event, customerName, email, phone, seats, total,
                createdAt, null, null);
    }

    public Booking(String reference,
                   String stadiumId,
                   String eventId,
                   String event,
                   String customerName,
                   String email,
                   String phone,
                   List<SeatKey> seats,
                   double total,
                   Instant createdAt) {
        this(reference, stadiumId, eventId, event, customerName, email, phone, seats, total,
                createdAt, null, null);
    }

    public Booking(String reference,
                   String stadiumId,
                   String eventId,
                   String event,
                   String customerName,
                   String email,
                   String phone,
                   List<SeatKey> seats,
                   double total,
                   Instant createdAt,
                   LocalDate eventDate,
                   LocalTime eventStartTime) {
        this.reference = Objects.requireNonNull(reference, "reference");
        this.stadiumId = stadiumId == null ? "" : stadiumId;
        this.eventId = eventId == null ? "" : eventId;
        this.event = Objects.requireNonNull(event, "event");
        this.customerName = Objects.requireNonNull(customerName, "customerName");
        this.email = Objects.requireNonNull(email, "email");
        this.phone = Objects.requireNonNull(phone, "phone");
        this.seats = new ArrayList<>(Objects.requireNonNull(seats, "seats"));
        this.total = total;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.eventDate = eventDate;
        this.eventStartTime = eventStartTime;
        this.status = BookingStatus.CONFIRMED;
    }

    public String getReference() {
        return reference;
    }

    public String getStadiumId() {
        return stadiumId == null ? "" : stadiumId;
    }

    public String getEventId() {
        return eventId == null ? "" : eventId;
    }

    public String getEvent() {
        return event;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public List<SeatKey> getSeats() {
        return Collections.unmodifiableList(seats);
    }

    public String getSeatDisplay() {
        return seats.stream()
                .sorted()
                .map(SeatKey::display)
                .collect(Collectors.joining(", "));
    }

    public double getTotal() {
        return total;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public LocalTime getEventStartTime() {
        return eventStartTime;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public boolean isConfirmed() {
        return status == BookingStatus.CONFIRMED;
    }

    public void cancel() {
        status = BookingStatus.CANCELLED;
    }

    @Override
    public String toString() {
        return reference + " - " + getSeatDisplay();
    }
}
