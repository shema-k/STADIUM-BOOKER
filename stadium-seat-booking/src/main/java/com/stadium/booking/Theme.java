package com.stadium.booking;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The colours the whole interface is painted with.
 *
 * <p>Every screen reads from here rather than building colours of its own, which is
 * what lets dark mode be a single switch rather than a second version of the
 * application. Each screen builder is written once and re-run when the theme
 * changes, so nothing has to know how it was painted last time.
 *
 * <p>Seat states are held in here too, because a seat that reads as "booked" must
 * still read as booked in the dark.
 */
public final class Theme {
    /** What a screen is painted with. */
    public static final class Palette {
        private final boolean dark;
        private final Color page;
        private final Color card;
        private final Color text;
        private final Color muted;
        private final Color border;
        private final Color header;
        private final Color headerSoft;
        private final Color headerText;
        private final Color headerMuted;
        private final Color accent;
        private final Color accentDark;
        private final Color onAccent;
        private final Color tableHeader;
        private final Color tableGrid;
        private final Color success;
        private final Color warning;
        private final Color danger;
        private final Color cancelled;
        private final Color field;
        private final Color fieldBorder;
        private final Color seatVacant;
        private final Color seatVacantText;
        private final Color seatSelected;
        private final Color seatSelectedText;
        private final Color seatBooked;
        private final Color seatBookedText;
        private final Color seatClosed;
        private final Color seatClosedText;
        private final Color seatHeld;
        private final Color seatHeldText;
        private final Color seatOutline;
        private final Color mapBackground;
        private final Color mapBorder;
        private final Color pitch;
        private final Color grassTop;
        private final Color grassBottom;
        private final Color skyTop;
        private final Color groundTop;
        private final Color groundBottom;
        private final Color captionText;

        private Palette(boolean dark, Color[] values) {
            int index = 0;
            this.dark = dark;
            this.page = values[index++];
            this.card = values[index++];
            this.text = values[index++];
            this.muted = values[index++];
            this.border = values[index++];
            this.header = values[index++];
            this.headerSoft = values[index++];
            this.headerText = values[index++];
            this.headerMuted = values[index++];
            this.accent = values[index++];
            this.accentDark = values[index++];
            this.onAccent = values[index++];
            this.tableHeader = values[index++];
            this.tableGrid = values[index++];
            this.success = values[index++];
            this.warning = values[index++];
            this.danger = values[index++];
            this.cancelled = values[index++];
            this.field = values[index++];
            this.fieldBorder = values[index++];
            this.seatVacant = values[index++];
            this.seatVacantText = values[index++];
            this.seatSelected = values[index++];
            this.seatSelectedText = values[index++];
            this.seatBooked = values[index++];
            this.seatBookedText = values[index++];
            this.seatClosed = values[index++];
            this.seatClosedText = values[index++];
            this.seatHeld = values[index++];
            this.seatHeldText = values[index++];
            this.seatOutline = values[index++];
            this.mapBackground = values[index++];
            this.mapBorder = values[index++];
            this.pitch = values[index++];
            this.grassTop = values[index++];
            this.grassBottom = values[index++];
            this.skyTop = values[index++];
            this.groundTop = values[index++];
            this.groundBottom = values[index++];
            this.captionText = values[index++];
        }

        public boolean isDark() {
            return dark;
        }

        public Color page() {
            return page;
        }

        public Color card() {
            return card;
        }

        public Color text() {
            return text;
        }

        public Color muted() {
            return muted;
        }

        public Color border() {
            return border;
        }

        public Color header() {
            return header;
        }

        public Color headerSoft() {
            return headerSoft;
        }

        public Color headerText() {
            return headerText;
        }

        public Color headerMuted() {
            return headerMuted;
        }

        public Color accent() {
            return accent;
        }

        public Color accentDark() {
            return accentDark;
        }

        /**
         * Text drawn on top of the accent. A bright accent needs dark text: white
         * on the dark theme's light blue was unreadable, which a contrast test
         * caught.
         */
        public Color onAccent() {
            return onAccent;
        }

        public Color tableHeader() {
            return tableHeader;
        }

        public Color tableGrid() {
            return tableGrid;
        }

        public Color success() {
            return success;
        }

        public Color warning() {
            return warning;
        }

        public Color danger() {
            return danger;
        }

        public Color cancelled() {
            return cancelled;
        }

        public Color field() {
            return field;
        }

        public Color fieldBorder() {
            return fieldBorder;
        }

        public Color seatVacant() {
            return seatVacant;
        }

        public Color seatVacantText() {
            return seatVacantText;
        }

        public Color seatSelected() {
            return seatSelected;
        }

        public Color seatSelectedText() {
            return seatSelectedText;
        }

        public Color seatBooked() {
            return seatBooked;
        }

        public Color seatBookedText() {
            return seatBookedText;
        }

        public Color seatClosed() {
            return seatClosed;
        }

        public Color seatClosedText() {
            return seatClosedText;
        }

        public Color seatHeld() {
            return seatHeld;
        }

        public Color seatHeldText() {
            return seatHeldText;
        }

        public Color seatOutline() {
            return seatOutline;
        }

        public Color mapBackground() {
            return mapBackground;
        }

        public Color mapBorder() {
            return mapBorder;
        }

        public Color pitch() {
            return pitch;
        }

        /** The grass of a drawn stadium plan, as opposed to the seat map's pitch. */
        public Color grassTop() {
            return grassTop;
        }

        public Color grassBottom() {
            return grassBottom;
        }

        /** The sky behind a drawn stadium plan. */
        public Color skyTop() {
            return skyTop;
        }

        public Color groundTop() {
            return groundTop;
        }

        public Color groundBottom() {
            return groundBottom;
        }

        public Color captionText() {
            return captionText;
        }

        /** Body text on a seat, chosen to stay readable on the seat's own fill. */
        public Color seatText(Color background) {
            return StadiumPhotoPanel.readableOn(background);
        }
    }

    private static final Palette LIGHT = new Palette(false, new Color[]{
            rgb(244, 247, 251), rgb(255, 255, 255), rgb(30, 41, 59), rgb(100, 116, 139),
            rgb(219, 228, 239), rgb(15, 35, 67), rgb(30, 57, 97), Color.WHITE,
            rgb(191, 219, 254), rgb(37, 99, 235), rgb(29, 78, 216), Color.WHITE,
            rgb(239, 246, 255),
            rgb(235, 240, 247), rgb(21, 128, 61), rgb(180, 83, 9), rgb(185, 28, 28),
            rgb(120, 130, 143), Color.WHITE, rgb(203, 213, 225),
            rgb(232, 241, 255), rgb(28, 55, 90), rgb(245, 158, 11), rgb(67, 37, 4),
            rgb(251, 213, 213), rgb(159, 18, 57), rgb(239, 229, 218), rgb(146, 104, 62),
            rgb(254, 243, 199), rgb(146, 64, 14), rgb(166, 181, 201), rgb(248, 251, 255),
            rgb(185, 201, 222), rgb(222, 242, 231), rgb(34, 120, 62), rgb(22, 92, 47),
            rgb(226, 232, 240), rgb(203, 213, 225),
            rgb(148, 163, 184), rgb(203, 213, 225)});

    private static final Palette DARK = new Palette(true, new Color[]{
            rgb(9, 14, 26), rgb(19, 28, 45), rgb(226, 232, 240), rgb(148, 163, 184),
            rgb(39, 52, 74), rgb(8, 15, 30), rgb(18, 32, 56), rgb(241, 245, 249),
            rgb(148, 163, 184), rgb(96, 165, 250), rgb(147, 197, 253), rgb(8, 15, 30),
            rgb(24, 37, 61),
            rgb(30, 41, 59), rgb(74, 222, 128), rgb(251, 191, 36), rgb(248, 113, 113),
            rgb(100, 116, 139), rgb(13, 20, 34), rgb(51, 65, 85),
            rgb(30, 48, 76), rgb(191, 219, 254), rgb(245, 158, 11), rgb(67, 37, 4),
            rgb(76, 20, 38), rgb(253, 164, 175), rgb(58, 46, 38), rgb(180, 140, 100),
            rgb(69, 46, 12), rgb(252, 211, 77), rgb(71, 85, 105), rgb(12, 20, 36),
            rgb(39, 52, 74), rgb(22, 40, 32), rgb(30, 96, 54), rgb(20, 70, 40),
            rgb(15, 23, 42), rgb(30, 41, 59),
            rgb(100, 116, 139), rgb(203, 213, 225)});

    private static Color rgb(int red, int green, int blue) {
        return new Color(red, green, blue);
    }

    private static Palette current = LIGHT;
    private static final List<Consumer<Palette>> LISTENERS = new ArrayList<>();

    private Theme() {
    }

    /** The palette in force right now. */
    public static Palette current() {
        return current;
    }

    public static boolean isDark() {
        return current.dark;
    }

    /**
     * Switches between light and dark and tells everything that cares.
     *
     * @return true when the theme actually changed
     */
    public static boolean setDark(boolean dark) {
        if (current.dark == dark) {
            return false;
        }
        current = dark ? DARK : LIGHT;
        for (Consumer<Palette> listener : new ArrayList<>(LISTENERS)) {
            listener.accept(current);
        }
        return true;
    }

    public static void toggle() {
        setDark(!current.dark);
    }

    /** Registers a callback run whenever the theme changes. */
    public static void onChange(Consumer<Palette> listener) {
        LISTENERS.add(listener);
    }

    /** Used by the tests to put the theme back. */
    static void resetForTesting() {
        current = LIGHT;
    }
}
