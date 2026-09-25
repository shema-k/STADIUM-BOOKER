package com.stadium.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/** A game or concert scheduled at a stadium. */
public final class StadiumEvent {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter DEADLINE_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM • HH:mm", Locale.ENGLISH);

    private final String id;
    private final String stadiumId;
    private final EventType type;
    private final String title;
    private final String sport;
    private final String teamOne;
    private final String teamTwo;
    private final String artist;
    private final String description;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime doorsTime;
    private final LocalDateTime bookingDeadline;
    private final double priceFactor;

    public StadiumEvent(String id,
                        String stadiumId,
                        EventType type,
                        String title,
                        String sport,
                        String teamOne,
                        String teamTwo,
                        String artist,
                        String description,
                        LocalDate date,
                        LocalTime startTime,
                        LocalTime doorsTime,
                        double priceFactor) {
        this(id, stadiumId, type, title, sport, teamOne, teamTwo, artist, description,
                date, startTime, doorsTime, priceFactor,
                date.atTime(startTime).minusHours(2));
    }

    public StadiumEvent(String id,
                        String stadiumId,
                        EventType type,
                        String title,
                        String sport,
                        String teamOne,
                        String teamTwo,
                        String artist,
                        String description,
                        LocalDate date,
                        LocalTime startTime,
                        LocalTime doorsTime,
                        double priceFactor,
                        LocalDateTime bookingDeadline) {
        this.id = Objects.requireNonNull(id, "id");
        this.stadiumId = Objects.requireNonNull(stadiumId, "stadiumId");
        this.type = Objects.requireNonNull(type, "type");
        this.title = Objects.requireNonNull(title, "title");
        this.sport = sport;
        this.teamOne = teamOne;
        this.teamTwo = teamTwo;
        this.artist = artist;
        this.description = Objects.requireNonNull(description, "description");
        this.date = Objects.requireNonNull(date, "date");
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.doorsTime = doorsTime;
        this.bookingDeadline = Objects.requireNonNull(bookingDeadline, "bookingDeadline");
        if (priceFactor <= 0) {
            throw new IllegalArgumentException("Event price factor must be positive");
        }
        this.priceFactor = priceFactor;
    }

    public String getId() {
        return id;
    }

    public String getStadiumId() {
        return stadiumId;
    }

    public EventType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getSport() {
        return sport;
    }

    public String getTeamOne() {
        return teamOne;
    }

    public String getTeamTwo() {
        return teamTwo;
    }

    public String getArtist() {
        return artist;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getDoorsTime() {
        return doorsTime;
    }

    public LocalDateTime getBookingDeadline() {
        return bookingDeadline;
    }

    public double getPriceFactor() {
        return priceFactor;
    }

    public boolean isGame() {
        return type == EventType.GAME;
    }

    public boolean isConcert() {
        return type == EventType.CONCERT;
    }

    public boolean isBookingOpen() {
        return LocalDateTime.now().isBefore(bookingDeadline);
    }

    public String getBookingDeadlineLabel() {
        return DEADLINE_FORMATTER.format(bookingDeadline);
    }

    public String getCountdownLabel() {
        return BookingCountdown.format(bookingDeadline);
    }

    public String getBookingStateLabel() {
        return isBookingOpen() ? "Booking open" : "Booking closed";
    }

    public String getHeadline() {
        if (isGame()) {
            return teamOne + " vs " + teamTwo;
        }
        return title;
    }

    public String getEventDetails() {
        if (isGame()) {
            return sport + "  •  " + teamOne + " vs " + teamTwo;
        }
        return artist + "  •  Live concert";
    }

    public String getDateLabel() {
        return DATE_FORMATTER.format(date);
    }

    public String getTimeLabel() {
        return TIME_FORMATTER.format(startTime);
    }

    public String getDoorsLabel() {
        return doorsTime == null ? "" : TIME_FORMATTER.format(doorsTime);
    }

    public String getWhenLabel() {
        return getDateLabel() + "  •  " + getTimeLabel();
    }

    public String searchableText() {
        return String.join(" ",
                id, stadiumId, type.getLabel(), title,
                sport == null ? "" : sport,
                teamOne == null ? "" : teamOne,
                teamTwo == null ? "" : teamTwo,
                artist == null ? "" : artist,
                description, getDateLabel(), getTimeLabel(), getBookingDeadlineLabel(), date.toString())
                .toLowerCase(Locale.ENGLISH);
    }

    @Override
    public String toString() {
        return getHeadline();
    }
}
