package com.stadium.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Demo data for the venue directory and schedules. The dates are generated
 * relative to the current day so the directory always has upcoming events.
 */
public final class StadiumData {
    private static final LocalDate TODAY = LocalDate.now();
    private static final List<Stadium> STADIUMS = createStadiums();
    private static final List<StadiumEvent> EVENTS = createEvents();
    private static final List<StadiumAnnouncement> ANNOUNCEMENTS = createAnnouncements();

    private StadiumData() {
    }

    public static List<Stadium> getStadiums() {
        return STADIUMS;
    }

    public static Stadium getStadium(String id) {
        for (Stadium stadium : STADIUMS) {
            if (stadium.getId().equals(id)) {
                return stadium;
            }
        }
        return null;
    }

    public static List<StadiumEvent> getEvents() {
        return EVENTS;
    }

    public static List<StadiumEvent> getEvents(String stadiumId) {
        return EVENTS.stream()
                .filter(event -> event.getStadiumId().equals(stadiumId))
                .sorted(eventOrder())
                .collect(Collectors.toList());
    }

    public static StadiumEvent getEvent(String id) {
        for (StadiumEvent event : EVENTS) {
            if (event.getId().equals(id)) {
                return event;
            }
        }
        return null;
    }

    public static List<LocalDate> getDates(String stadiumId) {
        Set<LocalDate> dates = new LinkedHashSet<>();
        for (StadiumEvent event : getEvents(stadiumId)) {
            dates.add(event.getDate());
        }
        return new ArrayList<>(dates);
    }

    public static List<StadiumAnnouncement> getAnnouncements() {
        return ANNOUNCEMENTS;
    }

    public static List<StadiumAnnouncement> getAnnouncements(String stadiumId) {
        return ANNOUNCEMENTS.stream()
                .filter(announcement -> announcement.getStadiumId().equals(stadiumId))
                .collect(Collectors.toList());
    }

    private static List<Stadium> createStadiums() {
        List<Stadium> stadiums = new ArrayList<>();
        stadiums.add(new Stadium(
                "grand-arena",
                "Grand Arena",
                "New York",
                "United States",
                "Box-shaped multi-purpose arena",
                "A landmark indoor venue for championship basketball, concerts and live entertainment.",
                "100 West 33rd Street, New York",
                24000,
                "#2563eb",
                StadiumShape.BOX,
                sections("North end", "East side", "South end", "West side", 100, 60,
                        52, 44, 34, 38)));
        stadiums.add(new Stadium(
                "riverside-stadium",
                "Riverside Stadium",
                "Manchester",
                "United Kingdom",
                "Oval football stadium",
                "A dramatic football home where every match is treated as a full stadium experience.",
                "Riverside Way, Manchester",
                32000,
                "#0f766e",
                StadiumShape.OVAL,
                sections("North end", "East side", "South end", "West side", 100, 80,
                        48, 40, 30, 34)));
        stadiums.add(new Stadium(
                "pacific-dome",
                "Pacific Dome",
                "Los Angeles",
                "United States",
                "Circular indoor arena",
                "An intimate modern dome for high-energy basketball, hockey and headline concerts.",
                "700 Figueroa Street, Los Angeles",
                21600,
                "#7c3aed",
                StadiumShape.CIRCULAR,
                sections("North end", "East side", "South end", "West side", 60, 90,
                        58, 48, 36, 42)));
        stadiums.add(new Stadium(
                "metro-dome",
                "Metro Dome",
                "Chicago",
                "United States",
                "Circular multi-purpose dome",
                "A spectacular downtown dome built for championship basketball, hockey and large-scale concerts.",
                "333 West Madison Street, Chicago",
                28000,
                "#0891b2",
                StadiumShape.CIRCULAR,
                sections("North end", "East side", "South end", "West side", 100, 70,
                        50, 42, 32, 36)));
        stadiums.add(new Stadium(
                "harbor-arena",
                "Harbor Arena",
                "Seattle",
                "United States",
                "Oval waterfront arena",
                "A bright waterfront venue with a steep bowl, premium basketball seats and waterfront concert nights.",
                "1200 Harbor Avenue, Seattle",
                22400,
                "#0f766e",
                StadiumShape.OVAL,
                sections("North end", "East side", "South end", "West side", 80, 70,
                        46, 38, 30, 34)));
        stadiums.add(new Stadium(
                "crown-park",
                "Crown Park",
                "Toronto",
                "Canada",
                "Box-shaped football and concerts stadium",
                "A four-sided urban stadium hosting international football, club matches and major live shows.",
                "85 King Street West, Toronto",
                32000,
                "#dc2626",
                StadiumShape.BOX,
                sections("North end", "East side", "South end", "West side", 100, 80,
                        44, 36, 28, 32)));
        stadiums.add(new Stadium(
                "southside-coliseum",
                "Southside Coliseum",
                "Atlanta",
                "United States",
                "Circular entertainment coliseum",
                "A southern entertainment landmark for high-energy basketball, football and touring concerts.",
                "100 Arena Way, Atlanta",
                24800,
                "#ea580c",
                StadiumShape.CIRCULAR,
                sections("North end", "East side", "South end", "West side", 100, 62,
                        52, 43, 33, 37)));
        stadiums.add(new Stadium(
                "desert-field",
                "Desert Field",
                "Phoenix",
                "United States",
                "Oval outdoor sports stadium",
                "A sunlit open-air venue for baseball, football and spectacular desert concerts.",
                "1 East Camelback Road, Phoenix",
                26400,
                "#ca8a04",
                StadiumShape.OVAL,
                sections("North end", "East side", "South end", "West side", 110, 60,
                        40, 34, 27, 30)));
        stadiums.add(new Stadium(
                "nordic-arena",
                "Nordic Arena",
                "Stockholm",
                "Sweden",
                "Circular winter arena",
                "A glass-roofed winter arena built for hockey, international football and Nordic music.",
                "2 Arena Lane, Stockholm",
                21600,
                "#4f46e5",
                StadiumShape.CIRCULAR,
                sections("North end", "East side", "South end", "West side", 60, 90,
                        55, 46, 35, 40)));
        stadiums.add(new Stadium(
                "sakura-stadium",
                "Sakura Stadium",
                "Tokyo",
                "Japan",
                "Box-shaped national stadium",
                "A modern four-sided national venue for football, basketball and spectacular live performances.",
                "1 Sakura Walk, Tokyo",
                36000,
                "#db2777",
                StadiumShape.BOX,
                sections("North end", "East side", "South end", "West side", 120, 75,
                        48, 40, 30, 35)));
        stadiums.add(new Stadium(
                "coastal-arena",
                "Coastal Arena",
                "Miami",
                "United States",
                "Oval coastal sports and concert arena",
                "A bright coastal venue where basketball, baseball, football and music share the same bowl.",
                "50 Ocean Drive, Miami",
                30000,
                "#0284c7",
                StadiumShape.OVAL,
                sections("North end", "East side", "South end", "West side", 100, 75,
                        51, 42, 32, 37)));
        return Collections.unmodifiableList(stadiums);
    }

    private static List<SeatSection> sections(String north,
                                             String east,
                                             String south,
                                             String west,
                                             int rows,
                                             int seatsPerRow,
                                             double northPrice,
                                             double eastPrice,
                                             double southPrice,
                                             double westPrice) {
        List<SeatSection> sections = new ArrayList<>();
        sections.add(new SeatSection("A", north, rows, seatsPerRow, northPrice));
        sections.add(new SeatSection("B", east, rows, seatsPerRow, eastPrice));
        sections.add(new SeatSection("C", south, rows, seatsPerRow, southPrice));
        sections.add(new SeatSection("D", west, rows, seatsPerRow, westPrice));
        return sections;
    }

    private static List<StadiumEvent> createEvents() {
        List<StadiumEvent> events = new ArrayList<>();

        events.add(game("grand-01", "grand-arena", "Basketball",
                "New York Knicks", "Boston Celtics", 1, 19, 30, 18, 30, 1.20,
                "A prime-time rivalry game with both teams in championship form."));
        events.add(concert("grand-02", "grand-arena", "Aurora World Tour",
                "Aurora", 2, 20, 0, 18, 30, 1.00,
                "A cinematic arena concert featuring new music and a full light show."));
        events.add(game("grand-03", "grand-arena", "Baseball",
                "New York Yankees", "Boston Red Sox", 3, 18, 40, 17, 40, 1.10,
                "A traditional rivalry under the lights with post-match city views."));
        events.add(concert("grand-04", "grand-arena", "City Lights Live",
                "The Northern Notes", 4, 20, 0, 18, 0, 1.05,
                "A high-energy evening of anthems, drums and audience sing-alongs."));
        events.add(game("grand-05", "grand-arena", "Basketball",
                "Los Angeles Lakers", "Miami Heat", 6, 19, 0, 17, 30, 1.25,
                "Two historic franchises meet in a showcase basketball event."));
        events.add(concert("grand-06", "grand-arena", "Midnight Sessions",
                "Velvet Avenue", 8, 20, 30, 18, 30, 0.95,
                "A late-night concert experience with immersive stage production."));
        events.add(concert("grand-07", "grand-arena", "Daylight Sessions",
                "Luna Park", 1, 16, 0, 15, 0, 0.90,
                "An early acoustic performance before the evening game."));
        events.add(concert("grand-08", "grand-arena", "Season Finale",
                "The Horizon", 45, 20, 0, 18, 30, 1.10,
                "A future headline concert with a long booking window."));

        events.add(game("river-01", "riverside-stadium", "Football",
                "Manchester City", "Liverpool", 1, 16, 30, 15, 0, 1.25,
                "A top-level football meeting between two title contenders."));
        events.add(game("river-02", "riverside-stadium", "Football",
                "Arsenal", "Chelsea", 2, 20, 0, 18, 0, 1.20,
                "A London derby with attacking football and a loud home crowd."));
        events.add(concert("river-03", "riverside-stadium", "Northern Lights Tour",
                "Elias Stone", 3, 19, 30, 17, 30, 1.00,
                "A stadium-scale concert returning for one special night."));
        events.add(game("river-04", "riverside-stadium", "Football",
                "Manchester United", "Tottenham Hotspur", 5, 17, 30, 15, 30, 1.15,
                "A fast-paced Sunday fixture with a full stadium atmosphere."));
        events.add(game("river-05", "riverside-stadium", "Football",
                "Newcastle United", "Brighton", 7, 20, 0, 18, 0, 1.10,
                "A late-season clash between two ambitious and entertaining sides."));
        events.add(game("river-07", "riverside-stadium", "Football",
                "Everton", "Fulham", 1, 14, 0, 13, 0, 1.00,
                "An afternoon fixture that opens the stadium's football weekend."));
        events.add(concert("river-06", "riverside-stadium", "Summer Sound",
                "Harbour Lights", 9, 19, 0, 17, 0, 1.00,
                "A relaxed summer evening of pop, soul and stadium anthems."));

        events.add(game("pacific-01", "pacific-dome", "Basketball",
                "Los Angeles Lakers", "Golden State Warriors", 1, 19, 0, 17, 30, 1.30,
                "A marquee basketball matchup in the heart of Los Angeles."));
        events.add(concert("pacific-02", "pacific-dome", "The Sound of Soul",
                "Maya Rivers", 2, 20, 0, 18, 0, 1.10,
                "A soulful live show with a carefully designed intimate atmosphere."));
        events.add(game("pacific-03", "pacific-dome", "Hockey",
                "Los Angeles Kings", "Vegas Golden Knights", 3, 19, 30, 18, 0, 1.15,
                "A high-tempo hockey night with rivalry and playoff-level energy."));
        events.add(game("pacific-04", "pacific-dome", "Basketball",
                "Phoenix Suns", "Dallas Mavericks", 5, 19, 0, 17, 30, 1.20,
                "A big-time Western Conference basketball showcase."));
        events.add(concert("pacific-05", "pacific-dome", "Velvet Underground",
                "Nova Lane", 6, 20, 0, 18, 0, 1.05,
                "A modern rock performance with a dramatic light and stage design."));
        events.add(game("pacific-06", "pacific-dome", "Hockey",
                "Seattle Kraken", "Vegas Golden Knights", 8, 18, 0, 16, 30, 1.10,
                "A memorable evening of speed, skill and physical hockey."));
        events.add(concert("pacific-07", "pacific-dome", "Acoustic Sunset",
                "Nia Cole", 1, 16, 30, 15, 30, 0.90,
                "An intimate acoustic set in the dome before the evening programme."));

        events.add(game("metro-01", "metro-dome", "Basketball",
                "Chicago Bulls", "Detroit Pistons", 1, 19, 0, 17, 30, 1.15,
                "A downtown basketball rivalry with a loud national broadcast atmosphere."));
        events.add(concert("metro-02", "metro-dome", "Neon Horizon",
                "Neon Horizon", 2, 20, 0, 18, 30, 1.00,
                "A bright electronic concert with a full-stage light and laser production."));
        events.add(game("metro-03", "metro-dome", "Hockey",
                "Chicago Blackhawks", "Colorado Avalanche", 4, 18, 30, 17, 0, 1.10,
                "A high-tempo hockey matchup beneath the dome lights."));
        events.add(concert("metro-04", "metro-dome", "Summer Sessions",
                "The Echoes", 7, 19, 30, 18, 0, 1.00,
                "A summery live set featuring fan favourites and new material."));

        events.add(game("harbor-01", "harbor-arena", "Basketball",
                "Seattle SuperSonics", "Portland Trail Blazers", 1, 19, 0, 17, 30, 1.20,
                "A waterfront basketball rivalry with city pride on the line."));
        events.add(concert("harbor-02", "harbor-arena", "Pacific Night",
                "Maya Rivers", 3, 20, 0, 18, 30, 1.05,
                "A polished live performance overlooking the waterfront."));
        events.add(game("harbor-03", "harbor-arena", "Hockey",
                "Seattle Kraken", "Vancouver Canucks", 5, 18, 0, 16, 30, 1.10,
                "A regional hockey duel with a fast and physical style."));
        events.add(concert("harbor-04", "harbor-arena", "Soundwave",
                "Northstar", 8, 20, 0, 18, 30, 0.95,
                "A large-scale concert built around guitars, drums and cinematic visuals."));

        events.add(game("crown-01", "crown-park", "Football",
                "Toronto FC", "Montreal Impact", 1, 16, 0, 14, 30, 1.15,
                "A Canadian football rivalry with a strong international following."));
        events.add(concert("crown-02", "crown-park", "Crown Summer",
                "Ari Lane", 2, 20, 0, 18, 30, 1.00,
                "A bright summer concert featuring pop anthems and a full band."));
        events.add(game("crown-03", "crown-park", "Football",
                "Canada", "United States", 6, 19, 0, 17, 0, 1.25,
                "A national-team fixture with a highly anticipated atmosphere."));
        events.add(concert("crown-04", "crown-park", "Northern Lights",
                "Celine Hart", 10, 20, 0, 18, 30, 1.05,
                "A sweeping arena show with orchestral and electronic elements."));

        events.add(game("south-01", "southside-coliseum", "Basketball",
                "Atlanta Hawks", "Charlotte Hornets", 1, 19, 0, 17, 30, 1.10,
                "A fast-paced basketball game in a classic southern basketball home."));
        events.add(concert("south-02", "southside-coliseum", "Southern Lights",
                "Lena Brooks", 2, 20, 0, 18, 30, 1.00,
                "A soulful concert with a warm vocal performance and full band."));
        events.add(game("south-03", "southside-coliseum", "American football",
                "Atlanta Falcons", "New Orleans Saints", 5, 16, 0, 14, 30, 1.20,
                "A high-energy football game with classic conference rivalries."));
        events.add(concert("south-04", "southside-coliseum", "Rhythm & Blues Night",
                "The Blue Notes", 9, 20, 0, 18, 30, 0.95,
                "A late-night blues and soul celebration for a full coliseum."));

        events.add(game("desert-01", "desert-field", "Baseball",
                "Arizona Diamondbacks", "Colorado Rockies", 1, 18, 30, 17, 0, 1.10,
                "A desert baseball matchup with a dramatic open-air setting."));
        events.add(game("desert-02", "desert-field", "Football",
                "Phoenix Rising", "LA Galaxy", 3, 19, 0, 17, 0, 1.15,
                "A colorful football fixture under the lights."));
        events.add(concert("desert-03", "desert-field", "Desert Bloom",
                "Canyon Sun", 4, 20, 0, 18, 30, 1.00,
                "A sunset concert experience with desert-inspired visuals."));
        events.add(game("desert-04", "desert-field", "Baseball",
                "Arizona Diamondbacks", "San Diego Padres", 8, 18, 30, 17, 0, 1.05,
                "A rematch under the lights with a competitive late-innings finish expected."));

        events.add(game("nordic-01", "nordic-arena", "Hockey",
                "Sweden", "Finland", 2, 18, 0, 16, 30, 1.20,
                "A high-skill northern hockey rivalry under a glass roof."));
        events.add(concert("nordic-02", "nordic-arena", "Arctic Light",
                "Vega", 3, 20, 0, 18, 30, 1.05,
                "A dramatic Nordic concert with atmospheric lighting and live strings."));
        events.add(game("nordic-03", "nordic-arena", "Football",
                "Sweden", "Norway", 6, 19, 0, 17, 0, 1.10,
                "A national football celebration with a passionate home crowd."));
        events.add(concert("nordic-04", "nordic-arena", "Arctic Symphony",
                "The Nordic Line", 11, 20, 0, 18, 30, 0.95,
                "A symphonic pop performance designed for the circular arena."));

        events.add(game("sakura-01", "sakura-stadium", "Football",
                "Tokyo FC", "Osaka FC", 1, 17, 0, 15, 0, 1.20,
                "A major Japanese football meeting with an electric national atmosphere."));
        events.add(concert("sakura-02", "sakura-stadium", "Sakura Night",
                "Hikaru", 2, 20, 0, 18, 30, 1.05,
                "A modern stadium concert with a strong visual and musical production."));
        events.add(game("sakura-03", "sakura-stadium", "Basketball",
                "Tokyo Rockets", "Nagoya Dolphins", 5, 19, 0, 17, 30, 1.10,
                "A high-level basketball matchup with a passionate Tokyo crowd."));
        events.add(concert("sakura-04", "sakura-stadium", "Future Horizon",
                "Aoi", 9, 20, 0, 18, 30, 1.00,
                "A future-facing live show combining pop, dance and cinematic visuals."));

        events.add(game("coastal-01", "coastal-arena", "Basketball",
                "Miami Heat", "Boston Celtics", 1, 19, 0, 17, 30, 1.20,
                "A major basketball matchup in a bright coastal setting."));
        events.add(game("coastal-02", "coastal-arena", "Baseball",
                "Miami Marlins", "Atlanta Braves", 2, 18, 30, 17, 0, 1.10,
                "A baseball evening with a strong home crowd and a quick start."));
        events.add(concert("coastal-03", "coastal-arena", "Coastal Vibes",
                "Solara", 4, 20, 0, 18, 30, 1.00,
                "A breezy coastal concert with bright lights and a live horn section."));
        events.add(game("coastal-04", "coastal-arena", "Football",
                "Inter Miami", "New York City", 7, 19, 30, 17, 30, 1.15,
                "A high-profile football night with an international atmosphere."));

        return Collections.unmodifiableList(events);
    }

    private static List<StadiumAnnouncement> createAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        List<StadiumAnnouncement> announcements = new ArrayList<>();
        announcements.add(new StadiumAnnouncement(
                "grand-notice-01", "grand-arena", "grand-01", AnnouncementType.NOTICE,
                "Enhanced arrival information",
                "Use the north entrance for accessible seating and arrive early for security screening.",
                now.minusHours(2), now.plusDays(30)));
        announcements.add(new StadiumAnnouncement(
                "grand-cancel-01", "grand-arena", "grand-03", AnnouncementType.CANCELLATION,
                "Game cancelled — weather monitoring",
                "The Yankees vs Red Sox game is cancelled while the venue team monitors severe weather.",
                now.minusHours(1), now.plusDays(14)));
        announcements.add(new StadiumAnnouncement(
                "grand-schedule-01", "grand-arena", "", AnnouncementType.SCHEDULE_CHANGE,
                "Concert doors may open earlier",
                "For selected concerts, doors may open up to 30 minutes earlier than the published time.",
                now.minusDays(1), now.plusDays(45)));
        announcements.add(new StadiumAnnouncement(
                "river-emergency-01", "riverside-stadium", "river-01", AnnouncementType.EMERGENCY,
                "Emergency entrance change",
                "The east turnstile is temporarily closed. Please follow stadium staff to the west entrance.",
                now.minusMinutes(30), now.plusDays(2)));
        announcements.add(new StadiumAnnouncement(
                "pacific-notice-01", "pacific-dome", "pacific-01", AnnouncementType.NOTICE,
                "Quiet arrival support available",
                "Guests needing a quieter arrival route can contact the venue team before the event.",
                now.minusHours(4), now.plusDays(30)));
        return Collections.unmodifiableList(announcements);
    }

    private static StadiumEvent game(String id,
                                     String stadiumId,
                                     String sport,
                                     String teamOne,
                                     String teamTwo,
                                     int dayOffset,
                                     int hour,
                                     int minute,
                                     int doorsHour,
                                     int doorsMinute,
                                     double priceFactor,
                                     String description) {
        String title = teamOne + " vs " + teamTwo;
        return new StadiumEvent(id, stadiumId, EventType.GAME, title, sport, teamOne, teamTwo,
                null, description, TODAY.plusDays(dayOffset), LocalTime.of(hour, minute),
                LocalTime.of(doorsHour, doorsMinute), priceFactor);
    }

    private static StadiumEvent concert(String id,
                                        String stadiumId,
                                        String title,
                                        String artist,
                                        int dayOffset,
                                        int hour,
                                        int minute,
                                        int doorsHour,
                                        int doorsMinute,
                                        double priceFactor,
                                        String description) {
        return new StadiumEvent(id, stadiumId, EventType.CONCERT, title, null, null, null,
                artist, description, TODAY.plusDays(dayOffset), LocalTime.of(hour, minute),
                LocalTime.of(doorsHour, doorsMinute), priceFactor);
    }

    private static Comparator<StadiumEvent> eventOrder() {
        return Comparator.comparing(StadiumEvent::getDate)
                .thenComparing(StadiumEvent::getStartTime)
                .thenComparing(StadiumEvent::getId);
    }
}
