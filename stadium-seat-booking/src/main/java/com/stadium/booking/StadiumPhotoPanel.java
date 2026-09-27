package com.stadium.booking;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * A picture of a stadium.
 *
 * <p>If a real photograph has been supplied, it is used. The application looks
 * for {@code photos/<venue id>.jpg} or {@code .png} in the working directory, so
 * adding a genuine picture later needs no code change: put the file there and
 * restart.
 *
 * <p>When there is no photograph the panel draws the venue instead: an aerial
 * plan of its actual seating bowl, using the real shape, the four real sections
 * and the venue's own accent colour. That is drawn rather than shipped as an
 * image file so it stays sharp at any size and adds nothing to the repository.
 * Real photographs of the venues are third-party images, so they are supplied by
 * whoever runs the application rather than bundled here.
 */
public final class StadiumPhotoPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    /** Where a supplied photograph is looked for. */
    public static final String PHOTO_FOLDER = "photos";
    private static final String[] PHOTO_SUFFIXES = {".jpg", ".jpeg", ".png"};

    private static final int CORNER = 16;

    /** Photographs already loaded, keyed by venue id. */
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();
    /** Venues already searched, so a missing file is not looked for on every repaint. */
    private static final Map<String, Boolean> SEARCHED = new HashMap<>();

    private final transient Stadium stadium;
    private final String pictureNote;

    public StadiumPhotoPanel(Stadium stadium) {
        this.stadium = stadium;
        this.pictureNote = describePicture();
        setOpaque(false);
    }

    /**
     * Where a supplied photograph for this venue would go, so the interface can
     * tell the user exactly which file name to use.
     */
    public static File expectedPhotoFile(Stadium stadium) {
        for (String suffix : PHOTO_SUFFIXES) {
            File candidate = new File(PHOTO_FOLDER,
                    stadium.getId() + suffix.toLowerCase(Locale.ENGLISH));
            if (candidate.isFile()) {
                return candidate;
            }
        }
        return new File(PHOTO_FOLDER, stadium.getId() + ".jpg");
    }

    /** True when a real photograph is being shown rather than the drawn plan. */
    public boolean hasPhotograph() {
        return load(stadium) != null;
    }

    /** A short note about where the picture came from, used as a caption. */
    public String getPictureNote() {
        return pictureNote;
    }

    /** Forgets cached pictures, so newly supplied files are picked up. */
    public static void forgetCachedPhotographs() {
        CACHE.clear();
        SEARCHED.clear();
    }

    private static synchronized BufferedImage load(Stadium stadium) {
        if (stadium == null || SEARCHED.containsKey(stadium.getId())) {
            return stadium == null ? null : CACHE.get(stadium.getId());
        }
        SEARCHED.put(stadium.getId(), Boolean.TRUE);
        for (String suffix : PHOTO_SUFFIXES) {
            File file = new File(PHOTO_FOLDER, stadium.getId() + suffix);
            if (!file.isFile() || file.length() == 0) {
                continue;
            }
            try {
                BufferedImage image = ImageIO.read(file);
                if (image != null) {
                    CACHE.put(stadium.getId(), image);
                    return image;
                }
            } catch (IOException | RuntimeException ignored) {
                // An unreadable or unsupported file simply falls back to the plan.
            }
        }
        return null;
    }

    private String describePicture() {
        if (stadium == null) {
            return "";
        }
        BufferedImage photograph = load(stadium);
        if (photograph != null) {
            return "Photograph  •  " + expectedPhotoFile(stadium).getName();
        }
        return "Aerial seating plan  •  add photos/" + stadium.getId() + ".jpg for a photograph";
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (stadium == null) {
            return;
        }
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            java.awt.Shape card = new RoundRectangle2D.Double(0, 0, width, height, CORNER, CORNER);
            g.setClip(card);
            BufferedImage photograph = load(stadium);
            if (photograph != null) {
                paintPhotograph(g, photograph, width, height);
            } else {
                paintDrawnPlan(g, width, height);
            }
            g.setClip(null);
            paintCaption(g, width, height);
            g.setColor(new Color(203, 213, 225));
            g.setStroke(new BasicStroke(1f));
            g.draw(card);
        } finally {
            g.dispose();
        }
    }

    /** Fills the panel with the photograph, cropping rather than squashing it. */
    private void paintPhotograph(Graphics2D g, BufferedImage image, int width, int height) {
        double scale = Math.max((double) width / image.getWidth(),
                (double) height / image.getHeight());
        int drawWidth = (int) Math.round(image.getWidth() * scale);
        int drawHeight = (int) Math.round(image.getHeight() * scale);
        g.drawImage(image, (width - drawWidth) / 2, (height - drawHeight) / 2,
                drawWidth, drawHeight, null);
        // A wash at the foot so the caption stays readable on any photograph.
        g.setPaint(new GradientPaint(0, height - 84, new Color(2, 6, 23, 0),
                0, height, new Color(2, 6, 23, 190)));
        g.fill(new Rectangle2D.Double(0, height - 84, width, 84));
    }

    /**
     * Draws an aerial plan: the real shape, the four real sections in tier
     * order, seat rows suggested by fine ticks, and the pitch in the middle.
     */
    private void paintDrawnPlan(Graphics2D g, int width, int height) {
        Color accent = colorOf(stadium.getAccentColor(), new Color(220, 38, 38));

        // Ground outside the stadium.
        g.setPaint(new GradientPaint(0, 0, new Color(226, 232, 240),
                0, height, new Color(191, 203, 219)));
        g.fill(new Rectangle2D.Double(0, 0, width, height));
        paintGlow(g, width, height, accent);

        double caption = 44;
        double margin = 14;
        double area = Math.min(width - margin * 2, height - margin * 2 - caption);
        if (area < 60) {
            return;
        }
        double left = (width - area) / 2;
        double top = margin + (height - margin * 2 - caption - area) / 2;

        // A soft drop shadow so the bowl sits on the ground.
        g.setColor(new Color(15, 23, 42, 38));
        g.fill(shadowOf(left, top, area, stadium.getShape()));

        if (stadium.getShape() == StadiumShape.BOX) {
            drawBoxBowl(g, left, top, area, accent);
        } else {
            drawRoundBowl(g, left, top, area, accent, stadium.getShape() == StadiumShape.CIRCULAR);
        }
    }

    private void paintGlow(Graphics2D g, int width, int height, Color accent) {
        float radius = (float) (Math.min(width, height) * 0.55);
        g.setPaint(new java.awt.RadialGradientPaint(
                new java.awt.geom.Point2D.Float(width * 0.5f, height * 0.3f), radius,
                new float[]{0f, 1f},
                new Color[]{new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40),
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0)}));
        g.fill(new Rectangle2D.Double(0, 0, width, height));
    }

    private java.awt.Shape shadowOf(double left, double top, double size, StadiumShape shape) {
        double y = top + 5;
        return shape == StadiumShape.BOX
                ? new RoundRectangle2D.Double(left, y, size, size, 30, 30)
                : new Ellipse2D.Double(left, y, size, size);
    }

    /** The shape the seating bowl takes, before the oval is squashed. */
    private java.awt.Shape outerShape(double left, double top, double size, StadiumShape shape) {
        switch (shape) {
            case BOX:
                return new RoundRectangle2D.Double(left, top, size, size, 26, 26);
            case OVAL:
                return new Ellipse2D.Double(left, top + size * 0.09, size, size * 0.82);
            case CIRCULAR:
            default:
                return new Ellipse2D.Double(left, top, size, size);
        }
    }

    private java.awt.Shape innerShape(double left, double top, double size, StadiumShape shape) {
        double ring = size * 0.17;
        switch (shape) {
            case BOX:
                return new RoundRectangle2D.Double(left + ring, top + ring,
                        size - ring * 2, size - ring * 2, 14, 14);
            case OVAL:
                return new Ellipse2D.Double(left + ring, top + size * 0.09 + ring,
                        size - ring * 2, size * 0.82 - ring * 2);
            case CIRCULAR:
            default:
                return new Ellipse2D.Double(left + ring, top + ring,
                        size - ring * 2, size - ring * 2);
        }
    }

    /** Oval or circular bowl, with the four sections as quarter arcs of the ring. */
    private void drawRoundBowl(Graphics2D g, double left, double top, double size,
                               Color accent, boolean circular) {
        java.awt.Shape outer = outerShape(left, top, size,
                circular ? StadiumShape.CIRCULAR : StadiumShape.OVAL);
        java.awt.Shape inner = innerShape(left, top, size,
                circular ? StadiumShape.CIRCULAR : StadiumShape.OVAL);

        // A is the north stand, then east, south, west, so the picture matches the
        // order the sections are listed in everywhere else. Arc2D measures 0 at
        // three o'clock and runs clockwise, hence the negative start angles.
        java.awt.Shape clip = g.getClip();
        g.clip(outer);
        for (int section = 0; section < 4; section++) {
            java.awt.Shape quarter = new Arc2D.Double(left, top, size, size,
                    -135 + section * 90, 90, Arc2D.PIE);
            java.awt.Shape innerClip = g.getClip();
            g.clip(quarter);
            g.setColor(sectionColor(section, accent));
            g.fill(outer);
            g.setClip(innerClip);
        }
        g.setClip(clip);
        paintRowTicks(g, left, top, size, circular, true);
        paintPitch(g, inner);
        outline(g, outer, inner, accent);
        placeBadges(g, left, top, size, accent, circular);
    }

    /** Box-shaped bowl, with the four sections as straight stands. */
    private void drawBoxBowl(Graphics2D g, double left, double top, double size, Color accent) {
        java.awt.Shape outer = outerShape(left, top, size, StadiumShape.BOX);
        java.awt.Shape inner = innerShape(left, top, size, StadiumShape.BOX);
        double ring = size * 0.17;
        java.awt.Shape clip = g.getClip();
        g.clip(outer);
        g.setColor(sectionColor(0, accent));
        g.fill(new Rectangle2D.Double(left, top, size, ring));
        g.setColor(sectionColor(1, accent));
        g.fill(new Rectangle2D.Double(left + size - ring, top, ring, size));
        g.setColor(sectionColor(2, accent));
        g.fill(new Rectangle2D.Double(left, top + size - ring, size, ring));
        g.setColor(sectionColor(3, accent));
        g.fill(new Rectangle2D.Double(left, top, ring, size));
        g.setClip(clip);

        paintRowTicks(g, left, top, size, false, false);
        paintPitch(g, inner);
        outline(g, outer, inner, accent);

        double cx = left + size / 2;
        double cy = top + size / 2;
        double offset = size / 2 - ring / 2;
        double[][] positions = {
                {cx, cy - offset}, {cx + offset, cy}, {cx, cy + offset}, {cx - offset, cy}};
        for (int section = 0; section < 4; section++) {
            drawSectionBadge(g, section, positions[section][0], positions[section][1], accent);
        }
    }

    /** Fine ticks across each stand, suggesting rows without drawing every seat. */
    private void paintRowTicks(Graphics2D g, double left, double top, double size,
                               boolean circular, boolean arcs) {
        g.setColor(new Color(15, 23, 42, 46));
        g.setStroke(new BasicStroke(0.7f));
        int count = 40;
        if (arcs) {
            for (int band = 1; band < 7; band++) {
                double inset = size * 0.022 * band;
                for (int section = 0; section < 4; section++) {
                    g.draw(new Arc2D.Double(left + inset, top + inset,
                            size - inset * 2, size - inset * 2,
                            -135 + section * 90, 90, Arc2D.OPEN));
                }
            }
        } else {
            double ring = size * 0.17;
            for (int index = 1; index < count; index++) {
                double offset = (size - ring * 2) * index / (double) count;
                g.draw(new Line2D.Double(left + ring, top + ring + offset,
                        left + size - ring, top + ring + offset));
                g.draw(new Line2D.Double(left + ring, top + size - ring - offset,
                        left + size - ring, top + size - ring - offset));
                g.draw(new Line2D.Double(left + ring + offset, top + ring,
                        left + ring + offset, top + size - ring));
                g.draw(new Line2D.Double(left + size - ring - offset, top + ring,
                        left + size - ring - offset, top + size - ring));
            }
        }
    }

    /** The pitch in the middle: grass, then the markings. */
    private void paintPitch(Graphics2D g, java.awt.Shape inner) {
        Rectangle2D bounds = inner.getBounds2D();
        g.setPaint(new GradientPaint(0, (float) bounds.getY(), new Color(34, 120, 62),
                0, (float) bounds.getMaxY(), new Color(22, 92, 47)));
        g.fill(inner);
        g.setClip(inner);
        double x = bounds.getX();
        double y = bounds.getY();
        double w = bounds.getWidth();
        double h = bounds.getHeight();
        // Mown stripes, which is what makes a pitch read as a pitch from above.
        g.setColor(new Color(255, 255, 255, 16));
        int stripes = 8;
        for (int index = 0; index < stripes; index += 2) {
            double band = h / stripes;
            g.fill(new Rectangle2D.Double(x, y + band * index, w, band));
        }
        g.setColor(new Color(255, 255, 255, 96));
        g.setStroke(new BasicStroke(1.1f));
        g.draw(new Ellipse2D.Double(x + w * 0.5 - h * 0.15, y + h * 0.35, h * 0.3, h * 0.3));
        g.draw(new Line2D.Double(x + w * 0.5, y, x + w * 0.5, y + h));
        double box = Math.min(w, h) * 0.28;
        g.draw(new Rectangle2D.Double(x + 1, y + h * 0.21, box, h * 0.58));
        g.draw(new Rectangle2D.Double(x + w - box - 1, y + h * 0.21, box, h * 0.58));
        g.setClip(null);
    }

    private void outline(Graphics2D g, java.awt.Shape outer, java.awt.Shape inner,
                         Color accent) {
        g.setColor(new Color(255, 255, 255, 150));
        g.setStroke(new BasicStroke(1.6f));
        g.draw(outer);
        g.setColor(new Color(15, 23, 42, 190));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(inner);
    }

    /**
     * Section letters on the four compass points, so a badge always sits on the
     * stand it names whatever shape the bowl is.
     */
    private void placeBadges(Graphics2D g, double left, double top, double size,
                             Color accent, boolean circular) {
        double ring = size * 0.17;
        double topEdge = circular ? top : top + size * 0.09;
        double bottomEdge = circular ? top + size : top + size * 0.91;
        double cx = left + size / 2;
        double cy = (topEdge + bottomEdge) / 2;
        double[][] positions = {
                {cx, topEdge + ring / 2},
                {left + size - ring / 2, cy},
                {cx, bottomEdge - ring / 2},
                {left + ring / 2, cy}};
        for (int section = 0; section < 4; section++) {
            drawSectionBadge(g, section, positions[section][0], positions[section][1], accent);
        }
    }

    /** Section letter and seat count, on a pill that reads against any colour. */
    private void drawSectionBadge(Graphics2D g, int section, double x, double y, Color accent) {
        SeatSection model = stadium.getSection(String.valueOf((char) ('A' + section)));
        String letter = model == null ? String.valueOf((char) ('A' + section)) : model.getId();
        int seats = model == null ? 0 : model.getSeatCount();
        Color colour = sectionColor(section, accent);

        double badgeWidth = 46;
        double badgeHeight = 30;
        java.awt.Shape pill = new RoundRectangle2D.Double(
                x - badgeWidth / 2, y - badgeHeight / 2, badgeWidth, badgeHeight, 12, 12);
        g.setColor(new Color(2, 6, 23, 130));
        g.fill(new RoundRectangle2D.Double(x - badgeWidth / 2 + 1, y - badgeHeight / 2 + 1.5,
                badgeWidth, badgeHeight, 12, 12));
        g.setColor(colour);
        g.fill(pill);
        g.setColor(new Color(255, 255, 255, 190));
        g.setStroke(new BasicStroke(1f));
        g.draw(pill);

        g.setColor(readableOn(colour));
        g.setFont(g.getFont().deriveFont(Font.BOLD, 11f));
        java.awt.FontMetrics metrics = g.getFontMetrics();
        g.drawString(letter, (float) (x - metrics.stringWidth(letter) / 2f), (float) (y - 1));
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 8f));
        metrics = g.getFontMetrics();
        String count = compact(seats);
        g.drawString(count, (float) (x - metrics.stringWidth(count) / 2f), (float) (y + 10));
    }

    /**
     * The four section colours, from the venue's own accent at the front to a
     * quiet slate at the back, so the price order is visible in the picture.
     */
    static Color sectionColor(int section, Color accent) {
        float[] towardsSlate = {0f, 0.42f, 0.70f, 0.86f};
        Color slate = new Color(71, 85, 105);
        float mix = towardsSlate[Math.min(section, towardsSlate.length - 1)];
        int red = mix(accent.getRed(), slate.getRed(), mix);
        int green = mix(accent.getGreen(), slate.getGreen(), mix);
        int blue = mix(accent.getBlue(), slate.getBlue(), mix);
        return new Color(red, green, blue);
    }

    private static int mix(int from, int to, float amount) {
        return clamp(Math.round(from + (to - from) * amount));
    }

    /** Black or white, whichever stays readable on the given colour. */
    static Color readableOn(Color background) {
        double luminance = (0.299 * background.getRed() + 0.587 * background.getGreen()
                + 0.114 * background.getBlue()) / 255.0;
        return luminance > 0.55 ? new Color(15, 23, 42) : Color.WHITE;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /** The venue name and where the picture came from, on a strip at the foot. */
    private void paintCaption(Graphics2D g, int width, int height) {
        int strip = 42;
        g.setColor(new Color(2, 6, 23, 196));
        g.fill(new Rectangle2D.Double(0, height - strip, width, strip));
        g.setColor(new Color(148, 163, 184));
        g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
        g.drawString(clip(g, stadium.getName(), width - 24), 12, height - strip + 16);
        g.setColor(new Color(203, 213, 225));
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 9f));
        g.drawString(clip(g, pictureNote, width - 24), 12, height - 6);
    }

    /** Truncates text that would run past the right edge. */
    private static String clip(Graphics2D g, String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (g.getFontMetrics().stringWidth(text) <= maxWidth) {
            return text;
        }
        String trimmed = text;
        while (trimmed.length() > 3
                && g.getFontMetrics().stringWidth(trimmed + "…") > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "…";
    }

    /** Seat counts in the badges, shortened so they fit. */
    static String compact(int value) {
        if (value < 1000) {
            return String.valueOf(value);
        }
        if (value % 1000 == 0) {
            return (value / 1000) + "k";
        }
        return String.format(Locale.ENGLISH, "%.1fk", value / 1000.0);
    }

    /** Parses the venue's own accent colour, falling back if it is not a colour. */
    static Color colorOf(String hex, Color fallback) {
        if (hex == null || hex.isBlank()) {
            return fallback;
        }
        try {
            return new Color(Integer.parseInt(hex.replace("#", ""), 16));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
