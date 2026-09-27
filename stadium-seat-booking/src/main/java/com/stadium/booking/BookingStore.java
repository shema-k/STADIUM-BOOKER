package com.stadium.booking;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Persistent H2 database for reservations.
 *
 * <p>Bookings are written one row at a time inside a transaction. An earlier
 * version deleted the whole table and re-inserted every booking, which silently
 * destroyed a confirmed booking whenever two people saved at the same time.
 *
 * <p>Seats are also stored in their own {@code booking_seats} table with a
 * primary key of event plus seat position. The database therefore refuses a
 * second booking of the same seat, even if two copies of the application are
 * open at once, instead of letting one overwrite the other.
 */
public final class BookingStore {
    private final Path file;
    private final String jdbcUrl;

    public BookingStore(Path file) {
        this.file = file;
        this.jdbcUrl = createJdbcUrl(file);
    }

    private String createJdbcUrl(Path databaseFile) {
        if (databaseFile == null) {
            return null;
        }
        String path = databaseFile.toAbsolutePath().toString();
        if (path.endsWith(".dat")) {
            path = path.substring(0, path.length() - 4);
        }
        return "jdbc:h2:file:" + path.replace('\\', '/') + ";DB_CLOSE_ON_EXIT=FALSE";
    }

    private Connection openConnection() throws SQLException, IOException {
        if (jdbcUrl == null) {
            throw new IOException("No booking database path configured");
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return DriverManager.getConnection(jdbcUrl, "sa", "");
    }

    private void initializeSchema() throws SQLException, IOException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bookings ("
                    + "reference VARCHAR(64) PRIMARY KEY, "
                    + "stadium_id VARCHAR(80) NOT NULL, "
                    + "event_id VARCHAR(80) NOT NULL, "
                    + "event_name VARCHAR(255) NOT NULL, "
                    + "customer_name VARCHAR(160) NOT NULL, "
                    + "email VARCHAR(255) NOT NULL, "
                    + "phone VARCHAR(40) NOT NULL, "
                    + "seats VARCHAR(8000) NOT NULL, "
                    + "total DECIMAL(12,2) NOT NULL, "
                    + "created_at BIGINT NOT NULL, "
                    + "event_date DATE, "
                    + "event_start_time TIME, "
                    + "status VARCHAR(20) NOT NULL"
                    + ")");
            // One row per seat. The primary key makes a seat unique per event, so a
            // double booking is rejected by the database rather than by luck.
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS booking_seats ("
                    + "event_id VARCHAR(80) NOT NULL, "
                    + "section VARCHAR(8) NOT NULL, "
                    + "seat_row INT NOT NULL, "
                    + "seat_number INT NOT NULL, "
                    + "reference VARCHAR(64) NOT NULL, "
                    + "PRIMARY KEY (event_id, section, seat_row, seat_number)"
                    + ")");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_seats_reference "
                    + "ON booking_seats(reference)");
        }
    }

    public List<Booking> read() {
        if (jdbcUrl == null) {
            return new ArrayList<>();
        }
        try {
            initializeSchema();
            List<Booking> bookings = readRows();
            if (bookings.isEmpty()) {
                List<Booking> legacy = readLegacyBookings();
                if (!legacy.isEmpty()) {
                    for (Booking booking : legacy) {
                        save(booking);
                    }
                    return legacy;
                }
            }
            return bookings;
        } catch (IOException | SQLException | RuntimeException exception) {
            // A damaged or unavailable database should not stop the GUI opening.
            return new ArrayList<>();
        }
    }

    private List<Booking> readRows() throws SQLException, IOException {
        List<Booking> bookings = new ArrayList<>();
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(
                     "SELECT reference, stadium_id, event_id, event_name, customer_name, "
                             + "email, phone, seats, total, created_at, event_date, "
                             + "event_start_time, status FROM bookings "
                             + "ORDER BY created_at DESC, reference DESC")) {
            while (results.next()) {
                String reference = results.getString("reference");
                String stadiumId = results.getString("stadium_id");
                String eventId = results.getString("event_id");
                String eventName = results.getString("event_name");
                String customerName = results.getString("customer_name");
                String email = results.getString("email");
                String phone = results.getString("phone");
                List<SeatKey> seats = decodeSeats(results.getString("seats"));
                double total = results.getBigDecimal("total").doubleValue();
                Instant createdAt = Instant.ofEpochMilli(results.getLong("created_at"));
                LocalDate eventDate = toLocalDate(results.getDate("event_date"));
                LocalTime eventTime = toLocalTime(results.getTime("event_start_time"));
                String statusValue = results.getString("status");
                Booking booking = new Booking(reference, stadiumId, eventId, eventName,
                        customerName, email, phone, seats, total, createdAt, eventDate, eventTime);
                if ("CANCELLED".equalsIgnoreCase(statusValue)) {
                    booking.cancel();
                }
                bookings.add(booking);
            }
        }
        return bookings;
    }

    /**
     * Allocates a booking reference that is not already used in the database.
     *
     * <p>References used to be a simple counter held in memory, so two copies of
     * the application both started at ST-1 and the second silently overwrote the
     * first. Reading the database here makes the reference unique no matter how
     * many instances are open.
     */
    public String allocateReference() throws IOException {
        if (jdbcUrl == null) {
            return "ST-" + (1 + (int) (Math.random() * 8999));
        }
        try {
            initializeSchema();
            for (int attempt = 0; attempt < 50; attempt++) {
                String candidate = "ST-" + UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 6).toUpperCase(Locale.ENGLISH);
                if (!referenceExists(candidate)) {
                    return candidate;
                }
            }
        } catch (SQLException exception) {
            throw new IOException("Could not allocate a booking reference", exception);
        }
        throw new IOException("Could not allocate a free booking reference");
    }

    private boolean referenceExists(String reference) throws SQLException, IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM bookings WHERE reference = ?")) {
            statement.setString(1, reference);
            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }

    /**
     * Inserts or updates a single booking, together with its seat rows.
     *
     * @throws SeatAlreadyBookedException if another booking has taken one of the seats
     * @throws IOException                if the booking could not be stored
     */
    public void save(Booking booking) throws IOException {
        if (jdbcUrl == null) {
            return;
        }
        try {
            initializeSchema();
            try (Connection connection = openConnection()) {
                boolean previousAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    // Release this booking's own seats first so a re-save is allowed.
                    try (PreparedStatement release = connection.prepareStatement(
                            "DELETE FROM booking_seats WHERE reference = ?")) {
                        release.setString(1, booking.getReference());
                        release.executeUpdate();
                    }
                    if (booking.isConfirmed()) {
                        try (PreparedStatement claim = connection.prepareStatement(
                                "INSERT INTO booking_seats "
                                        + "(event_id, section, seat_row, seat_number, reference) "
                                        + "VALUES (?, ?, ?, ?, ?)")) {
                            for (SeatKey key : booking.getSeats()) {
                                claim.setString(1, booking.getEventId());
                                claim.setString(2, key.getSection());
                                claim.setInt(3, key.getRow());
                                claim.setInt(4, key.getNumber());
                                claim.setString(5, booking.getReference());
                                claim.addBatch();
                            }
                            claim.executeBatch();
                        }
                    }
                    try (PreparedStatement upsert = connection.prepareStatement(
                            "MERGE INTO bookings (reference, stadium_id, event_id, event_name, "
                                    + "customer_name, email, phone, seats, total, created_at, "
                                    + "event_date, event_start_time, status) "
                                    + "KEY (reference) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        upsert.setString(1, booking.getReference());
                        upsert.setString(2, booking.getStadiumId());
                        upsert.setString(3, booking.getEventId());
                        upsert.setString(4, booking.getEvent());
                        upsert.setString(5, booking.getCustomerName());
                        upsert.setString(6, booking.getEmail());
                        upsert.setString(7, booking.getPhone());
                        upsert.setString(8, encodeSeats(booking.getSeats()));
                        upsert.setBigDecimal(9, java.math.BigDecimal.valueOf(booking.getTotal()));
                        upsert.setLong(10, booking.getCreatedAt().toEpochMilli());
                        if (booking.getEventDate() == null) {
                            upsert.setNull(11, java.sql.Types.DATE);
                        } else {
                            upsert.setDate(11, Date.valueOf(booking.getEventDate()));
                        }
                        if (booking.getEventStartTime() == null) {
                            upsert.setNull(12, java.sql.Types.TIME);
                        } else {
                            upsert.setTime(12, Time.valueOf(booking.getEventStartTime()));
                        }
                        upsert.setString(13, booking.getStatus().name());
                        upsert.executeUpdate();
                    }
                    connection.commit();
                } catch (SQLException | RuntimeException exception) {
                    connection.rollback();
                    throw asRuntime(exception, booking);
                } finally {
                    connection.setAutoCommit(previousAutoCommit);
                }
            }
        } catch (SeatAlreadyBookedException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw new IOException("Could not save booking " + booking.getReference(), exception);
        }
    }

    private RuntimeException asRuntime(Exception exception, Booking booking) {
        // H2 reports a primary key clash on a duplicate seat as an integrity violation.
        String state = exception instanceof SQLException sql ? sql.getSQLState() : null;
        if (state != null && state.startsWith("23")) {
            return new SeatAlreadyBookedException(booking);
        }
        return new IllegalStateException("Could not save booking " + booking.getReference(), exception);
    }

    private List<Booking> readLegacyBookings() {
        if (file == null || !Files.exists(file) || !file.getFileName().toString().endsWith(".dat")) {
            return new ArrayList<>();
        }
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(file))) {
            Object value = input.readObject();
            if (!(value instanceof List<?>)) {
                return new ArrayList<>();
            }
            List<Booking> bookings = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item instanceof Booking) {
                    bookings.add((Booking) item);
                }
            }
            return bookings;
        } catch (Exception exception) {
            return new ArrayList<>();
        }
    }

    private String encodeSeats(List<SeatKey> seats) {
        StringBuilder encoded = new StringBuilder();
        for (SeatKey key : seats) {
            if (encoded.length() > 0) {
                encoded.append(';');
            }
            encoded.append(key.getSection()).append('|').append(key.getRow())
                    .append('|').append(key.getNumber());
        }
        return encoded.toString();
    }

    private List<SeatKey> decodeSeats(String encoded) {
        List<SeatKey> seats = new ArrayList<>();
        if (encoded == null || encoded.trim().isEmpty()) {
            return seats;
        }
        for (String token : encoded.split(";")) {
            String[] values = token.trim().split("\\|");
            if (values.length == 3) {
                try {
                    seats.add(new SeatKey(values[0], Integer.parseInt(values[1]),
                            Integer.parseInt(values[2])));
                } catch (IllegalArgumentException ignored) {
                    // Skip malformed legacy seat data.
                }
            }
        }
        return seats;
    }

    private LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private LocalTime toLocalTime(Time value) {
        return value == null ? null : value.toLocalTime();
    }

    /** Raised when a seat has already been taken by another booking. */
    public static final class SeatAlreadyBookedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final transient Booking booking;

        SeatAlreadyBookedException(Booking booking) {
            super("One or more seats in " + booking.getReference() + " have just been booked");
            this.booking = booking;
        }

        public Booking getBooking() {
            return booking;
        }
    }
}
