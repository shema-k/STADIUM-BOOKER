package com.stadium.booking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * How full each venue is, worked out from the database rather than the screen.
 *
 * <p>Seat counts are summed across every event at a venue, so the capacity a row
 * is measured against is the venue's capacity multiplied by the number of events
 * it is hosting. Reporting a single event's capacity against seats booked for
 * eight events would make a busy ground look empty.
 */
public final class OccupancyReport {
    /** One venue's figures. */
    public static final class Row {
        private final Stadium stadium;
        private final int events;
        private final int seatCapacity;
        private final int seatsBooked;
        private final int[] seatsBySection;

        Row(Stadium stadium, int events, int seatCapacity, int seatsBooked, int[] seatsBySection) {
            this.stadium = stadium;
            this.events = events;
            this.seatCapacity = seatCapacity;
            this.seatsBooked = seatsBooked;
            this.seatsBySection = seatsBySection;
        }

        public Stadium getStadium() {
            return stadium;
        }

        public int getEvents() {
            return events;
        }

        /** Seats on sale in total, which is capacity multiplied by event count. */
        public int getSeatCapacity() {
            return seatCapacity;
        }

        public int getSeatsBooked() {
            return seatsBooked;
        }

        public int getSeatsAvailable() {
            return Math.max(0, seatCapacity - seatsBooked);
        }

        /** Percentage of seats still on sale, between 0 and 100. */
        public double getVacancyPercentage() {
            if (seatCapacity <= 0) {
                return 100.0;
            }
            return getSeatsAvailable() * 100.0 / seatCapacity;
        }

        public int getSeatsInSection(String sectionId) {
            int index = "ABCD".indexOf(sectionId);
            return index < 0 ? 0 : seatsBySection[index];
        }

    }

    private final List<Row> rows = new ArrayList<>();
    private final int totalCapacity;
    private final int totalBooked;
    private final int totalEvents;

    private OccupancyReport(List<Row> rows, int totalCapacity, int totalBooked, int totalEvents) {
        this.rows.addAll(rows);
        this.totalCapacity = totalCapacity;
        this.totalBooked = totalBooked;
        this.totalEvents = totalEvents;
    }

    /**
     * Builds the report from confirmed bookings.
     *
     * @param bookingService the service holding the reservations; may be null,
     *                       in which case every venue simply reads as empty
     */
    public static OccupancyReport compute(BookingService bookingService) {
        List<Row> rows = new ArrayList<>();
        int totalCapacity = 0;
        int totalBooked = 0;
        int totalEvents = 0;
        for (Stadium stadium : StadiumData.getStadiums()) {
            List<StadiumEvent> events = StadiumData.getEvents(stadium.getId());
            int perEventCapacity = stadium.getSeatCount();
            int[] bySection = new int[4];
            int booked = 0;
            for (StadiumEvent event : events) {
                Set<SeatKey> taken = bookingService == null
                        ? Collections.<SeatKey>emptySet() : bookingService.getBookedSeatKeys(event);
                booked += taken.size();
                for (SeatKey key : taken) {
                    int index = "ABCD".indexOf(key.getSection());
                    if (index >= 0) {
                        bySection[index]++;
                    }
                }
            }
            int capacity = perEventCapacity * Math.max(1, events.size());
            rows.add(new Row(stadium, events.size(), capacity, booked, bySection));
            totalCapacity += capacity;
            totalBooked += booked;
            totalEvents += events.size();
        }
        return new OccupancyReport(rows, totalCapacity, totalBooked, totalEvents);
    }

    public List<Row> getRows() {
        return rows;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public int getTotalBooked() {
        return totalBooked;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public double getOverallVacancyPercentage() {
        if (totalCapacity <= 0) {
            return 100.0;
        }
        return (totalCapacity - totalBooked) * 100.0 / totalCapacity;
    }
}
