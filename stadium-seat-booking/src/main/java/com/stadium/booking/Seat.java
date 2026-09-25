package com.stadium.booking;

import java.util.Objects;

/** An immutable seat in the stadium inventory. */
public final class Seat {
    private final SeatKey key;
    private final double price;

    public Seat(SeatKey key, double price) {
        this.key = Objects.requireNonNull(key, "key");
        if (price < 0) {
            throw new IllegalArgumentException("Seat price cannot be negative");
        }
        this.price = price;
    }

    public SeatKey getKey() {
        return key;
    }

    public double getPrice() {
        return price;
    }

    public String display() {
        return key.display();
    }

    @Override
    public String toString() {
        return display();
    }
}
