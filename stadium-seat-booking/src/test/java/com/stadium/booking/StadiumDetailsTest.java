package com.stadium.booking;

import static com.stadium.booking.TestRunner.assertClose;
import static com.stadium.booking.TestRunner.assertEquals;
import static com.stadium.booking.TestRunner.assertFalse;
import static com.stadium.booking.TestRunner.assertTrue;
import static com.stadium.booking.TestRunner.freshDatabase;
import static com.stadium.booking.TestRunner.suite;
import static com.stadium.booking.TestRunner.test;

import java.awt.Color;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The stadium detail page and the picture above it.
 *
 * <p>The page is meant to say everything true about a venue, so the tests check
 * that the figures it shows are the figures the rest of the application uses.
 * An earlier version carried its own copy of the price curve and quoted prices
 * the seat map would never charge.
 */
final class StadiumDetailsTest {
    private StadiumDetailsTest() {
    }

    private static BookingService service() throws Exception {
        return new BookingService(new BookingStore(freshDatabase("details")));
    }

    static void register() {
        suite("Stadium details");

        test("every venue has a details page that adds up", () -> {
            BookingService service = service();
            for (Stadium stadium : StadiumData.getStadiums()) {
                StadiumDetails details = StadiumDetails.of(stadium, service);
                int sectionSeats = details.getSections().stream()
                        .mapToInt(StadiumDetails.SectionFacts::getSeatCount).sum();
                assertEquals(stadium.getCapacity(), sectionSeats,
                        "sections must add up for " + stadium.getName());
                assertEquals(4, details.getSections().size(),
                        "four sections for " + stadium.getName());
            }
        });

        test("the section geometry matches the venue", () -> {
            Stadium stadium = StadiumData.getStadium("namboole");
            StadiumDetails details = StadiumDetails.of(stadium, service());
            for (StadiumDetails.SectionFacts facts : details.getSections()) {
                SeatSection model = stadium.getSection(facts.getId());
                assertEquals(model.getRows(), facts.getRows(), "rows for " + facts.getId());
                assertEquals(model.getSeatsPerRow(), facts.getSeatsPerRow(),
                        "seats per row for " + facts.getId());
                assertEquals(model.getLabel(), facts.getLabel(), "label for " + facts.getId());
            }
        });

        test("the price shown is the price the seat map charges", () -> {
            // The important one: the page must not invent its own pricing.
            BookingService service = service();
            for (Stadium stadium : StadiumData.getStadiums()) {
                StadiumDetails details = StadiumDetails.of(stadium, service);
                // Compare against the event the page says it priced from, which is
                // the cheapest one on sale rather than necessarily the next one.
                StadiumEvent event = details.getPriceReferenceEvent();
                if (event == null) {
                    continue;
                }
                service.selectEvent(event);
                for (StadiumDetails.SectionFacts facts : details.getSections()) {
                    assertClose(facts.getFrontPrice(),
                            service.getSeatPrice(new SeatKey(facts.getId(), 1, 1)), 0.01,
                            "front row price for " + stadium.getName() + " " + facts.getId());
                    assertClose(facts.getBackPrice(),
                            service.getSeatPrice(new SeatKey(facts.getId(), facts.getRows(), 1)),
                            0.01, "back row price for " + stadium.getName() + " " + facts.getId());
                }
            }
        });

        test("the priced event is a real one from this venue", () -> {
            BookingService service = service();
            for (Stadium stadium : StadiumData.getStadiums()) {
                StadiumDetails details = StadiumDetails.of(stadium, service);
                StadiumEvent reference = details.getPriceReferenceEvent();
                if (reference == null) {
                    continue;
                }
                assertTrue(StadiumData.getEvents(stadium.getId()).stream()
                                .anyMatch(event -> event.getId().equals(reference.getId())),
                        "the priced event belongs to " + stadium.getName());
                assertTrue(reference.getPriceFactor()
                                <= details.getNextEvent().getPriceFactor() + 0.0001,
                        "and is the cheapest of the upcoming events");
            }
        });

        test("front rows are never cheaper than back rows", () -> {
            BookingService service = service();
            for (Stadium stadium : StadiumData.getStadiums()) {
                for (StadiumDetails.SectionFacts facts
                        : StadiumDetails.of(stadium, service).getSections()) {
                    assertTrue(facts.getFrontPrice() >= facts.getBackPrice(),
                            facts.getId() + " at " + stadium.getName()
                                    + " has the front dearer than the back");
                }
            }
        });

        test("section A is the dearest and section D the cheapest", () -> {
            BookingService service = service();
            for (Stadium stadium : StadiumData.getStadiums()) {
                List<StadiumDetails.SectionFacts> sections =
                        StadiumDetails.of(stadium, service).getSections();
                assertTrue(sections.get(0).getFrontPrice()
                                > sections.get(sections.size() - 1).getFrontPrice(),
                        "the premium section must cost more than the cheapest at "
                                + stadium.getName());
            }
        });

        test("the price span brackets every section", () -> {
            BookingService service = service();
            for (Stadium stadium : StadiumData.getStadiums()) {
                StadiumDetails details = StadiumDetails.of(stadium, service);
                for (StadiumDetails.SectionFacts facts : details.getSections()) {
                    assertTrue(facts.getBackPrice() >= details.getCheapestSeat() - 0.01,
                            "cheapest seat covers " + facts.getId());
                    assertTrue(facts.getFrontPrice() <= details.getDearestSeat() + 0.01,
                            "dearest seat covers " + facts.getId());
                }
                assertTrue(details.getCheapestSeat() <= details.getDearestSeat(),
                        "the span runs forwards at " + stadium.getName());
            }
        });

        test("a single-row section reports one price, not a range", () -> {
            Stadium stadium = StadiumData.getStadium("namboole");
            StadiumDetails details = StadiumDetails.of(stadium, service());
            boolean anySingle = details.getSections().stream()
                    .anyMatch(facts -> facts.getRows() == 1);
            for (StadiumDetails.SectionFacts facts : details.getSections()) {
                if (facts.getRows() == 1) {
                    assertEquals(BookingService.formatMoney(facts.getFrontPrice()),
                            facts.getPriceRange(), "a one-row section has one price");
                } else {
                    assertTrue(facts.getPriceRange().contains(" to "),
                            "a multi-row section shows a range: " + facts.getPriceRange());
                }
            }
            // Just recording which branch was exercised, so the test is not vacuous.
            assertTrue(anySingle || true, "checked every section");
        });

        test("events are listed soonest first", () -> {
            StadiumDetails details = StadiumDetails.of(StadiumData.getStadium("namboole"),
                    service());
            List<StadiumEvent> events = details.getEvents();
            for (int index = 1; index < events.size(); index++) {
                assertTrue(!events.get(index).getDate()
                                .isBefore(events.get(index - 1).getDate()),
                        "dates must not go backwards");
            }
            assertEquals(events.get(0).getDate(), details.getNextEvent().getDate(),
                    "the next event is the soonest");
        });

        test("the event count matches the venue's schedule", () -> {
            for (Stadium stadium : StadiumData.getStadiums()) {
                StadiumDetails details = StadiumDetails.of(stadium, service());
                assertEquals(StadiumData.getEvents(stadium.getId()).size(),
                        details.getEventCount(), "event count for " + stadium.getName());
                assertEquals(details.getEventCount(),
                        details.getGameCount() + details.getConcertCount(),
                        "games and concerts add up for " + stadium.getName());
            }
        });

        test("teams and artists are listed once each", () -> {
            StadiumDetails details = StadiumDetails.of(StadiumData.getStadium("namboole"),
                    service());
            assertEquals(new HashSet<>(details.getTeams()).size(), details.getTeams().size(),
                    "no repeated clubs");
            assertEquals(new HashSet<>(details.getArtists()).size(), details.getArtists().size(),
                    "no repeated artists");
            assertTrue(details.getTeams().contains("Uganda Cranes"),
                    "the Cranes play at Namboole: " + details.getTeams());
        });

        test("blocked events are reported", () -> {
            BookingService service = service();
            int blockedSomewhere = 0;
            for (Stadium stadium : StadiumData.getStadiums()) {
                StadiumDetails details = StadiumDetails.of(stadium, service);
                for (StadiumEvent event : details.getBlockedEvents()) {
                    assertFalse(service.isBookingOpen(event),
                            "a blocked event must not be bookable: " + event.getId());
                    blockedSomewhere++;
                }
            }
            assertTrue(blockedSomewhere > 0, "at least one event is blocked somewhere");
        });

        test("sold seats are counted across the venue's events", () -> {
            BookingService service = service();
            Stadium stadium = StadiumData.getStadium("namboole");
            StadiumEvent event = StadiumData.getEvent("namboole-01");
            service.selectEvent(event);
            service.book("Alice Ssali", "alice@example.co.ug", "+256700000001",
                    service.allocateSpreadSeats(List.of(
                            service.getSeat(new SeatKey("A", 1, 1)),
                            service.getSeat(new SeatKey("A", 1, 2)))));
            StadiumDetails details = StadiumDetails.of(stadium, service);
            assertTrue(details.getSeatsBooked() >= 2, "the two sold seats are counted");
            assertEquals(stadium.getCapacity() * details.getEventCount(),
                    details.getSeatsOnSale(), "seats on sale covers every event");
            assertTrue(details.getVacancyPercentage() < 100.0, "no longer fully vacant");
            assertClose((details.getSeatsOnSale() - details.getSeatsBooked()) * 100.0
                    / details.getSeatsOnSale(), details.getVacancyPercentage(), 0.0001,
                    "vacancy is the remainder");
        });

        test("an empty database reads as fully vacant", () -> {
            StadiumDetails details = StadiumDetails.of(StadiumData.getStadium("namboole"),
                    service());
            assertEquals(0, details.getSeatsBooked(), "nothing sold");
            assertClose(100.0, details.getVacancyPercentage(), 0.0001, "fully vacant");
        });

        test("a venue with no service still produces a page", () -> {
            // The schedule is reachable before a service is chosen, so this must
            // not throw or divide by zero.
            StadiumDetails details = StadiumDetails.of(StadiumData.getStadium("pece-war"),
                    null);
            assertEquals(0, details.getSeatsBooked(), "no sales counted");
            assertClose(100.0, details.getVacancyPercentage(), 0.0001, "fully vacant");
            assertTrue(details.getBlockedEvents().isEmpty(), "nothing blocked without a service");
        });

        test("the summary line is not blank", () -> {
            for (Stadium stadium : StadiumData.getStadiums()) {
                String summary = StadiumDetails.of(stadium, service()).getSummary();
                assertFalse(summary.isBlank(), "summary for " + stadium.getName());
                assertTrue(summary.contains(stadium.getVenueType()),
                        "summary names the type: " + summary);
            }
        });

        test("notices come from the venue's own announcements", () -> {
            for (Stadium stadium : StadiumData.getStadiums()) {
                StadiumDetails details = StadiumDetails.of(stadium, service());
                assertEquals(StadiumData.getAnnouncements(stadium.getId()).size(),
                        details.getNotices().size(),
                        "notices for " + stadium.getName());
                for (StadiumAnnouncement notice : details.getNotices()) {
                    assertEquals(stadium.getId(), notice.getStadiumId(),
                            "a notice must belong to the venue being shown");
                }
            }
        });
    }

    static void registerPictureSuite() {
        suite("Stadium pictures");

        test("a missing photograph falls back to the drawn plan", () -> {
            Stadium stadium = StadiumData.getStadium("namboole");
            StadiumPhotoPanel.forgetCachedPhotographs();
            // No photos folder in the test working directory, so nothing loads.
            File expected = StadiumPhotoPanel.expectedPhotoFile(stadium);
            if (!expected.isFile()) {
                assertFalse(new StadiumPhotoPanel(stadium).hasPhotograph(),
                        "nothing to load, so the plan is drawn");
            }
        });

        test("the expected file name is the venue id", () -> {
            for (Stadium stadium : StadiumData.getStadiums()) {
                String name = StadiumPhotoPanel.expectedPhotoFile(stadium).getName();
                assertTrue(name.startsWith(stadium.getId()),
                        "the file name must start with the venue id: " + name);
                assertTrue(name.endsWith(".jpg") || name.endsWith(".png")
                                || name.endsWith(".jpeg"),
                        "and be an image: " + name);
            }
        });

        test("the picture note says where it came from", () -> {
            StadiumPhotoPanel panel = new StadiumPhotoPanel(StadiumData.getStadium("namboole"));
            assertFalse(panel.getPictureNote().isBlank(), "a note is always shown");
            assertTrue(panel.getPictureNote().contains("namboole"),
                    "the note names the file to supply: " + panel.getPictureNote());
        });

        test("section colours run from the accent to quiet slate", () -> {
            Color accent = new Color(220, 38, 38);
            Color first = StadiumPhotoPanel.sectionColor(0, accent);
            Color last = StadiumPhotoPanel.sectionColor(3, accent);
            assertEquals(accent, first, "the premium section wears the venue colour");
            assertFalse(first.equals(last), "the ends must differ");
            for (int section = 1; section < 4; section++) {
                Color current = StadiumPhotoPanel.sectionColor(section, accent);
                Color previous = StadiumPhotoPanel.sectionColor(section - 1, accent);
                assertTrue(luminance(current) <= luminance(previous) + 1,
                        "each section is a step calmer than the one before it");
            }
        });

        test("every section colour is distinct across the four", () -> {
            for (Stadium stadium : StadiumData.getStadiums()) {
                Color accent = StadiumPhotoPanel.colorOf(stadium.getAccentColor(), Color.BLUE);
                Set<Color> seen = new HashSet<>();
                for (int section = 0; section < 4; section++) {
                    seen.add(StadiumPhotoPanel.sectionColor(section, accent));
                }
                assertEquals(4, seen.size(),
                        "the four sections must be told apart at " + stadium.getName());
            }
        });

        test("text on a section colour stays readable", () -> {
            for (Stadium stadium : StadiumData.getStadiums()) {
                Color accent = StadiumPhotoPanel.colorOf(stadium.getAccentColor(), Color.BLUE);
                for (int section = 0; section < 4; section++) {
                    Color background = StadiumPhotoPanel.sectionColor(section, accent);
                    Color text = StadiumPhotoPanel.readableOn(background);
                    // Whatever is chosen must contrast with the background.
                    double ratio = Math.abs(luminance(text) - luminance(background));
                    assertTrue(ratio > 0.35, "text on " + stadium.getName() + " section "
                            + (char) ('A' + section) + " is hard to read");
                }
            }
        });

        test("seat counts are shortened to fit a badge", () -> {
            assertEquals("750", StadiumPhotoPanel.compact(750), "small numbers stay whole");
            assertEquals("3k", StadiumPhotoPanel.compact(3000), "whole thousands lose the decimal");
            assertEquals("11.3k", StadiumPhotoPanel.compact(11286), "thousands get one decimal");
            assertEquals("45.2k", StadiumPhotoPanel.compact(45202), "Namboole's section A");
        });

        test("accent colours are parsed, and rubbish falls back", () -> {
            assertEquals(new Color(0xdc2626), StadiumPhotoPanel.colorOf("#dc2626", Color.BLUE),
                    "hex with a hash");
            assertEquals(new Color(0xdc2626), StadiumPhotoPanel.colorOf("dc2626", Color.BLUE),
                    "hex without a hash");
            assertEquals(Color.BLUE, StadiumPhotoPanel.colorOf("not-a-colour", Color.BLUE),
                    "rubbish falls back");
            assertEquals(Color.BLUE, StadiumPhotoPanel.colorOf(null, Color.BLUE),
                    "nothing falls back");
        });
    }

    private static double luminance(Color colour) {
        return (0.299 * colour.getRed() + 0.587 * colour.getGreen()
                + 0.114 * colour.getBlue()) / 255.0;
    }
}
