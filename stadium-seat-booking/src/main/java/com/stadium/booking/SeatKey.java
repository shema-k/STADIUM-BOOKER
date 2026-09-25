package com.stadium.booking;

import java.io.Serializable;
import java.util.Objects;

/** Uniquely identifies a seat by section, row and seat number. */
public final class SeatKey implements Comparable<SeatKey>, Serializable {
    private static final long serialVersionUID = 1L;

    private final String section;
    private final int row;
    private final int number;

    public SeatKey(String section, int row, int number) {
        if (section == null || section.trim().isEmpty()) {
            throw new IllegalArgumentException("Section is required");
        }
        if (row < 1 || number < 1) {
            throw new IllegalArgumentException("Row and seat number must be positive");
        }
        this.section = section.trim().toUpperCase();
        this.row = row;
        this.number = number;
    }

    public String getSection() {
        return section;
    }

    public int getRow() {
        return row;
    }

    public int getNumber() {
        return number;
    }

    /** Returns a compact label suitable for buttons and receipts. */
    public String display() {
        return String.format("%s%d-%02d", section, row, number);
    }

    @Override
    public int compareTo(SeatKey other) {
        int sectionComparison = section.compareTo(other.section);
        if (sectionComparison != 0) {
            return sectionComparison;
        }
        int rowComparison = Integer.compare(row, other.row);
        if (rowComparison != 0) {
            return rowComparison;
        }
        return Integer.compare(number, other.number);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SeatKey)) {
            return false;
        }
        SeatKey that = (SeatKey) other;
        return row == that.row && number == that.number && section.equals(that.section);
    }

    @Override
    public int hashCode() {
        return Objects.hash(section, row, number);
    }

    @Override
    public String toString() {
        return display();
    }
}
