package com.stadium.booking;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * All user-facing wording in one place, so the interface can run in English,
 * Luganda or Swahili.
 *
 * <p>An English fallback is always used for any key a translation has not
 * covered yet, so a missing translation can never leave a blank label.
 */
public final class Messages {
    /** The languages the interface offers. */
    public enum Language {
        ENGLISH("en", "English"),
        LUGANDA("lg", " Luganda"),
        SWAHILI("sw", "Kiswahili");

        private final String code;
        private final String label;

        Language(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static Language current = Language.ENGLISH;

    private Messages() {
    }

    public static void setLanguage(Language language) {
        current = language == null ? Language.ENGLISH : language;
    }

    public static Language getLanguage() {
        return current;
    }

    private static final Map<Language, Map<String, String>> TEXT = new LinkedHashMap<>();

    private static void put(Language language, String key, String value) {
        TEXT.computeIfAbsent(language, ignored -> new LinkedHashMap<>()).put(key, value);
    }

    static {
        // English is the reference wording.
        put(Language.ENGLISH, "app.title", "Stadium Select");
        put(Language.ENGLISH, "app.tagline", "STADIUM SELECT  /  LIVE EVENT TICKETS");
        put(Language.ENGLISH, "nav.back", "← Back");
        put(Language.ENGLISH, "nav.venues", "Venues");
        put(Language.ENGLISH, "nav.bookings", "My bookings");
        put(Language.ENGLISH, "nav.bookedSeats", "Booked seats");
        put(Language.ENGLISH, "nav.liveSchedules", "LIVE SCHEDULES");
        put(Language.ENGLISH, "directory.title", "Choose your stadium");
        put(Language.ENGLISH, "directory.subtitle",
                "Start with the venue, then choose the date and event you want to attend.");
        put(Language.ENGLISH, "directory.hero.eyebrow", "VENUE DIRECTORY");
        put(Language.ENGLISH, "directory.hero.title", "Find your next live event");
        put(Language.ENGLISH, "directory.hero.subtitle",
                "Browse stadiums, check what is on, and reserve the right seats in a few clear steps.");
        put(Language.ENGLISH, "directory.findVenue", "FIND A VENUE");
        put(Language.ENGLISH, "directory.search.hint", "Search stadium, city, team or artist");
        put(Language.ENGLISH, "directory.venuesAvailable", "{0} venues available");
        put(Language.ENGLISH, "directory.venuesMatching", "{0} venues matching your search");
        put(Language.ENGLISH, "directory.open", "Open stadium");
        put(Language.ENGLISH, "directory.viewDetails", "View details");
        put(Language.ENGLISH, "stadium.upcoming", "Upcoming schedule");
        put(Language.ENGLISH, "stadium.search.hint", "Search teams, artists, sport or date");
        put(Language.ENGLISH, "stadium.subtitle",
                "Choose a date, then select the game or concert you want to attend.");
        put(Language.ENGLISH, "stadium.notices", "Notices & requests");
        put(Language.ENGLISH, "stadium.specialRequest", "Submit a special request");
        put(Language.ENGLISH, "booking.title", "Book your seats");
        put(Language.ENGLISH, "booking.yourDetails", "Your details");
        put(Language.ENGLISH, "booking.threeDetails", "Just three details are needed");
        put(Language.ENGLISH, "booking.name", "Name");
        put(Language.ENGLISH, "booking.email", "Email");
        put(Language.ENGLISH, "booking.phone", "Phone");
        put(Language.ENGLISH, "booking.chooseSeats", "Choose your seats");
        put(Language.ENGLISH, "booking.selectSeat", "SELECT A SEAT");
        put(Language.ENGLISH, "booking.priceOutline", "Price outline");
        put(Language.ENGLISH, "booking.total", "TOTAL");
        put(Language.ENGLISH, "booking.vacancy", "VACANCY");
        put(Language.ENGLISH, "booking.seatsHeld", "SEATS HELD");
        put(Language.ENGLISH, "booking.yourSelection", "YOUR SELECTION");
        put(Language.ENGLISH, "booking.noSeats", "No seats selected");
        put(Language.ENGLISH, "booking.clearSeats", "Clear seats");
        put(Language.ENGLISH, "booking.confirm", "Confirm booked seats");
        put(Language.ENGLISH, "booking.viewBooked", "View booked seats");
        put(Language.ENGLISH, "booking.seatSubtotal", "Seat subtotal");
        put(Language.ENGLISH, "booking.bookingFee", "Booking fee (once)");
        put(Language.ENGLISH, "booking.totalDue", "Total due");
        put(Language.ENGLISH, "legend.vacant", "Vacant");
        put(Language.ENGLISH, "legend.selected", "Selected");
        put(Language.ENGLISH, "legend.held", "Held");
        put(Language.ENGLISH, "legend.booked", "Booked");
        put(Language.ENGLISH, "legend.closed", "Closed");
        put(Language.ENGLISH, "bookings.title", "Booking history");
        put(Language.ENGLISH, "bookings.subtitle",
                "Every confirmed or cancelled reservation in one place.");
        put(Language.ENGLISH, "bookings.search.hint", "Search bookings");
        put(Language.ENGLISH, "bookings.refresh", "Refresh");
        put(Language.ENGLISH, "bookings.cancel", "Cancel selected booking");
        put(Language.ENGLISH, "bookings.export", "Export CSV");
        put(Language.ENGLISH, "seats.title", "Seats booked so far");
        put(Language.ENGLISH, "seats.subtitle", "See every seat already taken, by stadium and by event.");
        put(Language.ENGLISH, "status.ready", "Ready");
        put(Language.ENGLISH, "status.chooseStadium", "Choose a stadium to begin");
        put(Language.ENGLISH, "common.back", "Back");
        put(Language.ENGLISH, "common.cancel", "Cancel");
        put(Language.ENGLISH, "common.close", "Close");
        put(Language.ENGLISH, "common.signIn", "Sign in");
        put(Language.ENGLISH, "common.pin", "Staff PIN");
        put(Language.ENGLISH, "common.pay", "Pay");
        put(Language.ENGLISH, "common.print", "Print");
        put(Language.ENGLISH, "common.save", "Save as text");

        put(Language.ENGLISH, "booking.priceGuide", "Booked seats at this event");
        put(Language.ENGLISH, "booking.priceHint", "Seat prices vary by row and section.");
        put(Language.ENGLISH, "booking.feeHint", "One booking fee per reservation");
        put(Language.ENGLISH, "booking.limitHint", "No limit per person • up to 6 seats per reservation");
        put(Language.ENGLISH, "booking.priceSubtitle", "Every charge is shown before you confirm");
        put(Language.ENGLISH, "bookings.hint",
                "Click any booking row to view the complete reservation and customer details.");
        put(Language.ENGLISH, "bookings.cancelSelected", "Cancel selected booking");
        put(Language.ENGLISH, "common.backToStadiums", "\u2190 Back to stadiums");
        put(Language.ENGLISH, "common.backToSchedule", "\u2190 Back to schedule");
        put(Language.ENGLISH, "nav.occupancy", "Occupancy");
        put(Language.ENGLISH, "nav.staff", "Staff");
        put(Language.ENGLISH, "occupancy.title", "Occupancy report");
        put(Language.ENGLISH, "occupancy.subtitle",
                "How full every venue and section is, across all events.");
        put(Language.ENGLISH, "occupancy.export", "Export occupancy CSV");
        put(Language.ENGLISH, "payment.title", "Payment");
        put(Language.ENGLISH, "payment.question", "How would you like to pay?");
        put(Language.ENGLISH, "common.totalDue", "Total due");
        put(Language.ENGLISH, "seatMap.hint",
                "Front rows are premium \u2022 tap a seat, or use the arrow keys");
        put(Language.ENGLISH, "seatMap.priceGuide", "Prices fall from front to back");
        put(Language.ENGLISH, "occupancy.allVenues", "Occupancy across all venues");
        put(Language.ENGLISH, "occupancy.note",
                "Confirmed seats only. Cancelled bookings free their seats.");
        put(Language.ENGLISH, "occupancy.vacancyNote",
                "Occupancy of the venue and event you are booking on.");
        put(Language.ENGLISH, "seats.everyConfirmed",
                "Every confirmed seat for the selected event, newest first.");
        put(Language.ENGLISH, "payment.simulatedNote",
                "mobile money is simulated, no money moves");
        put(Language.ENGLISH, "common.pinHint",
                "Default PIN is 1234. Contact details stay hidden until you sign in.");
        put(Language.ENGLISH, "booking.seatPriceGuide", "Seat prices vary by row and section.");
        put(Language.ENGLISH, "booking.emptyPrice",
                "Select one or more vacant seats to see the full price outline.");
        put(Language.ENGLISH, "common.allVenues", "\u2190 All stadiums");

        // Luganda
        put(Language.LUGANDA, "nav.back", "← Subira");
        put(Language.LUGANDA, "nav.venues", "Amabirali");
        put(Language.LUGANDA, "nav.bookings", "Ebikwata byanjulo");
        put(Language.LUGANDA, "nav.bookedSeats", "Ebisitansa ebikwata");
        put(Language.LUGANDA, "nav.liveSchedules", "EBIRO LUNDA");
        put(Language.LUGANDA, "directory.title", "Londa ekizimbe k'ekyamukaliro");
        put(Language.LUGANDA, "directory.hero.title", "Funa ekigyendererwa ekijja");
        put(Language.LUGANDA, "directory.findVenue", "LONDA EKIZIMBE");
        put(Language.LUGANDA, "directory.search.hint", "Shunja ekizimbe, egga, ekizibuBy'obujjuni");
        put(Language.LUGANDA, "directory.open", "Kikikizo");
        put(Language.LUGANDA, "directory.viewDetails", "Lola ebisinga by'obuddwa");
        put(Language.LUGANDA, "booking.title", "Yatandika ebizitansa");
        put(Language.LUGANDA, "booking.yourDetails", "Ebikwata by'ekisobola");
        put(Language.LUGANDA, "booking.name", "Amalinnya");
        put(Language.LUGANDA, "booking.email", "Imeera y'okwandika");
        put(Language.LUGANDA, "booking.phone", "Ennimi");
        put(Language.LUGANDA, "booking.chooseSeats", "Londa ebizitansa");
        put(Language.LUGANDA, "booking.priceOutline", "Ennimiro y'omutunda");
        put(Language.LUGANDA, "booking.total", "TOTAL");
        put(Language.LUGANDA, "booking.vacancy", "EZZAALIRO");
        put(Language.LUGANDA, "booking.yourSelection", "OKATORODDA");
        put(Language.LUGANDA, "booking.noSeats", "Tewali buzitansa");
        put(Language.LUGANDA, "booking.clearSeats", "Ggiriza");
        put(Language.LUGANDA, "booking.confirm", "Wekakise ebizitansa");
        put(Language.LUGANDA, "legend.vacant", "Naludika");
        put(Language.LUGANDA, "legend.selected", "Waliwo");
        put(Language.LUGANDA, "legend.held", "Wamanyagiridwa");
        put(Language.LUGANDA, "legend.booked", "Wakibwa");
        put(Language.LUGANDA, "legend.closed", "Wafunye");
        put(Language.LUGANDA, "bookings.title", "Ebikwata byanjulo");
        put(Language.LUGANDA, "bookings.refresh", "Yakiza");
        put(Language.LUGANDA, "bookings.cancel", "Sikiza ekyali");
        put(Language.LUGANDA, "bookings.export", "Kikola CSV");
        put(Language.LUGANDA, "seats.title", "Ebizitansa ebikwata");
        put(Language.LUGANDA, "common.signIn", "Yingira");
        put(Language.LUGANDA, "common.pin", "PIN ow'omunanny");

        // Swahili
        put(Language.SWAHILI, "nav.back", "← Rudi");
        put(Language.SWAHILI, "nav.venues", "Uwanja");
        put(Language.SWAHILI, "nav.bookings", "Reservations zangu");
        put(Language.SWAHILI, "nav.bookedSeats", "Viti vilivyookwa");
        put(Language.SWAHILI, "nav.liveSchedules", "RATIBU YA MOJA KWA MOJA");
        put(Language.SWAHILI, "directory.title", "Chagua uwanja");
        put(Language.SWAHILI, "directory.hero.title", "Pata matukio yajayo");
        put(Language.SWAHILI, "directory.findVenue", "TAFAUTA UWANJA");
        put(Language.SWAHILI, "directory.search.hint", "Tafuta uwanja, mji, timu au msanii");
        put(Language.SWAHILI, "directory.open", "Fungua uwanja");
        put(Language.SWAHILI, "directory.viewDetails", "Angalia maelezo");
        put(Language.SWAHILI, "booking.title", "Viti vyako");
        put(Language.SWAHILI, "booking.yourDetails", "Maelezo yako");
        put(Language.SWAHILI, "booking.name", "Jina");
        put(Language.SWAHILI, "booking.email", "Barua pepe");
        put(Language.SWAHILI, "booking.phone", "Namba ya simu");
        put(Language.SWAHILI, "booking.chooseSeats", "Chagua viti");
        put(Language.SWAHILI, "booking.priceOutline", "Muhtasari wa bei");
        put(Language.SWAHILI, "booking.total", "JUMLA");
        put(Language.SWAHILI, "booking.vacancy", "NAFASI");
        put(Language.SWAHILI, "booking.yourSelection", "ULICHAGUA");
        put(Language.SWAHILI, "booking.noSeats", "Hakuna viti vilivyochaguliwa");
        put(Language.SWAHILI, "booking.clearSeats", "Futa");
        put(Language.SWAHILI, "booking.confirm", "Thibitisha viti");
        put(Language.SWAHILI, "legend.vacant", "Zilizo huria");
        put(Language.SWAHILI, "legend.selected", "Vimechaguliwa");
        put(Language.SWAHILI, "legend.held", "Vimeshikiliwa");
        put(Language.SWAHILI, "legend.booked", "Vimeuzwa");
        put(Language.SWAHILI, "legend.closed", "Yimefungwa");
        put(Language.SWAHILI, "bookings.title", "Historia ya reservations");
        put(Language.SWAHILI, "bookings.refresh", "Onyesha upya");
        put(Language.SWAHILI, "bookings.cancel", "Ghairi iliyochaguliwa");
        put(Language.SWAHILI, "bookings.export", "Hamisha CSV");
        put(Language.SWAHILI, "seats.title", "Viti vilivyookwa hadi sasa");
        put(Language.SWAHILI, "common.signIn", "Ingia");
        put(Language.SWAHILI, "common.pin", "PIN ya wafanyakazi");

        put(Language.LUGANDA, "directory.subtitle",
                "Katandika ekizimbe, mbeera olunaku lwa ekizibuBy'obujjuni oluyako.");
        put(Language.LUGANDA, "directory.hero.eyebrow", "EBIRO BY'AMABIRALI");
        put(Language.LUGANDA, "directory.hero.subtitle",
                "Lulambula amabirali, keka ebbaliwo, era yatandika ebizitansa nga bwe kisobola.");
        put(Language.LUGANDA, "directory.venuesAvailable", "Amabirali {0} awo aliwo");
        put(Language.LUGANDA, "directory.venuesMatching", "Amabirali {0} agasobola n'okunoonya");
        put(Language.LUGANDA, "stadium.upcoming", "Ebiro nga biri");
        put(Language.LUGANDA, "stadium.search.hint",
                "Shunja ekizibuBy'obujjuni, omusani, sport n'obudde");
        put(Language.LUGANDA, "stadium.notices", "Ebikwata n'okusaba");
        put(Language.LUGANDA, "stadium.specialRequest", "Saba ekikwata ekikwateekero");
        put(Language.LUGANDA, "stadium.specialRequest.submit", "Yatindika okusaba");
        put(Language.LUGANDA, "booking.threeDetails", "Ebituuka bibiri gusa birina");
        put(Language.LUGANDA, "booking.selectSeat", "LONDA EKIZITANSA");
        put(Language.LUGANDA, "booking.seatsHeld", "EBIZITANSA EBIRAMBIDDE");
        put(Language.LUGANDA, "booking.viewBooked", "Loola ebizitansa ebikwata");
        put(Language.LUGANDA, "booking.priceGuide", "Ebizitansa ebikwata ebisobola ku ngalo");
        put(Language.LUGANDA, "booking.priceHint", "Emirimu egendera ku lutundu n'ekizimbe.");
        put(Language.LUGANDA, "booking.feeHint", "Kikumi k'ekikwata kimwe ku nkwaateeka");
        put(Language.LUGANDA, "booking.limitHint", "Tewali kkano • kiwango kya vituuka 6 ku nkwaateeka");
        put(Language.LUGANDA, "booking.priceSubtitle", "Bwe buli ntikita bonynkusooka nga osazeeko");
        put(Language.LUGANDA, "bookings.subtitle", "Byonna ebikwata ebirimu n'ebitannyalukirwa mu kifo kina.");
        put(Language.LUGANDA, "bookings.hint",
                "Kikwata ekigyendererwa okulaba ekikwata by'ekintu n'ebisubiraby'omuntu.");
        put(Language.LUGANDA, "bookings.cancelSelected", "Sikiza ekikwata ekisobola");
        put(Language.LUGANDA, "seats.subtitle",
                "Loola kila kifo ekikwata, ku nzimbe n'ekigyendererwa.");
        put(Language.LUGANDA, "common.backToStadiums", "← Amabirali onoona");
        put(Language.LUGANDA, "common.backToSchedule", "← Subira ku lutundu");
        put(Language.LUGANDA, "nav.occupancy", "Ebikwata");
        put(Language.LUGANDA, "nav.staff", "Abasa");
        put(Language.LUGANDA, "occupancy.title", "Ebikwata by'ebizitansa");
        put(Language.LUGANDA, "occupancy.subtitle",
                "Ebizitansa ebikwata buli kizimbe n'ekizimbe, mu bweru bw'ekigyendererwa.");
        put(Language.LUGANDA, "occupancy.export", "Kikola CSV y'ebikwata");
        put(Language.LUGANDA, "payment.title", "Nn-payment");
        put(Language.LUGANDA, "payment.question", "Wandi wa mula okugy Payment?");
        put(Language.LUGANDA, "common.totalDue", "Amagazi a'okugy Payment");
        put(Language.LUGANDA, "seatMap.hint", "Emiganda esooka eri eby'omugaso • kikka kifo, oba kozooda endoboozi");
        put(Language.LUGANDA, "seatMap.priceGuide", "Emirimu ekkka ku ngalo kubanga ku ngalo");

        put(Language.SWAHILI, "directory.subtitle",
                "Anza na uwanja, kisha chagua tarehe na matukio unayotaka.");
        put(Language.SWAHILI, "directory.hero.eyebrow", "OBDHA YA UWANJA");
        put(Language.SWAHILI, "directory.hero.subtitle",
                "Tembelea uwanja, angalia matukio yako, na hifadhi viti sahihi kwa hatua rahisi.");
        put(Language.SWAHILI, "directory.venuesAvailable", "Uwanja {0} zilizopo");
        put(Language.SWAHILI, "directory.venuesMatching", "Uwanja {0} zinalingana na utafutaji wako");
        put(Language.SWAHILI, "stadium.upcoming", "Ratiba ijayo");
        put(Language.SWAHILI, "stadium.search.hint", "Tafuta timu, msanii, mchezo au tarehe");
        put(Language.SWAHILI, "stadium.notices", "Taarifa na maombi");
        put(Language.SWAHILI, "stadium.specialRequest", "Wasilisha ombi maalum");
        put(Language.SWAHILI, "stadium.specialRequest.submit", "Wasilisha ombi");
        put(Language.SWAHILI, "booking.threeDetails", "Maelezo matatu tu ndiyo yanayohitajika");
        put(Language.SWAHILI, "booking.selectSeat", "CHAGUA KITI");
        put(Language.SWAHILI, "booking.seatsHeld", "VITI VILIVYOSHIKILIWA");
        put(Language.SWAHILI, "booking.viewBooked", "Ona viti vilivyookwa");
        put(Language.SWAHILI, "booking.priceGuide", "Viti vilivyookwa zinaonyeshwa hapa");
        put(Language.SWAHILI, "booking.priceHint", "Bei za viti hutofautiana kwa safu na sehemu.");
        put(Language.SWAHILI, "booking.feeHint", "Kadi moja kwa kila reservation");
        put(Language.SWAHILI, "booking.limitHint", "Hakio kikomo • hadi viti 6 kwa kila reservation");
        put(Language.SWAHILI, "booking.priceSubtitle", "Kila malipo yanaonyeshwa kabla ya kuthibitisha");
        put(Language.SWAHILI, "bookings.subtitle",
                "Reservations zote zilizothibitishwa au zilizoghairiwa mahali pamoja.");
        put(Language.SWAHILI, "bookings.hint",
                "Bofya safu yoyote ili kuona reservation kamili na maelezo ya mteja.");
        put(Language.SWAHILI, "bookings.cancelSelected", "Ghairi iliyochaguliwa");
        put(Language.SWAHILI, "seats.subtitle",
                "Ona kila kiti kilichookwa, kwa uwanja na kwa matukio.");
        put(Language.SWAHILI, "common.backToStadiums", "← Uwanja zote");
        put(Language.SWAHILI, "common.backToSchedule", "← Rudi kwa ratiba");
        put(Language.SWAHILI, "nav.occupancy", "Kujaza");
        put(Language.SWAHILI, "nav.staff", "Wafanyakazi");
        put(Language.SWAHILI, "occupancy.title", "Ripoti ya kujaza viti");
        put(Language.SWAHILI, "occupancy.subtitle",
                "Jinsi gani kila uwanja na sehemu ilivyojaa, katika matukio yote.");
        put(Language.SWAHILI, "occupancy.export", "Hamisha CSV ya kujaza");
        put(Language.SWAHILI, "payment.title", "Malipo");
        put(Language.SWAHILI, "payment.question", "Ungependa kulipa vipi?");
        put(Language.SWAHILI, "common.totalDue", "Inayotakiwa kulipa");
        put(Language.SWAHILI, "seatMap.hint",
                "Safu za mbele ni za kipekee • bofya kiti, au tumia vishale vya mishale");
        put(Language.SWAHILI, "seatMap.priceGuide", "Bei zinashuka kutoka mbele hadi nyuma");
    }

    /**
     * The wording for a key in the current language, falling back to English and
     * then to the key itself so nothing ever renders blank.
     */
    public static String get(String key) {
        Map<String, String> language = TEXT.get(current);
        if (language != null) {
            String value = language.get(key);
            if (value != null) {
                return value;
            }
        }
        String fallback = TEXT.get(Language.ENGLISH).get(key);
        return fallback == null ? key : fallback;
    }

    /** Wording with a single number substituted, for example "3 venues available". */
    public static String get(String key, Object... arguments) {
        String template = get(key);
        for (int index = 0; index < arguments.length; index++) {
            template = template.replace("{" + index + "}", String.valueOf(arguments[index]));
        }
        return template;
    }

    /** Shillings are always written as UGX so a translated screen still reads clearly. */
    public static String money(double amount) {
        return BookingService.formatMoney(amount);
    }

    public static String dateLabel(java.time.LocalDate date) {
        return date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM yyyy",
                current == Language.SWAHILI ? Locale.forLanguageTag("sw") : Locale.ENGLISH));
    }
}
