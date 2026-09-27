package com.stadium.booking;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Remembers whether dark mode was switched on.
 *
 * <p>Held in a small file beside the booking database rather than in the database
 * itself, because the choice belongs to the machine and the person using it, not
 * to the venue's records. A missing or unreadable file simply means light mode,
 * which is the safe default.
 */
public final class ThemePreference {
    private static final String FILE_NAME = "stadium-select.properties";
    private static final String KEY = "darkMode";

    private ThemePreference() {
    }

    private static Path file() {
        return Path.of(System.getProperty("user.dir", ".")).resolve(FILE_NAME);
    }

    /** Reads the saved choice and applies it. */
    public static void restore() {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file())) {
            properties.load(input);
        } catch (IOException | RuntimeException ignored) {
            // No saved choice, or one we cannot read. Light mode is the default.
            Theme.setDark(false);
            return;
        }
        Theme.setDark(Boolean.parseBoolean(properties.getProperty(KEY, "false")));
    }

    /** Saves the choice in force. */
    public static void save() {
        Properties properties = new Properties();
        properties.setProperty(KEY, String.valueOf(Theme.isDark()));
        try (OutputStream output = Files.newOutputStream(file())) {
            properties.store(output, "Stadium Select settings");
        } catch (IOException ignored) {
            // Not being able to remember the choice is not worth interrupting for.
        }
    }
}
