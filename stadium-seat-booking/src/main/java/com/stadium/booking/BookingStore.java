package com.stadium.booking;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Small file-backed store for reservations. It intentionally uses Java
 * serialization so the project has no third-party dependencies.
 */
public final class BookingStore {
    private final Path file;

    public BookingStore(Path file) {
        this.file = file;
    }

    public List<Booking> read() {
        if (file == null || !Files.exists(file)) {
            return new ArrayList<>();
        }

        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(file))) {
            Object value = input.readObject();
            if (!(value instanceof List<?>)) {
                return new ArrayList<>();
            }

            List<Booking> loaded = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item instanceof Booking) {
                    loaded.add((Booking) item);
                }
            }
            return loaded;
        } catch (IOException | ClassNotFoundException | RuntimeException ignored) {
            // A damaged or old data file should not prevent the GUI from opening.
            return new ArrayList<>();
        }
    }

    public void write(List<Booking> bookings) throws IOException {
        if (file == null) {
            return;
        }

        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path temporary = Files.createTempFile(parent, "stadium-bookings-", ".tmp");
        try {
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(temporary))) {
                output.writeObject(new ArrayList<>(bookings));
            }

            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
