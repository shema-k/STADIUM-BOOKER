package com.stadium.booking;

import static com.stadium.booking.TestRunner.assertEquals;
import static com.stadium.booking.TestRunner.assertFalse;
import static com.stadium.booking.TestRunner.assertTrue;
import static com.stadium.booking.TestRunner.suite;
import static com.stadium.booking.TestRunner.test;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Dark mode.
 *
 * <p>A dark theme is easy to add and easy to get wrong: colours that were fine on
 * white stop being readable, and seat states that were obvious in daylight become
 * indistinguishable at night. These tests check the two things that actually
 * break in practice, readability and telling the seat states apart.
 */
final class ThemeTest {
    private ThemeTest() {
    }

    /** Relative luminance, the WCAG definition, so contrast can be judged properly. */
    private static double luminance(Color colour) {
        double red = channel(colour.getRed());
        double green = channel(colour.getGreen());
        double blue = channel(colour.getBlue());
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
    }

    private static double channel(int value) {
        double scaled = value / 255.0;
        return scaled <= 0.03928 ? scaled / 12.92
                : Math.pow((scaled + 0.055) / 1.055, 2.4);
    }

    private static double contrast(Color one, Color two) {
        double first = luminance(one);
        double second = luminance(two);
        double lighter = Math.max(first, second);
        double darker = Math.min(first, second);
        return (lighter + 0.05) / (darker + 0.05);
    }

    static void register() {
        suite("Dark mode");

        test("light mode is the default and dark is a switch away", () -> {
            Theme.resetForTesting();
            assertFalse(Theme.isDark(), "light by default");
            assertTrue(Theme.setDark(true), "the switch reports a change");
            assertTrue(Theme.isDark(), "now dark");
            Theme.resetForTesting();
        });

        test("switching to the theme already in force changes nothing", () -> {
            Theme.resetForTesting();
            assertFalse(Theme.setDark(false), "no change when already light");
            Theme.setDark(true);
            assertFalse(Theme.setDark(true), "no change when already dark");
            Theme.resetForTesting();
        });

        test("everything listening is told when the theme changes", () -> {
            Theme.resetForTesting();
            List<Boolean> heard = new ArrayList<>();
            Theme.onChange(palette -> heard.add(palette.isDark()));
            try {
                Theme.setDark(true);
                Theme.setDark(false);
            } finally {
                Theme.resetForTesting();
            }
            assertEquals(2, heard.size(), "one callback per real change");
            assertTrue(heard.get(0), "the first was the dark palette");
            assertFalse(heard.get(1), "the second was the light palette");
        });

        test("the dark page is actually dark and the light one is not", () -> {
            Theme.resetForTesting();
            assertTrue(luminance(Theme.current().page()) > 0.5, "light page is bright");
            Theme.setDark(true);
            assertTrue(luminance(Theme.current().page()) < 0.1, "dark page is near black");
            Theme.resetForTesting();
        });

        test("body text is readable on cards in both themes", () -> {
            for (boolean dark : new boolean[]{false, true}) {
                Theme.resetForTesting();
                Theme.setDark(dark);
                Theme.Palette palette = Theme.current();
                double onCard = contrast(palette.text(), palette.card());
                double onPage = contrast(palette.text(), palette.page());
                double mutedOnCard = contrast(palette.muted(), palette.card());
                assertTrue(onCard >= 4.5, "body text on a card in "
                        + (dark ? "dark" : "light") + " mode: " + onCard);
                assertTrue(onPage >= 4.5, "body text on the page in "
                        + (dark ? "dark" : "light") + " mode: " + onPage);
                assertTrue(mutedOnCard >= 3.0, "secondary text in "
                        + (dark ? "dark" : "light") + " mode: " + mutedOnCard);
            }
            Theme.resetForTesting();
        });

        test("header text is readable on the header in both themes", () -> {
            for (boolean dark : new boolean[]{false, true}) {
                Theme.resetForTesting();
                Theme.setDark(dark);
                Theme.Palette palette = Theme.current();
                assertTrue(contrast(palette.headerText(), palette.header()) >= 4.5,
                        "header text in " + (dark ? "dark" : "light") + " mode");
                assertTrue(contrast(palette.headerMuted(), palette.header()) >= 3.0,
                        "header subtitle in " + (dark ? "dark" : "light") + " mode");
            }
            Theme.resetForTesting();
        });

        test("typed text is readable in the entry fields in both themes", () -> {
            for (boolean dark : new boolean[]{false, true}) {
                Theme.resetForTesting();
                Theme.setDark(dark);
                Theme.Palette palette = Theme.current();
                assertTrue(contrast(palette.text(), palette.field()) >= 4.5,
                        "what you type in " + (dark ? "dark" : "light") + " mode");
            }
            Theme.resetForTesting();
        });

        test("the four seat states stay apart in both themes", () -> {
            for (boolean dark : new boolean[]{false, true}) {
                Theme.resetForTesting();
                Theme.setDark(dark);
                Theme.Palette palette = Theme.current();
                List<Color> seats = List.of(palette.seatVacant(), palette.seatSelected(),
                        palette.seatBooked(), palette.seatClosed(), palette.seatHeld());
                for (int first = 0; first < seats.size(); first++) {
                    for (int second = first + 1; second < seats.size(); second++) {
                        assertFalse(seats.get(first).equals(seats.get(second)),
                                "two seat states are the same colour in "
                                        + (dark ? "dark" : "light") + " mode");
                    }
                }
                Theme.resetForTesting();
            }
        });

        test("a seat's own number stays readable on its own fill", () -> {
            for (boolean dark : new boolean[]{false, true}) {
                Theme.resetForTesting();
                Theme.setDark(dark);
                Theme.Palette palette = Theme.current();
                for (Color seat : List.of(palette.seatVacant(), palette.seatSelected(),
                        palette.seatBooked(), palette.seatClosed(), palette.seatHeld())) {
                    assertTrue(contrast(palette.seatText(seat), seat) >= 3.0,
                            "a seat number is unreadable on " + hex(seat) + " in "
                                    + (dark ? "dark" : "light") + " mode");
                }
            }
            Theme.resetForTesting();
        });

        test("a button's label is readable on the accent in both themes", () -> {
            for (boolean dark : new boolean[]{false, true}) {
                Theme.resetForTesting();
                Theme.setDark(dark);
                Theme.Palette palette = Theme.current();
                // The dark theme's accent is a bright blue, so its label has to be
                // dark. White on it was unreadable until this was caught.
                assertTrue(contrast(palette.onAccent(), palette.accent()) >= 4.5,
                        "the button label on the accent in "
                                + (dark ? "dark" : "light") + " mode");
            }
            Theme.resetForTesting();
        });

        test("selected rows are readable in the tables in both themes", () -> {
            for (boolean dark : new boolean[]{false, true}) {
                Theme.resetForTesting();
                Theme.setDark(dark);
                Theme.Palette palette = Theme.current();
                assertTrue(contrast(palette.onAccent(), palette.accent()) >= 4.5,
                        "a selected table row in " + (dark ? "dark" : "light") + " mode");
            }
            Theme.resetForTesting();
        });

        test("success and danger read as themselves against a card", () -> {
            for (boolean dark : new boolean[]{false, true}) {
                Theme.resetForTesting();
                Theme.setDark(dark);
                Theme.Palette palette = Theme.current();
                assertTrue(contrast(palette.success(), palette.card()) >= 3.0,
                        "green text in " + (dark ? "dark" : "light") + " mode");
                assertTrue(contrast(palette.danger(), palette.card()) >= 3.0,
                        "red text in " + (dark ? "dark" : "light") + " mode");
                assertTrue(contrast(palette.warning(), palette.card()) >= 3.0,
                        "amber text in " + (dark ? "dark" : "light") + " mode");
            }
            Theme.resetForTesting();
        });

        test("a card is told apart from the page behind it", () -> {
            for (boolean dark : new boolean[]{false, true}) {
                Theme.resetForTesting();
                Theme.setDark(dark);
                Theme.Palette palette = Theme.current();
                assertTrue(contrast(palette.card(), palette.page()) >= 1.05,
                        "cards vanish into the page in "
                                + (dark ? "dark" : "light") + " mode");
            }
            Theme.resetForTesting();
        });

        test("the drawn stadium plan has grass in both themes", () -> {
            for (boolean dark : new boolean[]{false, true}) {
                Theme.resetForTesting();
                Theme.setDark(dark);
                Theme.Palette palette = Theme.current();
                // Grass should sit clearly green, not grey, in either theme.
                assertTrue(palette.grassTop().getGreen() > palette.grassTop().getRed()
                                && palette.grassTop().getGreen() > palette.grassTop().getBlue(),
                        "the pitch is not green in " + (dark ? "dark" : "light") + " mode");
            }
            Theme.resetForTesting();
        });

        test("the chosen theme is remembered between runs", () -> {
            Theme.resetForTesting();
            Path file = Path.of("stadium-select.properties");
            try {
                Theme.setDark(true);
                ThemePreference.save();
                assertTrue(Files.exists(file), "the choice is written down");
                Theme.resetForTesting();
                assertFalse(Theme.isDark(), "reset for the next run of the test");
                ThemePreference.restore();
                assertTrue(Theme.isDark(), "and read back as dark");
            } finally {
                Theme.setDark(false);
                try {
                    Files.deleteIfExists(file);
                } catch (java.io.IOException ignored) {
                    // Leaving the file behind only means the next run starts dark.
                }
                Theme.resetForTesting();
            }
        });

        test("a missing settings file simply means light mode", () -> {
            Theme.resetForTesting();
            Path file = Path.of("stadium-select.properties");
            try {
                Files.deleteIfExists(file);
                ThemePreference.restore();
                assertFalse(Theme.isDark(), "no file, so light mode");
            } catch (java.io.IOException exception) {
                throw new AssertionError("could not clear the settings file", exception);
            } finally {
                Theme.resetForTesting();
            }
        });
    }

    private static String hex(Color colour) {
        return String.format("#%02x%02x%02x",
                colour.getRed(), colour.getGreen(), colour.getBlue());
    }
}
