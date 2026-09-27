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
    /**
     * Labels for the four independent seating sections (A to D). Section A is the
     * premium stand, and prices fall through to section D at the back. Declared
     * before the lists below because static initialisers run in declaration order.
     */
    private static final String[] SECTION_LABELS = {
            "VIP Box", "Main Stand", "Terrace", "Kampala End"};

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
                "namboole",
                "Mandela National Stadium (Namboole)",
                "Kampala",
                "Uganda",
                "National multi-purpose stadium",
                "Uganda's flagship arena on Namboole Hill, opened in 1997 with Chinese government "
                        + "funding. Home of the Cranes national team and a regular host of Uganda "
                        + "Premier League derbies, CHAN matches and major concerts.",
                "Namboole Hill, Kira Municipality, Wakiso District",
                45202,
                "#dc2626",
                StadiumShape.OVAL,
                sections(114, 99, 250000, 127, 89, 190000, 127, 89, 140000, 130, 87, 95000)));
        stadiums.add(new Stadium(
                "hoima-city",
                "Hoima City Stadium",
                "Hoima",
                "Uganda",
                "Municipal football stadium",
                "A modern civic stadium in the Bwinege capital of western Uganda, opened for the "
                        + "FUFA Drum and the home ground of Kitara FC.",
                "Hoima Town, Hoima District, Western Uganda",
                20000,
                "#ca8a04",
                StadiumShape.BOX,
                sections(100, 50, 90000, 100, 50, 70000, 100, 50, 55000, 100, 50, 40000)));
        stadiums.add(new Stadium(
                "hamz-stadium",
                "Hamz Stadium (Nakivubo)",
                "Kampala",
                "Uganda",
                "Historic urban football ground",
                "Kampala's oldest surviving stadium, first used in 1926 and rebuilt as Hamz Stadium. "
                        + "Its tight brick bowl sits in the middle of the city and is the traditional "
                        + "home of Express FC.",
                "Nakivubo, Kampala, Central Region",
                15000,
                "#b45309",
                StadiumShape.BOX,
                sections(75, 50, 80000, 75, 50, 60000, 75, 50, 45000, 75, 50, 35000)));
        stadiums.add(new Stadium(
                "st-marys",
                "St. Mary's Stadium, Kitende",
                "Entebbe",
                "Uganda",
                "Club football stadium",
                "The compact modern home of Vipers SC on the Kampala-Entebbe road, known for its "
                        + "loud atmosphere and a steep, close-to-the-pitch bowl.",
                "Kitende, Wakiso District, Entebbe Road",
                15000,
                "#15803d",
                StadiumShape.OVAL,
                sections(75, 50, 85000, 75, 50, 65000, 75, 50, 50000, 75, 50, 38000)));
        stadiums.add(new Stadium(
                "omondi",
                "MTN Omondi Stadium, Lugogo",
                "Kampala",
                "Uganda",
                "City football stadium",
                "Lugogo's municipal ground, opened in 1957 and home of KCCA FC. A compact bowl in the "
                        + "middle of Kampala's central business district.",
                "Lugogo, Kampala, Central Region",
                10000,
                "#1d4ed8",
                StadiumShape.OVAL,
                sections(50, 50, 60000, 50, 50, 48000, 50, 50, 38000, 50, 50, 28000)));
        stadiums.add(new Stadium(
                "mutesa-ii",
                "Mutesa II Stadium, Wankulukuku",
                "Kampala",
                "Uganda",
                "Kampala city stadium",
                "A multi-purpose city stadium in Wankulukuku, used for football, athletics and "
                        + "national team fixtures in the run-up to international matches.",
                "Wankulukuku, Kampala, Central Region",
                8000,
                "#0f766e",
                StadiumShape.CIRCULAR,
                sections(40, 50, 55000, 40, 50, 42000, 40, 50, 32000, 40, 50, 25000)));
        stadiums.add(new Stadium(
                "kadiba",
                "FUFA Kadiba Stadium",
                "Kampala",
                "Uganda",
                "Federation club ground",
                "A modern four-sided ground in Kadiba developed by the federation, used for Uganda "
                        + "Premier League fixtures and the home matches of SC Villa.",
                "Kadiba, Kawempe, Kampala",
                7000,
                "#7c3aed",
                StadiumShape.BOX,
                sections(35, 50, 50000, 35, 50, 40000, 35, 50, 32000, 35, 50, 24000)));
        stadiums.add(new Stadium(
                "bunamwaya",
                "Bunamwaya Stadium",
                "Wakiso Town",
                "Uganda",
                "Community club ground",
                "A small community ground in Bunamwaya that grew out of a village football pitch and "
                        + "is now used for lower division and academy fixtures.",
                "Bunamwaya, Wakiso District",
                5000,
                "#a16207",
                StadiumShape.BOX,
                sections(25, 50, 35000, 25, 50, 28000, 25, 50, 22000, 25, 50, 16000)));
        stadiums.add(new Stadium(
                "bugembe",
                "Kyabazinga Stadium, Bugembe",
                "Jinja",
                "Uganda",
                "Regional multi-purpose stadium",
                "The main football venue of the Busoga region, a perennial host of FUFA Cup ties and "
                        + "Uganda Premier League fixtures in Jinja.",
                "Bugembe, Jinja, Eastern Uganda",
                12000,
                "#0e7490",
                StadiumShape.OVAL,
                sections(60, 50, 65000, 60, 50, 50000, 60, 50, 40000, 60, 50, 30000)));
        stadiums.add(new Stadium(
                "mbale-municipal",
                "Mbale Municipal Stadium",
                "Mbale",
                "Uganda",
                "Municipal football ground",
                "The civic stadium of Mbale in the eastern highlands, used by FUFA club fixtures and "
                        + "regional cup matches.",
                "Mbale, Eastern Uganda",
                5000,
                "#b91c1c",
                StadiumShape.BOX,
                sections(25, 50, 40000, 25, 50, 32000, 25, 50, 25000, 25, 50, 18000)));
        stadiums.add(new Stadium(
                "pece-war",
                "Pece War Memorial Stadium",
                "Gulu",
                "Uganda",
                "War memorial stadium",
                "Northern Uganda's oldest major sports ground, built by the British in 1959 as a "
                        + "Second World War memorial and long the home of Gulu United FC.",
                "Pece, Gulu, Northern Uganda",
                3000,
                "#334155",
                StadiumShape.OVAL,
                sections(15, 50, 30000, 15, 50, 24000, 15, 50, 19000, 15, 50, 14000)));
        return Collections.unmodifiableList(stadiums);
    }

    /**
     * Builds the four seating sections from explicit grids. Capacities are the real
     * published figures, so the grids are sized to total exactly the stated
     * capacity, which {@link Stadium} validates.
     */
    private static List<SeatSection> sections(int rowsA, int seatsA, double priceA,
                                              int rowsB, int seatsB, double priceB,
                                              int rowsC, int seatsC, double priceC,
                                              int rowsD, int seatsD, double priceD) {
        List<SeatSection> sections = new ArrayList<>();
        sections.add(new SeatSection("A", SECTION_LABELS[0], rowsA, seatsA, priceA));
        sections.add(new SeatSection("B", SECTION_LABELS[1], rowsB, seatsB, priceB));
        sections.add(new SeatSection("C", SECTION_LABELS[2], rowsC, seatsC, priceC));
        sections.add(new SeatSection("D", SECTION_LABELS[3], rowsD, seatsD, priceD));
        return sections;
    }

    private static List<StadiumEvent> createEvents() {
        List<StadiumEvent> events = new ArrayList<>();

        // --- Mandela National Stadium (Namboole), Kampala ---
        events.add(game("namboole-01", "namboole", "Football",
                "Uganda Cranes", "Kenya Harambee Stars", 1, 19, 30, 17, 30, 1.25,
                "A CECAFA qualifier at the national stadium, with the Cranes playing in front of a "
                        + "full Namboole crowd."));
        events.add(game("namboole-02", "namboole", "Football",
                "Vipers SC", "SC Villa", 3, 15, 0, 13, 0, 1.20,
                "The biggest club duel on the calendar, with the two most successful Kampala sides "
                        + "meeting on the big stage."));
        events.add(concert("namboole-03", "namboole", "Eddy Kenzo Live at Namboole",
                "Eddy Kenzo", 2, 20, 0, 18, 0, 1.10,
                "The Big Talent boss brings his dancehall catalogue to the national stadium for a full "
                        + "night of hits."));
        events.add(game("namboole-04", "namboole", "Football",
                "Uganda Cranes", "Tanzania Taifa Stars", 5, 20, 0, 18, 0, 1.20,
                "A regional derby in the east, with the Cranes seeking revenge from the last meeting."));
        events.add(concert("namboole-05", "namboole", "Bwatibhappu Festival Night",
                "Bobi Wine", 6, 20, 0, 18, 30, 1.15,
                "One night of the Bwatibhappu festival: a homecoming show with a full light and "
                        + "effects production."));
        events.add(game("namboole-06", "namboole", "Football",
                "KCCA FC", "Express FC", 8, 19, 0, 17, 0, 1.10,
                "A Kampala derby at national stadium scale, between the city club and the old guard."));
        events.add(game("namboole-07", "namboole", "Football",
                "URA FC", "Police FC", 10, 16, 0, 14, 0, 1.05,
                "Two of the country's oldest institution clubs, URA and Police, meeting on the big "
                        + "stage."));
        events.add(concert("namboole-08", "namboole", "Jose Chameleone: The Golden Voice",
                "Jose Chameleone", 12, 20, 0, 18, 0, 1.00,
                "The Leone Island boss returns to the national stadium with a career-spanning set."));

        // --- Hoima City Stadium, Hoima ---
        events.add(game("hoima-01", "hoima-city", "Football",
                "Kitara FC", "NEC FC", 1, 16, 0, 14, 0, 1.10,
                "A western Uganda heavyweight fixture in front of a home crowd that fills the Drum."));
        events.add(game("hoima-02", "hoima-city", "Football",
                "Kitara FC", "BUL FC", 4, 19, 0, 17, 0, 1.05,
                "A midweek evening clash with both sides chasing qualification points."));
        events.add(concert("hoima-03", "hoima-city", "Azawi Live in Hoima",
                "Azawi", 3, 20, 0, 18, 0, 1.00,
                "The Mother of Uganda's music takes her Naponda sound west to Hoima."));
        events.add(game("hoima-04", "hoima-city", "Football",
                "Mbarara City FC", "Kigezi Homeboyz FC", 6, 15, 0, 13, 0, 1.00,
                "Two of Uganda's best-supported regional sides meet in a local derby atmosphere."));

        // --- Hamz Stadium (Nakivubo), Kampala ---
        events.add(game("hamz-01", "hamz-stadium", "Football",
                "Express FC", "SC Villa", 1, 16, 30, 14, 30, 1.15,
                "The most traditional fixture on the Ugandan calendar, played a short walk from "
                        + "Kampala's market."));
        events.add(game("hamz-02", "hamz-stadium", "Football",
                "Express FC", "Soltilo Bright Stars FC", 4, 19, 0, 17, 0, 1.05,
                "A packed Nakivubo evening where the Red put on a show for their city supporters."));
        events.add(concert("hamz-03", "hamz-stadium", "Bebe Cool Live",
                "Bebe Cool", 2, 20, 0, 18, 30, 1.05,
                "The East African legend brings his dancehall gospel to the historic Nakivubo bowl."));
        events.add(game("hamz-04", "hamz-stadium", "Football",
                "UPDF FC", "Police FC", 7, 16, 0, 14, 0, 1.00,
                "Two disciplined institution sides meet in a tight Nakivubo fixture."));
        events.add(concert("hamz-05", "hamz-stadium", "King Saha Solo Live",
                "King Saha", 9, 20, 0, 18, 0, 0.95,
                "The Ggaba boss on a solo run through his dancehall hits in an intimate setting."));

        // --- St. Mary's Stadium, Kitende ---
        events.add(game("st-marys-01", "st-marys", "Football",
                "Vipers SC", "KCCA FC", 1, 19, 0, 17, 30, 1.25,
                "A title-deciding fixture in a stadium barely 20 minutes from Kampala, with a famously "
                        + "loud home crowd."));
        events.add(game("st-marys-02", "st-marys", "Football",
                "Vipers SC", "Express FC", 4, 19, 0, 17, 0, 1.15,
                "The Venoms look to continue a strong run at home against the old capital club."));
        events.add(game("st-marys-03", "st-marys", "Football",
                "Vipers SC", "Maroons FC", 7, 15, 0, 13, 0, 1.05,
                "A weekend afternoon home fixture with the chance to catch a rise in form."));
        events.add(concert("st-marys-04", "st-marys", "Radio and Weasel Live",
                "Radio & Weasel", 2, 20, 0, 18, 0, 1.00,
                "The duo behind Sugendalo return to a stadium stage for a full-band celebration."));
        events.add(game("st-marys-05", "st-marys", "Football",
                "Vipers SC", "Kigezi Homeboyz FC", 10, 19, 0, 17, 0, 1.05,
                "A home meeting with a Western Region side that always travels well."));

        // --- MTN Omondi Stadium, Lugogo ---
        events.add(game("omondi-01", "omondi", "Football",
                "KCCA FC", "SC Villa", 1, 16, 0, 14, 0, 1.15,
                "The biggest attendance of any Kampala club match, played a stone's throw from the "
                        + "city centre."));
        events.add(game("omondi-02", "omondi", "Football",
                "KCCA FC", "URA FC", 4, 19, 0, 17, 0, 1.05,
                "The city club against the Revenue Authority, a fixture with a big following on both "
                        + "sides."));
        events.add(game("omondi-03", "omondi", "Netball",
                "She Cranes", "Zambia", 2, 18, 0, 16, 0, 1.00,
                "Uganda's national netball side in front of a home crowd at the Lugogo indoor arena."));
        events.add(game("omondi-04", "omondi", "Football",
                "KCCA FC", "Lugazi FC", 6, 15, 0, 13, 0, 1.00,
                "An afternoon Lugogo fixture with the city club seeking a comfortable home win."));

        // --- Mutesa II Stadium, Wankulukuku ---
        events.add(game("mutesa-01", "mutesa-ii", "Football",
                "Kataka FC", "Blacks Power FC", 1, 19, 0, 17, 0, 1.05,
                "A Wankulukuku evening between two of the newest Uganda Premier League clubs."));
        events.add(concert("mutesa-02", "mutesa-ii", "Fame Fest Live",
                "Fik Fameica", 3, 20, 0, 18, 0, 1.00,
                "The Gulu-born star brings his Fame Fest energy to a Kampala stage."));
        events.add(game("mutesa-03", "mutesa-ii", "Football",
                "Blacks Power FC", "Ntugasaze FC", 5, 19, 0, 17, 0, 1.00,
                "A night match under the floodlights with two promotion-chasing sides."));

        // --- FUFA Kadiba Stadium, Kampala ---
        events.add(game("kadiba-01", "kadiba", "Football",
                "SC Villa", "Express FC", 1, 19, 0, 17, 0, 1.15,
                "The most decorated clubs in Ugandan football meet on the federation's new ground."));
        events.add(game("kadiba-02", "kadiba", "Football",
                "SC Villa", "Buhimba United Saints FC", 4, 15, 0, 13, 0, 1.00,
                "A home fixture where the Villa look to build on a strong start to the season."));
        events.add(concert("kadiba-03", "kadiba", "Sheebah Live in Kampala",
                "Sheebah", 2, 20, 0, 18, 0, 0.95,
                "The Wololo star performs a high-energy set for her home city."));

        // --- Bunamwaya Stadium, Wakiso ---
        events.add(game("bunamwaya-01", "bunamwaya", "Football",
                "Gaddafi FC", "Booma FC", 1, 16, 0, 14, 0, 1.00,
                "A packed community ground meeting for two sides that play the game close to the "
                        + "terraces."));
        events.add(game("bunamwaya-02", "bunamwaya", "Football",
                "Onduparaka FC", "Kiyinda Boys FC", 4, 19, 0, 17, 0, 0.95,
                "A midweek tie under lights in a genuinely local football atmosphere."));
        events.add(concert("bunamwaya-03", "bunamwaya", "Ndere Live",
                "Juliana Kanyomozi", 3, 20, 0, 18, 0, 0.95,
                "The Queen of Ugandan music leads a traditional and contemporary set for her home "
                        + "crowd."));

        // --- Kyabazinga Stadium, Bugembe, Jinja ---
        events.add(game("bugembe-01", "bugembe", "Football",
                "Jinja North United FC", "BUL FC", 1, 15, 0, 13, 0, 1.05,
                "A Busoga derby afternoon in Jinja, played in front of a famously vocal home crowd."));
        events.add(game("bugembe-02", "bugembe", "Football",
                "BUL FC", "Maroons FC", 4, 19, 0, 17, 0, 1.00,
                "An evening Jinja meeting with the two eastern sides looking for a home result."));
        events.add(concert("bugembe-03", "bugembe", "Spice Diana Live",
                "Spice Diana", 2, 20, 0, 18, 0, 1.00,
                "The Queen of Ugandan music headlines a full evening in her home region."));
        events.add(game("bugembe-04", "bugembe", "Rugby",
                "Heathens RFC", "Kampala RFC", 6, 16, 0, 14, 0, 1.00,
                "A club rugby fixture in the east, played in front of rugby's strong Jinja following."));

        // --- Mbale Municipal Stadium ---
        events.add(game("mbale-01", "mbale-municipal", "Football",
                "Kataka FC", "Blacks Power FC", 1, 15, 0, 13, 0, 1.00,
                "An eastern Uganda weekend fixture in the shadow of Mount Elgon."));
        events.add(game("mbale-02", "mbale-municipal", "Football",
                "MYDA FC", "Bujagali United", 4, 19, 0, 17, 0, 0.95,
                "A Big League night match where every point matters in the promotion race."));
        events.add(concert("mbale-03", "mbale-municipal", "John Blaq Live",
                "John Blaq", 2, 20, 0, 18, 0, 0.95,
                "The Nabukalu banger brings his own dancehall style to the eastern highlands."));

        // --- Pece War Memorial Stadium, Gulu ---
        events.add(game("pece-01", "pece-war", "Football",
                "Gulu United FC", "Young Elephant FC", 1, 15, 0, 13, 0, 1.00,
                "A northern Uganda derby at the country's oldest major ground, restored with "
                        + "support from the community."));
        events.add(game("pece-02", "pece-war", "Football",
                "Acholi United", "Kigezi Homeboyz FC", 4, 19, 0, 17, 0, 0.95,
                "A night fixture where visitors from the west travel all the way to the far north."));
        events.add(concert("pece-03", "pece-war", "Iryn Namubiru Live",
                "Iryn Namubiru", 3, 20, 0, 18, 0, 0.95,
                "A gospel and soul evening in the north, in front of a capacity crowd."));

        return Collections.unmodifiableList(events);
    }

    private static List<StadiumAnnouncement> createAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        List<StadiumAnnouncement> announcements = new ArrayList<>();
        announcements.add(new StadiumAnnouncement(
                "namboole-notice-01", "namboole", "namboole-01", AnnouncementType.NOTICE,
                "Match-day entry and security screening",
                "Spectators should arrive early through the north gate. Accessible seating and "
                        + "wheelchair spaces are available from the VIP box entrance.",
                now.minusHours(2), now.plusDays(30)));
        announcements.add(new StadiumAnnouncement(
                "namboole-cancel-01", "namboole", "namboole-04", AnnouncementType.CANCELLATION,
                "Match postponed — heavy rain warning",
                "The Cranes vs Taifa Stars qualifier has been postponed while the venue team monitors "
                        + "a heavy rain warning for the Kampala area.",
                now.minusHours(1), now.plusDays(14)));
        announcements.add(new StadiumAnnouncement(
                "namboole-schedule-01", "namboole", "", AnnouncementType.SCHEDULE_CHANGE,
                "Concert gates may open earlier",
                "For concerts, gates may open up to 60 minutes before the published doors time to "
                        + "allow security screening.",
                now.minusDays(1), now.plusDays(45)));
        announcements.add(new StadiumAnnouncement(
                "hamz-emergency-01", "hamz-stadium", "hamz-01", AnnouncementType.EMERGENCY,
                "Entrance change for the Express derby",
                "The western turnstile is closed for maintenance. Please follow stadium staff to the "
                        + "southern entrance off Busikwa Road.",
                now.minusMinutes(30), now.plusDays(2)));
        announcements.add(new StadiumAnnouncement(
                "kitende-notice-01", "st-marys", "st-marys-01", AnnouncementType.NOTICE,
                "Home fans arrival support",
                "Home supporters should use the Entebbe Road entrance. Accessible seating supporters "
                        + "can contact the venue team in advance for assisted entry.",
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
