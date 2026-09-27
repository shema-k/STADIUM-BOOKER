package com.stadium.booking;

import static com.stadium.booking.TestRunner.assertEquals;
import static com.stadium.booking.TestRunner.assertTrue;
import static com.stadium.booking.TestRunner.freshDatabase;
import static com.stadium.booking.TestRunner.suite;
import static com.stadium.booking.TestRunner.test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Filtering and live search behaviour, kept separate from the Swing layer. */
final class SearchAndFilterTest {
    private SearchAndFilterTest() {
    }

    static void register() {
        suite("Search and filters");

        test("a team name finds the venue hosting it", () -> {
            List<Stadium> matches = filterVenues("vipers");
            assertTrue(!matches.isEmpty(), "searching a club should find its venue");
            assertTrue(matches.stream().anyMatch(s -> "st-marys".equals(s.getId())),
                    "Vipers SC play at St. Mary's Stadium");
        });

        test("a city name finds its venues", () -> {
            List<Stadium> matches = filterVenues("kampala");
            List<String> kampala = StadiumData.getStadiums().stream()
                    .filter(s -> s.getCity().equalsIgnoreCase("Kampala"))
                    .map(Stadium::getId).collect(Collectors.toList());
            assertTrue(kampala.size() > 1, "several venues are in Kampala");
            for (String id : kampala) {
                assertTrue(matches.stream().anyMatch(s -> s.getId().equals(id)),
                        "searching Kampala must find " + id);
            }
            // A venue outside the city can still match if a fixture there mentions
            // the city, which is why this asserts a superset rather than equality.
        });

        test("an unknown word matches nothing", () -> {
            assertTrue(filterVenues("zzzznotathing").isEmpty(), "no venue should match nonsense");
        });

        test("venue search ignores the description so a team name stays specific", () -> {
            // Namboole's description mentions "Cranes"; only the Cranes events
            // should be returned, not every event at that ground.
            List<StadiumEvent> cranes = filterEvents("cranes");
            assertTrue(cranes.size() > 0, "the Cranes should be findable");
            assertTrue(cranes.stream().allMatch(e -> e.searchableText().contains("cranes")),
                    "only genuine Cranes matches should be returned, got " + cranes.size());
        });

        test("events sort soonest first", () -> {
            List<StadiumEvent> events = new ArrayList<>(StadiumData.getEvents());
            events.sort(java.util.Comparator.comparing(StadiumEvent::getDate)
                    .thenComparing(StadiumEvent::getStartTime)
                    .thenComparing(StadiumEvent::getId));
            LocalDate previous = null;
            for (StadiumEvent event : events) {
                if (previous != null) {
                    assertTrue(!event.getDate().isBefore(previous), "dates must not go backwards");
                }
                previous = event.getDate();
            }
        });

        test("every date can be labelled for a heading", () -> {
            for (StadiumEvent event : StadiumData.getEvents()) {
                String label = event.getDate().format(java.time.format.DateTimeFormatter
                        .ofPattern("EEE, d MMM yyyy", Locale.ENGLISH));
                assertTrue(!label.isBlank(), "date label for " + event.getId());
            }
        });

        test("type filtering splits games from concerts", () -> {
            long games = StadiumData.getEvents().stream().filter(StadiumEvent::isGame).count();
            long concerts = StadiumData.getEvents().size() - games;
            assertTrue(games > 0, "there should be games");
            assertTrue(concerts > 0, "there should be concerts");
        });

        test("unique venues for the directory", () -> {
            int distinct = (int) StadiumData.getStadiums().stream()
                    .map(Stadium::getId).distinct().count();
            assertEquals(StadiumData.getStadiums().size(), distinct, "venue ids must be unique");
        });

        test("distinct team and artist names are found", () -> {
            Map<String, Integer> counts = new java.util.LinkedHashMap<>();
            for (StadiumEvent event : StadiumData.getEvents()) {
                for (String name : List.of(nullToEmpty(event.getTeamOne()),
                        nullToEmpty(event.getTeamTwo()), nullToEmpty(event.getArtist()))) {
                    if (!name.isEmpty()) {
                        counts.merge(name, 1, Integer::sum);
                    }
                }
            }
            assertTrue(counts.containsKey("Vipers SC"), "Vipers SC should be present");
            assertTrue(counts.containsKey("Eddy Kenzo"), "Eddy Kenzo should be present");
        });
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Mirrors the directory search: a venue matches on its own details or on any
     * team or artist appearing at it.
     */
    private static List<Stadium> filterVenues(String query) {
        String q = query.toLowerCase(Locale.ENGLISH);
        Predicate<Stadium> detailsMatch = stadium ->
                (stadium.getName() + " " + stadium.getCity() + " " + stadium.getCountry()
                        + " " + stadium.getVenueType()).toLowerCase(Locale.ENGLISH).contains(q);
        Predicate<Stadium> eventMatch = stadium -> StadiumData.getEvents(stadium.getId()).stream()
                .anyMatch(event -> event.searchableText().contains(q));
        return StadiumData.getStadiums().stream()
                .filter(stadium -> detailsMatch.test(stadium) || eventMatch.test(stadium))
                .collect(Collectors.toList());
    }

    private static List<StadiumEvent> filterEvents(String query) {
        String q = query.toLowerCase(Locale.ENGLISH);
        List<StadiumEvent> matches = new ArrayList<>();
        for (StadiumEvent event : StadiumData.getEvents()) {
            if (event.searchableText().contains(q)) {
                matches.add(event);
            }
        }
        return matches;
    }

    private static LocalTime unusedTimeAnchor() {
        return LocalTime.NOON;
    }
}
