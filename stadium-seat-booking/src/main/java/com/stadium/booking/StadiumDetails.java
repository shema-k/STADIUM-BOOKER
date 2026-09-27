package com.stadium.booking;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Everything known about one venue, worked out in one place.
 *
 * <p>The detail screen is meant to say all there is to say about a stadium, so
 * the facts are gathered and calculated here rather than in the Swing code. That
 * keeps the arithmetic out of the painting, and lets the tests check that the
 * page is telling the truth without a display.
 *
 * <p>Nothing here is invented. Every figure comes from the venue record, its
 * seating sections or its published events.
 */
public final class StadiumDetails {
    /** What is known about one of the four seating sections. */
    public static final class SectionFacts {
        private final SeatSection section;
        private final double frontPrice;
        private final double backPrice;

        SectionFacts(SeatSection section, double frontPrice, double backPrice) {
            this.section = section;
            this.frontPrice = frontPrice;
            this.backPrice = backPrice;
        }

        public SeatSection getSection() {
            return section;
        }

        public String getId() {
            return section.getId();
        }

        public String getLabel() {
            return section.getLabel();
        }

        public int getRows() {
            return section.getRows();
        }

        public int getSeatsPerRow() {
            return section.getSeatsPerRow();
        }

        public int getSeatCount() {
            return section.getSeatCount();
        }

        public double getFrontPrice() {
            return frontPrice;
        }

        public double getBackPrice() {
            return backPrice;
        }

        /** What a seat costs, from the front of the section to the back. */
        public String getPriceRange() {
            return frontPrice == backPrice
                    ? BookingService.formatMoney(frontPrice)
                    : BookingService.formatMoney(frontPrice) + " to "
                            + BookingService.formatMoney(backPrice);
        }
    }

    private final Stadium stadium;
    private final List<SectionFacts> sections = new ArrayList<>();
    private final List<StadiumEvent> events;
    private final List<StadiumAnnouncement> notices;
    private final int seatsOnSale;
    private final int seatsBooked;
    private final double cheapestSeat;
    private final double dearestSeat;
    private final Set<String> teams = new LinkedHashSet<>();
    private final Set<String> artists = new LinkedHashSet<>();
    private final BookingService booked;
    private final StadiumEvent priceReference;

    private StadiumDetails(Stadium stadium, List<StadiumEvent> events, BookingService service) {
        this.stadium = stadium;
        this.events = new ArrayList<>(events);
        this.booked = service;

        // Prices are taken from the cheapest upcoming event, so the range shown is
        // a price the customer can actually pay rather than a list price.
        this.priceReference = cheapestEvent();
        for (SeatSection section : stadium.getSections()) {
            double front = priceReference == null ? section.getBasePrice()
                    : priceFor(priceReference, section, 1);
            double back = priceReference == null ? section.getBasePrice()
                    : priceFor(priceReference, section, section.getRows());
            sections.add(new SectionFacts(section, front, back));
        }
        this.cheapestSeat = sections.stream().mapToDouble(SectionFacts::getBackPrice).min()
                .orElse(0);
        this.dearestSeat = sections.stream().mapToDouble(SectionFacts::getFrontPrice).max()
                .orElse(0);

        this.notices = StadiumData.getAnnouncements(stadium.getId());
        for (StadiumEvent event : this.events) {
            if (event.getTeamOne() != null && !event.getTeamOne().isBlank()) {
                teams.add(event.getTeamOne());
            }
            if (event.getTeamTwo() != null && !event.getTeamTwo().isBlank()) {
                teams.add(event.getTeamTwo());
            }
            if (event.getArtist() != null && !event.getArtist().isBlank()) {
                artists.add(event.getArtist());
            }
        }

        int onSale = 0;
        int taken = 0;
        for (StadiumEvent event : this.events) {
            onSale += stadium.getSeatCount();
            taken += seatsTakenOn(event);
        }
        this.seatsOnSale = onSale;
        this.seatsBooked = taken;
    }

    /** Collects everything known about a venue. */
    public static StadiumDetails of(Stadium stadium, BookingService service) {
        return new StadiumDetails(stadium, StadiumData.getEvents(stadium.getId()),
                service);
    }

    /** Seats already sold on one event, or none when no service was supplied. */
    private int seatsTakenOn(StadiumEvent event) {
        return booked == null ? 0 : booked.getBookedSeatKeys(event).size();
    }

    /**
     * The price of one seat, using the seat map's own row curve. An earlier version
     * carried a second, linear copy of that curve and quietly quoted prices the map
     * would never charge.
     */
    private double priceFor(StadiumEvent event, SeatSection section, int row) {
        return BookingService.roundMoney(section.getBasePrice()
                * BookingService.getRowPriceMultiplier(row, section.getRows())
                * event.getPriceFactor());
    }

    /** The upcoming event with the lowest prices, used as the reference for pricing. */
    private StadiumEvent cheapestEvent() {
        StadiumEvent cheapest = null;
        for (StadiumEvent event : events) {
            if (cheapest == null || event.getPriceFactor() < cheapest.getPriceFactor()) {
                cheapest = event;
            }
        }
        return cheapest;
    }

    public Stadium getStadium() {
        return stadium;
    }

    public List<SectionFacts> getSections() {
        return sections;
    }

    /** Upcoming events, soonest first. */
    public List<StadiumEvent> getEvents() {
        return events.stream()
                .sorted(java.util.Comparator.comparing(StadiumEvent::getDate)
                        .thenComparing(StadiumEvent::getStartTime)
                        .thenComparing(StadiumEvent::getId))
                .collect(Collectors.toList());
    }

    /**
     * The event the quoted prices come from: the cheapest one on sale, which may
     * not be the next one. Null when nothing is scheduled.
     */
    public StadiumEvent getPriceReferenceEvent() {
        return priceReference;
    }

    public StadiumEvent getNextEvent() {
        List<StadiumEvent> sorted = getEvents();
        return sorted.isEmpty() ? null : sorted.get(0);
    }

    public int getEventCount() {
        return events.size();
    }

    public int getGameCount() {
        return (int) events.stream().filter(StadiumEvent::isGame).count();
    }

    public int getConcertCount() {
        return getEventCount() - getGameCount();
    }

    public List<StadiumAnnouncement> getNotices() {
        return notices;
    }

    public boolean hasNotices() {
        return !notices.isEmpty();
    }

    /** Teams seen at this venue, without repeats. */
    public List<String> getTeams() {
        return new ArrayList<>(teams);
    }

    /** Artists seen at this venue, without repeats. */
    public List<String> getArtists() {
        return new ArrayList<>(artists);
    }

    /** Events that cannot currently be booked, and why. */
    public List<StadiumEvent> getBlockedEvents() {
        if (booked == null) {
            return List.of();
        }
        return events.stream().filter(event -> !booked.isBookingOpen(event))
                .collect(Collectors.toList());
    }

    public int getSeatsOnSale() {
        return seatsOnSale;
    }

    public int getSeatsBooked() {
        return seatsBooked;
    }

    public double getVacancyPercentage() {
        if (seatsOnSale <= 0) {
            return 100.0;
        }
        return (seatsOnSale - seatsBooked) * 100.0 / seatsOnSale;
    }

    public double getCheapestSeat() {
        return cheapestSeat;
    }

    public double getDearestSeat() {
        return dearestSeat;
    }

    public double getBookingFee() {
        return BookingService.BOOKING_FEE;
    }

    /** The full price span, cheapest seat to dearest. */
    public String getPriceSpan() {
        return BookingService.formatMoney(cheapestSeat) + " to "
                + BookingService.formatMoney(dearestSeat);
    }

    /** A one-line summary for a card or a header. */
    public String getSummary() {
        return stadium.getVenueType() + "  •  " + stadium.getSeatCount() + " seats  •  "
                + getEvents().size() + " upcoming";
    }
}
