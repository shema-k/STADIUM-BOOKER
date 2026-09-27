package com.stadium.booking;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contains the venue directory, event schedules, seat inventory and booking
 * rules. The service has no Swing dependency, so the rules can be tested and
 * reused independently of the desktop interface.
 */
@SuppressWarnings("this-escape")
public class BookingService {
    public static final int MAX_SEATS_PER_BOOKING = 6;
    /** Ticketing fee charged once per reservation, in Ugandan shillings. */
    public static final double BOOKING_FEE = 15000.0;

    private final Map<SeatKey, Seat> seatInventory = new LinkedHashMap<>();
    private final List<Booking> bookings = new ArrayList<>();
    private final List<StadiumAnnouncement> submittedRequests = new ArrayList<>();
    private final BookingStore store;
    private StadiumEvent activeEvent;
    private Stadium stadiumOverride;
    private int nextReferenceNumber = 1;

    public BookingService() {
        this(new BookingStore(new File("stadium-bookings.dat").toPath()));
    }

    public BookingService(BookingStore store) {
        this.store = store;
        activeEvent = StadiumData.getEvents().get(0);
        createInventory();
        bookings.addAll(store == null ? Collections.<Booking>emptyList() : store.read());
        updateNextReferenceNumber();
    }

    /** Compatibility constructor for the original compact single-event demo. */
    public BookingService(String legacyEvent, BookingStore store) {
        this(store);
        if (legacyEvent != null && !legacyEvent.trim().isEmpty()) {
            stadiumOverride = createLegacyStadium();
            activeEvent = new StadiumEvent("legacy-event", stadiumOverride.getId(), EventType.GAME,
                    legacyEvent.trim(), "General event", "Home", "Away", null,
                    "Legacy event schedule", java.time.LocalDate.now().plusDays(1),
                    java.time.LocalTime.of(19, 0), java.time.LocalTime.of(18, 0), 1.0);
            createInventory();
        }
    }

    private Stadium createLegacyStadium() {
        List<SeatSection> sections = new ArrayList<>();
        sections.add(new SeatSection("A", "North end", 6, 10, 45));
        sections.add(new SeatSection("B", "East side", 6, 10, 40));
        sections.add(new SeatSection("C", "South end", 6, 10, 35));
        sections.add(new SeatSection("D", "West side", 6, 10, 30));
        return new Stadium("legacy-stadium", "Legacy Arena", "Demo", "Demo",
                "Demo venue", "A compact legacy venue for compatibility testing.",
                "Demo address", 240, "#2563eb", StadiumShape.BOX, sections);
    }

    private void createInventory() {
        seatInventory.clear();
        Stadium stadium = getActiveStadium();
        if (stadium == null || activeEvent == null) {
            return;
        }
        for (SeatSection section : stadium.getSections()) {
            for (int row = 1; row <= section.getRows(); row++) {
                for (int number = 1; number <= section.getSeatsPerRow(); number++) {
                    SeatKey key = new SeatKey(section.getId(), row, number);
                    seatInventory.put(key, new Seat(key, getSeatPrice(section, row)));
                }
            }
        }
    }

    /** Price decreases from the front rows toward the back rows. */
    public double getSeatPrice(SeatSection section, int row) {
        if (section == null) {
            return 0.0;
        }
        int safeRow = Math.max(1, Math.min(row, section.getRows()));
        double rowMultiplier = getRowPriceMultiplier(safeRow, section.getRows());
        double eventMultiplier = activeEvent == null ? 1.0 : activeEvent.getPriceFactor();
        return roundMoney(section.getBasePrice() * rowMultiplier * eventMultiplier);
    }

    public double getSeatPrice(SeatKey key) {
        Seat seat = seatInventory.get(key);
        return seat == null ? 0.0 : seat.getPrice();
    }

    public double getRowPriceMultiplier(int row, int rows) {
        if (rows <= 1) {
            return 1.0;
        }
        int safeRow = Math.max(1, Math.min(row, rows));
        double position = (safeRow - 1.0) / (rows - 1.0);
        if (position <= 0.20) {
            return 1.40;
        }
        if (position <= 0.50) {
            return 1.20;
        }
        if (position <= 0.80) {
            return 0.95;
        }
        return 0.70;
    }

    /**
     * Rounds a charge to the nearest 500 shillings, which is how Ugandan
     * ticketing prices are normally rounded for cash sales.
     */
    private double roundMoney(double value) {
        return Math.round(value / 500.0) * 500.0;
    }

    private void updateNextReferenceNumber() {
        int highest = 0;
        for (Booking booking : bookings) {
            try {
                String numberPart = booking.getReference().replaceFirst("^ST-", "");
                highest = Math.max(highest, Integer.parseInt(numberPart));
            } catch (NumberFormatException ignored) {
                // Keep generating a valid reference for old/custom records.
            }
        }
        nextReferenceNumber = highest + 1;
    }

    public List<Stadium> getStadiums() {
        return StadiumData.getStadiums();
    }

    public List<StadiumEvent> getEvents(String stadiumId) {
        return StadiumData.getEvents(stadiumId);
    }

    public List<StadiumEvent> getEventsForStadium(Stadium stadium) {
        return stadium == null ? new ArrayList<>() : getEvents(stadium.getId());
    }

    public List<StadiumAnnouncement> getAnnouncements(String stadiumId) {
        List<StadiumAnnouncement> announcements = new ArrayList<>(
                StadiumData.getAnnouncements(stadiumId));
        for (StadiumAnnouncement request : submittedRequests) {
            if (request.getStadiumId().equals(stadiumId)) {
                announcements.add(request);
            }
        }
        announcements.sort((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()));
        return announcements;
    }

    public List<StadiumAnnouncement> getAnnouncements() {
        List<StadiumAnnouncement> announcements = new ArrayList<>(StadiumData.getAnnouncements());
        announcements.addAll(submittedRequests);
        announcements.sort((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()));
        return announcements;
    }

    public StadiumAnnouncement addSpecialRequest(String stadiumId, String category, String message) {
        Stadium stadium = StadiumData.getStadium(stadiumId);
        if (stadium == null) {
            throw new IllegalArgumentException("Choose a valid stadium");
        }
        String normalizedCategory = category == null || category.trim().isEmpty()
                ? "General request" : category.trim();
        String normalizedMessage = message == null ? "" : message.trim();
        if (normalizedMessage.length() < 5) {
            throw new IllegalArgumentException("Please describe the request in a little more detail");
        }
        LocalDateTime now = LocalDateTime.now();
        StadiumAnnouncement request = new StadiumAnnouncement(
                "request-" + System.currentTimeMillis(), stadiumId, "", AnnouncementType.SPECIAL_REQUEST,
                normalizedCategory, normalizedMessage, now, now.plusDays(30));
        submittedRequests.add(request);
        return request;
    }

    public StadiumAnnouncement getBlockingAnnouncement(StadiumEvent event) {
        if (event == null) {
            return null;
        }
        for (StadiumAnnouncement announcement : getAnnouncements(event.getStadiumId())) {
            if (announcement.isBlocking() && announcement.appliesToEvent(event.getId())) {
                return announcement;
            }
        }
        return null;
    }

    public StadiumEvent getActiveEvent() {
        return activeEvent;
    }

    public Stadium getActiveStadium() {
        if (stadiumOverride != null && activeEvent != null
                && stadiumOverride.getId().equals(activeEvent.getStadiumId())) {
            return stadiumOverride;
        }
        return activeEvent == null ? null : StadiumData.getStadium(activeEvent.getStadiumId());
    }

    public StadiumShape getActiveShape() {
        Stadium stadium = getActiveStadium();
        return stadium == null ? StadiumShape.BOX : stadium.getShape();
    }

    public void selectEvent(StadiumEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("An event must be selected before booking");
        }
        if (!"legacy-event".equals(event.getId())) {
            stadiumOverride = null;
        }
        activeEvent = event;
        createInventory();
    }

    public String getEvent() {
        return activeEvent == null ? "" : activeEvent.getHeadline();
    }

    public List<Seat> getSeats() {
        return new ArrayList<>(seatInventory.values());
    }

    public List<Seat> getSeats(String sectionId) {
        if (sectionId == null) {
            return new ArrayList<>();
        }
        return seatInventory.values().stream()
                .filter(seat -> seat.getKey().getSection().equalsIgnoreCase(sectionId))
                .collect(Collectors.toList());
    }

    public Seat getSeat(SeatKey key) {
        return seatInventory.get(key);
    }

    public SeatStatus getStatus(SeatKey key) {
        return isBooked(key) ? SeatStatus.BOOKED : SeatStatus.AVAILABLE;
    }

    public boolean isSeatSelectable(SeatKey key) {
        return getStatus(key) == SeatStatus.AVAILABLE && isBookingOpen(activeEvent);
    }

    public boolean isBooked(SeatKey key) {
        if (key == null || activeEvent == null) {
            return false;
        }
        return getBookedSeatKeys(activeEvent).contains(key);
    }

    public Set<SeatKey> getBookedSeatKeys(StadiumEvent event) {
        Set<SeatKey> booked = new LinkedHashSet<>();
        for (Booking booking : bookings) {
            if (booking.isConfirmed() && belongsToEvent(booking, event)) {
                booked.addAll(booking.getSeats());
            }
        }
        return booked;
    }

    private boolean belongsToEvent(Booking booking, StadiumEvent event) {
        if (event == null) {
            return false;
        }
        String eventId = booking.getEventId();
        if (eventId != null && !eventId.isEmpty()) {
            return event.getId().equals(eventId);
        }
        return event.getHeadline().equals(booking.getEvent());
    }

    public int getTotalSeatCount(StadiumEvent event) {
        if (event == null) {
            return 0;
        }
        Stadium stadium = stadiumOverride != null && event.getId().equals("legacy-event")
                ? stadiumOverride : StadiumData.getStadium(event.getStadiumId());
        return stadium == null ? 0 : stadium.getSeatCount();
    }

    public int getAvailableSeatCount(StadiumEvent event) {
        int total = getTotalSeatCount(event);
        if (total == 0) {
            return 0;
        }
        return Math.max(0, total - getBookedSeatKeys(event).size());
    }

    public int getAvailableSeatCount() {
        return getAvailableSeatCount(activeEvent);
    }

    public int getBookedSeatCount() {
        return getBookedSeatKeys(activeEvent).size();
    }

    public double getVacancyPercentage(StadiumEvent event) {
        int total = getTotalSeatCount(event);
        if (total == 0) {
            return 0.0;
        }
        double percentage = getAvailableSeatCount(event) * 100.0 / total;
        return Math.round(percentage * 1000.0) / 1000.0;
    }

    public double getVacancyPercentage() {
        return getVacancyPercentage(activeEvent);
    }

    public boolean isBookingOpen(StadiumEvent event) {
        return event != null && event.isBookingOpen() && getBlockingAnnouncement(event) == null;
    }

    public String getBookingRestrictionMessage(StadiumEvent event) {
        StadiumAnnouncement blocking = getBlockingAnnouncement(event);
        if (blocking != null) {
            return blocking.getTitle() + " — " + blocking.getMessage();
        }
        if (event != null && !event.isBookingOpen()) {
            return "Bookings closed at " + event.getBookingDeadlineLabel();
        }
        return null;
    }

    public boolean isBookingOpen() {
        return isBookingOpen(activeEvent);
    }

    public String getBookingDeadlineLabel(StadiumEvent event) {
        return event == null ? "—" : event.getBookingDeadlineLabel();
    }

    public String getBookingDeadlineLabel() {
        return getBookingDeadlineLabel(activeEvent);
    }

    public List<SeatSection> getSectionDefinitions() {
        Stadium stadium = getActiveStadium();
        return stadium == null ? new ArrayList<>() : stadium.getSections();
    }

    public List<String> getSections() {
        List<String> sections = new ArrayList<>();
        for (SeatSection section : getSectionDefinitions()) {
            sections.add(section.getId());
        }
        return sections;
    }

    public int getRows(String sectionId) {
        SeatSection section = findSection(sectionId);
        return section == null ? 0 : section.getRows();
    }

    public int getSeatsPerRow(String sectionId) {
        SeatSection section = findSection(sectionId);
        return section == null ? 0 : section.getSeatsPerRow();
    }

    public double getSectionPrice(String sectionId) {
        SeatSection section = findSection(sectionId);
        return section == null ? 0.0 : getSeatPrice(section, 1);
    }

    private SeatSection findSection(String sectionId) {
        Stadium stadium = getActiveStadium();
        return stadium == null ? null : stadium.getSection(sectionId);
    }

    public List<Booking> getBookings() {
        return Collections.unmodifiableList(new ArrayList<>(bookings));
    }

    public List<Booking> getBookingsForStadium(String stadiumId) {
        return bookings.stream()
                .filter(booking -> stadiumId == null || stadiumId.equals(booking.getStadiumId()))
                .collect(Collectors.toList());
    }

    public List<Booking> getBookingsForEvent(StadiumEvent event) {
        if (event == null) {
            return new ArrayList<>();
        }
        return bookings.stream().filter(booking -> belongsToEvent(booking, event))
                .collect(Collectors.toList());
    }

    public double totalFor(List<Seat> selectedSeats) {
        if (selectedSeats == null) {
            return 0.0;
        }
        double total = 0.0;
        for (Seat seat : selectedSeats) {
            Seat inventorySeat = seatInventory.get(seat.getKey());
            if (inventorySeat != null) {
                total += inventorySeat.getPrice();
            }
        }
        return roundMoney(total);
    }

    public double getBookingFee() {
        return BOOKING_FEE;
    }

    /**
     * Formats an amount as Ugandan shillings, for example {@code UGX 89,500}.
     * Shared by every screen that shows a price so the format cannot drift.
     */
    public static String formatMoney(double amount) {
        return "UGX " + java.text.NumberFormat
                .getIntegerInstance(Locale.US)
                .format(Math.round(amount));
    }

    public double getTotalCharge(List<Seat> selectedSeats) {
        if (selectedSeats == null || selectedSeats.isEmpty()) {
            return 0.0;
        }
        return roundMoney(totalFor(selectedSeats) + BOOKING_FEE);
    }

    public String getRowTier(SeatKey key) {
        if (key == null) {
            return "Seat";
        }
        int rows = getRows(key.getSection());
        if (rows <= 1) {
            return "Standard seat";
        }
        double position = (key.getRow() - 1.0) / (rows - 1.0);
        if (position <= 0.20) {
            return "Front premium";
        }
        if (position <= 0.50) {
            return "Middle standard";
        }
        if (position <= 0.80) {
            return "Lower value";
        }
        return "Back value";
    }

    /**
     * Validates and commits a reservation for the currently selected event. A
     * seat is only marked booked after every selected seat has passed checks.
     */
    public synchronized Booking book(String customerName,
                                      String email,
                                      String phone,
                                      List<Seat> selectedSeats) {
        if (activeEvent == null) {
            throw new IllegalArgumentException("Choose an event before booking");
        }
        if (!isBookingOpen(activeEvent)) {
            throw new IllegalArgumentException(getBookingRestrictionMessage(activeEvent));
        }

        String normalizedName = customerName == null ? "" : customerName.trim();
        String normalizedEmail = email == null ? "" : email.trim();
        String normalizedPhone = phone == null ? "" : phone.trim();

        if (normalizedName.length() < 2) {
            throw new IllegalArgumentException("Please enter your name");
        }
        if (!normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Please enter a valid email address");
        }
        if (!normalizedPhone.matches("^[0-9+() .-]{7,20}$")) {
            throw new IllegalArgumentException("Please enter a valid phone number");
        }
        if (selectedSeats == null || selectedSeats.isEmpty()) {
            throw new IllegalArgumentException("Select at least one seat");
        }

        Set<SeatKey> selectedKeys = new LinkedHashSet<>();
        for (Seat seat : selectedSeats) {
            if (seat == null || !seatInventory.containsKey(seat.getKey())) {
                throw new IllegalArgumentException("One of the selected seats is not available");
            }
            if (!isSeatSelectable(seat.getKey())) {
                throw new IllegalArgumentException("Seat " + seat.getKey().display() + " is not selectable");
            }
            selectedKeys.add(seat.getKey());
        }
        if (selectedKeys.size() > MAX_SEATS_PER_BOOKING) {
            throw new IllegalArgumentException("A reservation can contain up to "
                    + MAX_SEATS_PER_BOOKING + " seats");
        }
        for (SeatKey key : selectedKeys) {
            if (isBooked(key)) {
                throw new IllegalArgumentException("Seat " + key.display() + " has just been booked");
            }
        }

        // Customer identity is stored for the receipt and history only. There is intentionally
        // no limit on how many reservations one person may create.
        List<SeatKey> keys = new ArrayList<>(selectedKeys);
        keys.sort(SeatKey::compareTo);
        double total = getTotalCharge(keys.stream()
                .map(seatInventory::get)
                .collect(Collectors.toList()));
        String reference = "ST-" + nextReferenceNumber;
        nextReferenceNumber++;
        Booking booking = new Booking(reference, activeEvent.getStadiumId(), activeEvent.getId(),
                activeEvent.getHeadline(), normalizedName, normalizedEmail, normalizedPhone,
                keys, total, Instant.now(), activeEvent.getDate(), activeEvent.getStartTime());
        bookings.add(booking);
        persist();
        return booking;
    }

    /** Cancels a reservation and returns its seats to that event's inventory. */
    public synchronized boolean cancel(String reference) {
        if (reference == null) {
            return false;
        }
        for (Booking booking : bookings) {
            if (booking.getReference().equals(reference) && booking.isConfirmed()) {
                booking.cancel();
                persist();
                return true;
            }
        }
        return false;
    }

    private void persist() {
        if (store == null) {
            return;
        }
        try {
            store.write(bookings);
        } catch (Exception ignored) {
            // The in-memory reservation remains usable if the data file is locked.
        }
    }
}
