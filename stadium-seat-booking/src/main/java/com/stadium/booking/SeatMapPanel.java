package com.stadium.booking;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

/**
 * A scalable, clickable seat map. A custom canvas is used instead of creating
 * tens of thousands of Swing buttons, which keeps large stadium maps responsive.
 */
@SuppressWarnings("serial")
final class SeatMapPanel extends JPanel {
    private static final Color AVAILABLE_BACKGROUND = new Color(232, 241, 255);
    private static final Color AVAILABLE_FOREGROUND = new Color(28, 55, 90);
    private static final Color SELECTED_BACKGROUND = new Color(245, 158, 11);
    private static final Color SELECTED_FOREGROUND = new Color(67, 37, 4);
    private static final Color BOOKED_BACKGROUND = new Color(214, 222, 232);
    private static final Color BOOKED_FOREGROUND = new Color(119, 132, 148);
    private static final Color CLOSED_BACKGROUND = new Color(239, 229, 218);
    private static final Color CLOSED_FOREGROUND = new Color(146, 104, 62);
    private static final Color MAP_BACKGROUND = new Color(248, 251, 255);
    private static final Color MAP_BORDER = new Color(185, 201, 222);
    private static final Color PITCH_GREEN = new Color(222, 242, 231);
    private static final Color SUCCESS_FOREGROUND = new Color(21, 128, 61);

    private static final int CELL_WIDTH = 18;
    private static final int CELL_HEIGHT = 13;
    private static final int CELL_GAP = 2;
    private static final int ROW_LABEL_WIDTH = 46;
    private static final int MAP_TOP = 48;

    private final BookingService bookingService;
    private final Consumer<Seat> seatToggledListener;
    private final Runnable selectionChangedListener;
    private final Consumer<String> messageListener;
    private final Set<SeatKey> selectedSeats = new LinkedHashSet<>();
    private final Set<SeatKey> bookedSeats = new LinkedHashSet<>();
    private final Map<String, SeatCanvas> canvases = new LinkedHashMap<>();
    private final JTabbedPane sectionTabs = new JTabbedPane();
    private final JLabel vacancyValue = new JLabel();
    private final JLabel lastSeatValue = new JLabel("Prices fall from front to back");
    private String renderedStadiumId;
    private String renderedEventId;

    SeatMapPanel(BookingService bookingService,
                 Consumer<Seat> seatToggledListener,
                 Runnable selectionChangedListener,
                 Consumer<String> messageListener) {
        this.bookingService = bookingService;
        this.seatToggledListener = seatToggledListener;
        this.selectionChangedListener = selectionChangedListener;
        this.messageListener = messageListener;

        setLayout(new BorderLayout(0, 8));
        setOpaque(false);
        setMinimumSize(new Dimension(600, 300));
        setPreferredSize(new Dimension(800, 420));
        add(buildMapHeader(), BorderLayout.NORTH);
        add(buildSectionTabs(), BorderLayout.CENTER);
        add(buildLegend(), BorderLayout.SOUTH);
        refreshStatuses();
    }

    private JPanel buildMapHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JPanel copy = new JPanel(new GridBagLayout());
        copy.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        JLabel title = new JLabel("SELECT A SEAT");
        title.setForeground(new Color(30, 64, 110));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 11f));
        copy.add(title, constraints);
        JLabel hint = new JLabel("Front rows are premium • tap a seat to see its number and price");
        hint.setForeground(new Color(100, 116, 139));
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 10f));
        constraints.gridy = 1;
        constraints.insets = new Insets(2, 0, 0, 0);
        copy.add(hint, constraints);

        vacancyValue.setForeground(SUCCESS_FOREGROUND);
        vacancyValue.setFont(vacancyValue.getFont().deriveFont(Font.BOLD, 12f));
        vacancyValue.setHorizontalAlignment(SwingConstants.RIGHT);
        header.add(copy, BorderLayout.CENTER);
        header.add(vacancyValue, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane buildSectionTabs() {
        sectionTabs.setFont(sectionTabs.getFont().deriveFont(Font.BOLD, 11f));
        sectionTabs.setBackground(MAP_BACKGROUND);
        sectionTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        return sectionTabs;
    }

    private JPanel buildLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        legend.setOpaque(false);
        legend.add(legendItem("Vacant", AVAILABLE_BACKGROUND, AVAILABLE_FOREGROUND));
        legend.add(legendItem("Selected", SELECTED_BACKGROUND, SELECTED_FOREGROUND));
        legend.add(legendItem("Booked", BOOKED_BACKGROUND, BOOKED_FOREGROUND));
        legend.add(legendItem("Closed", CLOSED_BACKGROUND, CLOSED_FOREGROUND));
        legend.add(Box.createHorizontalStrut(8));
        lastSeatValue.setForeground(new Color(71, 85, 105));
        lastSeatValue.setFont(lastSeatValue.getFont().deriveFont(Font.PLAIN, 10f));
        legend.add(lastSeatValue);
        return legend;
    }

    private JPanel legendItem(String text, Color background, Color foreground) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        item.setOpaque(false);
        JLabel swatch = new JLabel("  ");
        swatch.setOpaque(true);
        swatch.setBackground(background);
        swatch.setBorder(BorderFactory.createLineBorder(foreground));
        swatch.setPreferredSize(new Dimension(17, 13));
        JLabel label = new JLabel(text);
        label.setForeground(new Color(71, 85, 105));
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 10f));
        item.add(swatch);
        item.add(label);
        return item;
    }

    void refreshStatuses() {
        Stadium stadium = bookingService.getActiveStadium();
        StadiumEvent event = bookingService.getActiveEvent();
        if (stadium == null || event == null) {
            return;
        }

        boolean stadiumChanged = !stadium.getId().equals(renderedStadiumId);
        boolean eventChanged = !event.getId().equals(renderedEventId);
        if (stadiumChanged) {
            rebuildCanvases(stadium);
        }
        if (stadiumChanged || eventChanged) {
            selectedSeats.clear();
        }
        renderedStadiumId = stadium.getId();
        renderedEventId = event.getId();
        bookedSeats.clear();
        bookedSeats.addAll(bookingService.getBookedSeatKeys(event));
        int selectedBeforeRefresh = selectedSeats.size();
        selectedSeats.removeAll(bookedSeats);
        boolean removedSelectedSeat = selectedBeforeRefresh != selectedSeats.size();

        vacancyValue.setText(String.format(Locale.US, "%.3f%% vacant", bookingService.getVacancyPercentage(event)));
        vacancyValue.setForeground(bookingService.getVacancyPercentage(event) <= 10.0
                ? new Color(180, 83, 9) : SUCCESS_FOREGROUND);
        if (selectedSeats.isEmpty()) {
            lastSeatValue.setText(priceGuide());
        }
        for (SeatCanvas canvas : canvases.values()) {
            canvas.refresh();
        }
        if (selectionChangedListener != null && (stadiumChanged || eventChanged || removedSelectedSeat)) {
            selectionChangedListener.run();
        }
    }

    private void rebuildCanvases(Stadium stadium) {
        sectionTabs.removeAll();
        canvases.clear();
        StadiumShape shape = stadium.getShape();
        for (SeatSection section : stadium.getSections()) {
            SeatCanvas canvas = new SeatCanvas(section, shape);
            canvases.put(section.getId(), canvas);
            JScrollPane scroll = new JScrollPane(canvas);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getViewport().setBackground(MAP_BACKGROUND);
            scroll.getVerticalScrollBar().setUnitIncrement(20);
            scroll.getHorizontalScrollBar().setUnitIncrement(20);
            sectionTabs.addTab(section.getId() + "  •  " + section.getLabel(), scroll);
        }
        sectionTabs.setSelectedIndex(0);
        sectionTabs.revalidate();
        sectionTabs.repaint();
    }

    private void handleSeatClick(Seat seat) {
        if (seat == null) {
            return;
        }
        SeatKey key = seat.getKey();
        String status = statusLabel(key);
        lastSeatValue.setText(key.display() + "  •  " + status + "  •  " + currency(seat.getPrice()));
        if (!bookingService.isSeatSelectable(key)) {
            if (messageListener != null) {
                messageListener.accept("Seat " + key.display() + " is " + status.toLowerCase(Locale.ENGLISH));
            }
            return;
        }
        toggleSeat(seat);
    }

    private void toggleSeat(Seat seat) {
        SeatKey key = seat.getKey();
        if (!bookingService.isSeatSelectable(key)) {
            return;
        }
        if (selectedSeats.contains(key)) {
            selectedSeats.remove(key);
            lastSeatValue.setText(key.display() + "  •  Removed  •  " + currency(seat.getPrice()));
        } else {
            if (selectedSeats.size() >= BookingService.MAX_SEATS_PER_BOOKING) {
                if (messageListener != null) {
                    messageListener.accept("Up to " + BookingService.MAX_SEATS_PER_BOOKING
                            + " seats per reservation; booking count is unlimited per person");
                }
                return;
            }
            selectedSeats.add(key);
            lastSeatValue.setText(key.display() + "  •  Selected  •  " + currency(seat.getPrice()));
        }
        for (SeatCanvas canvas : canvases.values()) {
            canvas.repaint();
        }
        if (seatToggledListener != null) {
            seatToggledListener.accept(seat);
        }
        if (selectionChangedListener != null) {
            selectionChangedListener.run();
        }
    }

    private String statusLabel(SeatKey key) {
        if (!bookingService.isBookingOpen(bookingService.getActiveEvent())) {
            return "Booking closed";
        }
        SeatStatus status = bookingService.getStatus(key);
        if (status == SeatStatus.BOOKED) {
            return "Booked";
        }
        if (status == SeatStatus.BLOCKED) {
            return "Unavailable";
        }
        return selectedSeats.contains(key) ? "Selected" : "Vacant";
    }

    List<Seat> getSelectedSeats() {
        List<Seat> seats = new ArrayList<>();
        for (SeatKey key : selectedSeats) {
            Seat seat = bookingService.getSeat(key);
            if (seat != null) {
                seats.add(seat);
            }
        }
        return seats;
    }

    void clearSelection() {
        if (selectedSeats.isEmpty()) {
            return;
        }
        selectedSeats.clear();
        lastSeatValue.setText(priceGuide());
        for (SeatCanvas canvas : canvases.values()) {
            canvas.repaint();
        }
        if (selectionChangedListener != null) {
            selectionChangedListener.run();
        }
    }

    private String priceGuide() {
        Stadium stadium = bookingService.getActiveStadium();
        if (stadium == null || stadium.getSections().isEmpty()) {
            return "Prices fall from front to back";
        }
        double front = Double.MAX_VALUE;
        double back = Double.MAX_VALUE;
        for (SeatSection section : stadium.getSections()) {
            front = Math.min(front, bookingService.getSeatPrice(section, 1));
            back = Math.min(back, bookingService.getSeatPrice(section, section.getRows()));
        }
        return String.format(Locale.US, "Front from %s  •  Back from %s", currency(front), currency(back));
    }

    private String currency(double value) {
        return String.format(Locale.US, "$%.2f", value);
    }

    private final class SeatCanvas extends JPanel {
        private final SeatSection section;
        private final StadiumShape shape;
        private int hoveredRow = -1;
        private int hoveredNumber = -1;

        SeatCanvas(SeatSection section, StadiumShape shape) {
            this.section = section;
            this.shape = shape;
            setOpaque(true);
            setBackground(MAP_BACKGROUND);
            setPreferredSize(new Dimension(ROW_LABEL_WIDTH + section.getSeatsPerRow()
                    * (CELL_WIDTH + CELL_GAP) + 24,
                    MAP_TOP + section.getRows() * (CELL_HEIGHT + CELL_GAP) + 24));
            setCursor(Cursor.getDefaultCursor());
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    SeatCanvas canvas = (SeatCanvas) event.getSource();
                    SeatKey key = canvas.keyAt(event.getX(), event.getY());
                    if (key != null) {
                        canvas.handleClick(key);
                    }
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent event) {
                    SeatCanvas canvas = (SeatCanvas) event.getSource();
                    canvas.hoveredRow = rowAt(event.getY());
                    canvas.hoveredNumber = numberAt(event.getX());
                    canvas.setCursor(hoveredRow > 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            : Cursor.getDefaultCursor());
                    SeatKey key = canvas.keyAt(event.getX(), event.getY());
                    canvas.setToolTipText(key == null ? null : key.display() + "  •  "
                            + statusLabel(key) + "  •  " + currency(bookingService.getSeatPrice(key)));
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    SeatCanvas canvas = (SeatCanvas) event.getSource();
                    canvas.hoveredRow = -1;
                    canvas.hoveredNumber = -1;
                    canvas.setCursor(Cursor.getDefaultCursor());
                    canvas.setToolTipText(null);
                    canvas.repaint();
                }
            });
        }

        private void handleClick(SeatKey key) {
            Seat seat = bookingService.getSeat(key);
            if (seat != null) {
                handleSeatClick(seat);
            }
        }

        private int rowAt(int y) {
            int row = (y - MAP_TOP) / (CELL_HEIGHT + CELL_GAP) + 1;
            return row >= 1 && row <= section.getRows() ? row : -1;
        }

        private int numberAt(int x) {
            int number = (x - ROW_LABEL_WIDTH) / (CELL_WIDTH + CELL_GAP) + 1;
            return number >= 1 && number <= section.getSeatsPerRow() ? number : -1;
        }

        private SeatKey keyAt(int x, int y) {
            int row = rowAt(y);
            int number = numberAt(x);
            if (row < 1 || number < 1) {
                return null;
            }
            int cellX = ROW_LABEL_WIDTH + (number - 1) * (CELL_WIDTH + CELL_GAP);
            int cellY = MAP_TOP + (row - 1) * (CELL_HEIGHT + CELL_GAP);
            if (x < cellX || x > cellX + CELL_WIDTH || y < cellY || y > cellY + CELL_HEIGHT) {
                return null;
            }
            return new SeatKey(section.getId(), row, number);
        }

        private void refresh() {
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            drawOutline(g, width, height);
            drawPitch(g, width);

            g.setColor(new Color(100, 116, 139));
            g.setFont(getFont().deriveFont(Font.BOLD, 10f));
            g.drawString(section.getId() + "  " + section.getLabel(), 14, 19);
            g.setFont(getFont().deriveFont(Font.PLAIN, 9f));
            g.drawString("FRONT ROWS • PREMIUM", 14, 35);
            String middleTier = "MIDDLE ROWS • STANDARD";
            int middleWidth = g.getFontMetrics().stringWidth(middleTier);
            g.drawString(middleTier, Math.max(160, (width - middleWidth) / 2), 35);
            g.drawString("BACK ROWS • VALUE", Math.max(150, width - 145), 35);

            boolean closed = !bookingService.isBookingOpen(bookingService.getActiveEvent());
            for (int row = 1; row <= section.getRows(); row++) {
                int y = MAP_TOP + (row - 1) * (CELL_HEIGHT + CELL_GAP);
                if (row == 1 || row % 5 == 0 || row == section.getRows()) {
                    g.setColor(new Color(100, 116, 139));
                    g.setFont(getFont().deriveFont(Font.PLAIN, 8f));
                    g.drawString("Row " + row, 8, y + 9);
                }
                for (int number = 1; number <= section.getSeatsPerRow(); number++) {
                    int x = ROW_LABEL_WIDTH + (number - 1) * (CELL_WIDTH + CELL_GAP);
                    paintSeat(g, new SeatKey(section.getId(), row, number), x, y, closed);
                }
            }
            g.dispose();
        }

        private void drawOutline(Graphics2D g, int width, int height) {
            g.setColor(MAP_BORDER);
            g.setStroke(new BasicStroke(2f));
            if (shape == StadiumShape.CIRCULAR) {
                g.draw(new Ellipse2D.Double(8, 6, Math.max(20, width - 16), Math.max(20, height - 12)));
            } else if (shape == StadiumShape.OVAL) {
                g.draw(new RoundRectangle2D.Double(8, 6, Math.max(20, width - 16),
                        Math.max(20, height - 12), 80, 80));
            } else {
                g.drawRect(8, 6, Math.max(20, width - 17), Math.max(20, height - 13));
            }
        }

        private void drawPitch(Graphics2D g, int width) {
            int pitchWidth = Math.min(360, Math.max(190, width / 3));
            int pitchX = Math.max(65, (width - pitchWidth) / 2);
            g.setColor(PITCH_GREEN);
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Double(pitchX, 7, pitchWidth, 26, 12, 12));
            g.setColor(new Color(31, 96, 57));
            g.setFont(getFont().deriveFont(Font.BOLD, 9f));
            String pitchText = shape == StadiumShape.CIRCULAR ? "CIRCULAR PITCH" : "PITCH / STAGE";
            int textWidth = g.getFontMetrics().stringWidth(pitchText);
            g.drawString(pitchText, pitchX + (pitchWidth - textWidth) / 2, 24);
        }

        private void paintSeat(Graphics2D g, SeatKey key, int x, int y, boolean closed) {
            boolean booked = bookedSeats.contains(key);
            boolean selected = selectedSeats.contains(key);
            Color background;
            Color foreground;
            if (booked) {
                background = BOOKED_BACKGROUND;
                foreground = BOOKED_FOREGROUND;
            } else if (selected) {
                background = SELECTED_BACKGROUND;
                foreground = SELECTED_FOREGROUND;
            } else if (closed) {
                background = CLOSED_BACKGROUND;
                foreground = CLOSED_FOREGROUND;
            } else {
                background = AVAILABLE_BACKGROUND;
                foreground = AVAILABLE_FOREGROUND;
            }
            Rectangle rectangle = new Rectangle(x, y, CELL_WIDTH, CELL_HEIGHT);
            g.setColor(background);
            g.fillRoundRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height, 4, 4);
            g.setColor(selected ? SELECTED_FOREGROUND : new Color(166, 181, 201));
            g.setStroke(new BasicStroke(selected ? 1.5f : 1f));
            g.drawRoundRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height, 4, 4);
            if (key.getNumber() == 1 || key.getNumber() % 5 == 0 || key.getNumber() == section.getSeatsPerRow()) {
                g.setColor(foreground);
                g.setFont(getFont().deriveFont(Font.PLAIN, 7f));
                String number = String.valueOf(key.getNumber());
                int textWidth = g.getFontMetrics().stringWidth(number);
                g.drawString(number, x + (CELL_WIDTH - textWidth) / 2, y + 9);
            }
            if (hoveredRow == key.getRow() && hoveredNumber == key.getNumber()) {
                g.setColor(new Color(37, 99, 235, 130));
                g.drawRoundRect(x - 1, y - 1, CELL_WIDTH + 2, CELL_HEIGHT + 2, 5, 5);
            }
        }
    }
}
