package com.stadium.booking;

import java.util.Objects;

/** One of the four seating ends/sides of a stadium. */
public final class SeatSection {
    private final String id;
    private final String label;
    private final int rows;
    private final int seatsPerRow;
    private final double basePrice;

    public SeatSection(String id, String label, int rows, int seatsPerRow, double basePrice) {
        if (id == null || id.trim().isEmpty() || label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Section id and label are required");
        }
        if (rows < 1 || seatsPerRow < 1) {
            throw new IllegalArgumentException("Section rows and seats must be positive");
        }
        if (basePrice < 0) {
            throw new IllegalArgumentException("Section price cannot be negative");
        }
        this.id = id.trim().toUpperCase();
        this.label = label.trim();
        this.rows = rows;
        this.seatsPerRow = seatsPerRow;
        this.basePrice = basePrice;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public int getRows() {
        return rows;
    }

    public int getSeatsPerRow() {
        return seatsPerRow;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public int getSeatCount() {
        return rows * seatsPerRow;
    }

    public String searchableText() {
        return (id + " " + label).toLowerCase();
    }

    @Override
    public String toString() {
        return id + " - " + label;
    }
}
