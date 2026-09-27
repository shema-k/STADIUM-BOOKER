package com.stadium.booking;

import static com.stadium.booking.TestRunner.assertEquals;
import static com.stadium.booking.TestRunner.assertFalse;
import static com.stadium.booking.TestRunner.assertTrue;
import static com.stadium.booking.TestRunner.suite;
import static com.stadium.booking.TestRunner.test;

/** The interface can run in English, Luganda or Swahili. */
final class LocaleTest {
    private LocaleTest() {
    }

    static void register() {
        suite("Languages");

        test("English is the default", () -> {
            Messages.setLanguage(Messages.Language.ENGLISH);
            assertEquals("Choose your stadium", Messages.get("directory.title"), "directory title");
            assertEquals("Confirm booked seats", Messages.get("booking.confirm"), "confirm button");
        });

        test("Luganda translates the main wording", () -> {
            Messages.setLanguage(Messages.Language.LUGANDA);
            assertEquals("Londa ekizimbe k'ekyamukaliro", Messages.get("directory.title"),
                    "directory title");
            assertEquals("Wekakise ebizitansa", Messages.get("booking.confirm"), "confirm button");
            assertEquals("Wakibwa", Messages.get("legend.booked"), "booked legend");
        });

        test("Swahili translates the main wording", () -> {
            Messages.setLanguage(Messages.Language.SWAHILI);
            assertEquals("Chagua uwanja", Messages.get("directory.title"), "directory title");
            assertEquals("Thibitisha viti", Messages.get("booking.confirm"), "confirm button");
            assertEquals("Vimeuzwa", Messages.get("legend.booked"), "booked legend");
        });

        test("an untranslated key falls back to English", () -> {
            Messages.setLanguage(Messages.Language.SWAHILI);
            // app.tagline is only defined in English so far.
            assertEquals("STADIUM SELECT  /  LIVE EVENT TICKETS", Messages.get("app.tagline"),
                    "falls back to English rather than showing a key");
        });

        test("an unknown key is shown rather than blank", () -> {
            assertEquals("no.such.key", Messages.get("no.such.key"),
                    "an unknown key is visible so the gap is obvious");
        });

        test("placeholders are substituted", () -> {
            Messages.setLanguage(Messages.Language.ENGLISH);
            assertEquals("3 venues available", Messages.get("directory.venuesAvailable", 3),
                    "number substituted");
        });

        test("money stays in shillings whatever the language", () -> {
            Messages.setLanguage(Messages.Language.LUGANDA);
            assertEquals("UGX 15,000", Messages.money(15_000), "shillings are not translated");
        });

        test("every language defines the same core keys", () -> {
            for (String key : new String[]{"directory.title", "booking.confirm", "legend.booked",
                    "bookings.refresh", "common.signIn"}) {
                for (Messages.Language language : Messages.Language.values()) {
                    Messages.setLanguage(language);
                    assertFalse(Messages.get(key).isBlank(),
                            "missing wording for " + key + " in " + language);
                }
            }
            Messages.setLanguage(Messages.Language.ENGLISH);
        });

        test("no wording carries stray characters from another script", () -> {
            // Guards against mojibake slipping into a translation: Luganda and
            // Swahili are Latin-script, so a Han or Cyrillic run is always a bug.
            String[] keys = {"directory.title", "directory.subtitle",
                    "directory.hero.title", "directory.hero.subtitle", "directory.open",
                    "directory.findVenue", "directory.search.hint", "stadium.upcoming",
                    "stadium.search.hint", "stadium.notices", "stadium.specialRequest",
                    "booking.title", "booking.confirm", "booking.clearSeats", "booking.selectSeat",
                    "booking.seatsHeld", "booking.viewBooked", "booking.priceOutline",
                    "booking.priceHint", "booking.feeHint", "booking.limitHint",
                    "booking.priceSubtitle", "booking.emptyPrice", "booking.yourDetails",
                    "booking.threeDetails", "booking.yourSelection", "booking.noSeats",
                    "legend.vacant", "legend.selected", "legend.held", "legend.booked",
                    "legend.closed", "bookings.title", "bookings.refresh", "bookings.export",
                    "bookings.cancelSelected", "bookings.hint", "seats.title", "seats.subtitle",
                    "nav.back", "nav.venues", "nav.bookings", "nav.bookedSeats", "nav.occupancy",
                    "nav.liveSchedules", "common.signIn", "common.pin", "common.pinHint",
                    "common.backToStadiums", "common.backToSchedule", "common.allVenues",
                    "occupancy.title", "occupancy.subtitle", "occupancy.export",
                    "payment.title", "payment.question", "common.totalDue",
                    "seatMap.hint", "seatMap.priceGuide"};
            int checked = 0;
            for (String key : keys) {
                for (Messages.Language language : Messages.Language.values()) {
                    Messages.setLanguage(language);
                    String wording = Messages.get(key);
                    checked++;
                    assertFalse(wording.matches(".*[\\u2E80-\\u9FFF\\u0400-\\u04FF].*"),
                            "stray characters in \"" + key + "\" for " + language
                                    + ": " + wording);
                    assertFalse(wording.contains("?{") || wording.contains("}"),
                            "unsubstituted placeholder in \"" + key + "\": " + wording);
                }
            }
            Messages.setLanguage(Messages.Language.ENGLISH);
            assertEquals(keys.length * Messages.Language.values().length, checked,
                    "every core key in every language was checked");
        });

        test("switching language back to English restores the wording", () -> {
            Messages.setLanguage(Messages.Language.SWAHILI);
            Messages.setLanguage(Messages.Language.ENGLISH);
            assertEquals("Booking history", Messages.get("bookings.title"), "restored");
        });
    }
}
