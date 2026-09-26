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

/**
 * Persistent H2 database for reservations. The database keeps booking,
 * customer, event, seat and status information in queryable rows so the
 * bookings screen can display complete reservation details.
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
                    write(legacy);
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

    public void write(List<Booking> bookings) throws IOException {
        if (jdbcUrl == null) {
            return;
        }
        try {
            initializeSchema();
            try (Connection connection = openConnection()) {
                boolean previousAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate("DELETE FROM bookings");
                    }
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO bookings (reference, stadium_id, event_id, event_name, "
                                    + "customer_name, email, phone, seats, total, created_at, "
                                    + "event_date, event_start_time, status) "
                                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        for (Booking booking : bookings) {
                            insert.setString(1, booking.getReference());
                            insert.setString(2, booking.getStadiumId());
                            insert.setString(3, booking.getEventId());
                            insert.setString(4, booking.getEvent());
                            insert.setString(5, booking.getCustomerName());
                            insert.setString(6, booking.getEmail());
                            insert.setString(7, booking.getPhone());
                            insert.setString(8, encodeSeats(booking.getSeats()));
                            insert.setBigDecimal(9, java.math.BigDecimal.valueOf(booking.getTotal()));
                            insert.setLong(10, booking.getCreatedAt().toEpochMilli());
                            if (booking.getEventDate() == null) {
                                insert.setNull(11, java.sql.Types.DATE);
                            } else {
                                insert.setDate(11, Date.valueOf(booking.getEventDate()));
                            }
                            if (booking.getEventStartTime() == null) {
                                insert.setNull(12, java.sql.Types.TIME);
                            } else {
                                insert.setTime(12, Time.valueOf(booking.getEventStartTime()));
                            }
                            insert.setString(13, booking.getStatus().name());
                            insert.addBatch();
                        }
                        insert.executeBatch();
                    }
                    connection.commit();
                } catch (SQLException | RuntimeException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(previousAutoCommit);
                }
            }
        } catch (SQLException exception) {
            throw new IOException("Could not write booking database", exception);
        }
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

    private LocalDate toLocalDate(java.sql.Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private LocalTime toLocalTime(Time value) {
        return value == null ? null : value.toLocalTime();
    }
}
