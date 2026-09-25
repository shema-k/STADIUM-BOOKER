package com.stadium.booking;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * Professional desktop venue directory and stadium seat-booking application
 * built entirely with Java Swing.
 *
 * <p>The user flow is deliberately linear: choose a stadium, choose a date,
 * choose a game or concert, choose seats, and confirm the booking.</p>
 */
@SuppressWarnings("serial")
public final class StadiumBookingApp extends JFrame {
    private static final Color NAVY = new Color(15, 35, 67);
    private static final Color NAVY_SOFT = new Color(30, 57, 97);
    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color BLUE_DARK = new Color(29, 78, 216);
    private static final Color PURPLE = new Color(124, 58, 237);
    private static final Color TEAL = new Color(15, 118, 110);
    private static final Color SKY = new Color(239, 246, 255);
    private static final Color PAGE = new Color(244, 247, 251);
    private static final Color WHITE = Color.WHITE;
    private static final Color TEXT = new Color(30, 41, 59);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color BORDER = new Color(219, 228, 239);
    private static final Color SUCCESS_DARK = new Color(21, 128, 61);
    private static final Color SUCCESS_FOREGROUND = new Color(21, 128, 61);
    private static final Color WARNING_FOREGROUND = new Color(180, 83, 9);
    private static final Color CANCELLED = new Color(120, 130, 143);

    private static final DateTimeFormatter CREATED_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private final BookingService bookingService;
    private final SeatMapPanel seatMapPanel;
    private final JPanel contentHost = new JPanel(new BorderLayout());
    private final JLabel headerTitle = new JLabel();
    private final JLabel headerSubtitle = new JLabel();
    private final JLabel statusValue = new JLabel("Choose a stadium to begin");
    private final SearchField stadiumSearchField =
            new SearchField("Search stadium, city, team or artist");
    private final SearchField eventSearchField =
            new SearchField("Search teams, artists, sport or date");
    private final SearchField bookingSearchField =
            new SearchField("Search reference, stadium, event or status");
    private final JTextField nameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JLabel selectedSeatsValue = new JLabel("No seats selected");
    private final JLabel totalValue = new JLabel("$0.00");
    private final JLabel bookingAvailabilityValue = new JLabel();
    private final JLabel bookingCountdownValue = new JLabel();
    private JPanel pricingBreakdownHost;
    private final Map<JLabel, StadiumEvent> countdownLabels = new LinkedHashMap<>();
    private final Map<JButton, StadiumEvent> eventActionButtons = new LinkedHashMap<>();
    private final DefaultTableModel bookingTableModel;
    private final JTable bookingTable;
    private final JButton stadiumNavButton = new JButton("Stadiums");
    private final JButton bookingsNavButton = new JButton("My bookings");

    private Stadium selectedStadium;
    private StadiumEvent selectedEvent;
    private LocalDate selectedScheduleDate;
    private List<LocalDate> scheduleDates = new ArrayList<>();
    private JPanel scheduleListHost;
    private JLabel scheduleResultLabel;
    private JComboBox<String> dateCombo;
    private JButton clearSelectionButton;
    private JButton confirmBookingButton;
    private JButton cancelBookingButton;
    private String currentScreen = "directory";
    private final Timer directorySearchTimer = new Timer(140, event -> {
        if ("directory".equals(currentScreen)) {
            refreshDirectoryContent();
        }
    });
    private final Timer countdownTimer = new Timer(60_000, event -> updateCountdownDisplays());

    public StadiumBookingApp() {
        super("Stadium Select");
        directorySearchTimer.setRepeats(false);
        countdownTimer.setRepeats(true);
        bookingService = new BookingService();
        seatMapPanel = new SeatMapPanel(bookingService, this::onSeatToggled,
                this::updateBookingSummary, this::showStatus);
        bookingTableModel = new DefaultTableModel(
                new Object[]{"Reference", "Stadium", "Event", "When", "Seats", "Total", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bookingTable = new JTable(bookingTableModel);
        bookingTable.getSelectionModel().addListSelectionListener(event -> updateCancelButton());

        configureWindow();
        installSearchListeners();
        buildShell();
        countdownTimer.start();
        showStadiumDirectory();
    }

    private void configureWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 740));
        setSize(1320, 900);
        setLocationRelativeTo(null);
        getContentPane().setBackground(PAGE);
    }

    private void installSearchListeners() {
        addDocumentListener(stadiumSearchField, this::refreshDirectoryIfVisible);
        addDocumentListener(eventSearchField, this::refreshScheduleIfVisible);
        addDocumentListener(bookingSearchField, this::refreshBookingsIfVisible);
    }

    private void addDocumentListener(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                action.run();
            }
        });
    }

    private void buildShell() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(PAGE);
        root.add(buildHeader(), BorderLayout.NORTH);
        contentHost.setBackground(PAGE);
        contentHost.setBorder(new EmptyBorder(0, 24, 0, 24));
        root.add(contentHost, BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(49, 73, 108)),
                new EmptyBorder(18, 28, 18, 28)));

        JPanel titleBlock = new JPanel(new GridBagLayout());
        titleBlock.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        JLabel brand = new JLabel("STADIUM SELECT  /  LIVE EVENT TICKETS");
        brand.setForeground(new Color(147, 197, 253));
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 10f));
        titleBlock.add(brand, constraints);

        headerTitle.setForeground(WHITE);
        headerTitle.setFont(headerTitle.getFont().deriveFont(Font.BOLD, 23f));
        constraints.gridy = 1;
        constraints.insets = new Insets(3, 0, 0, 0);
        titleBlock.add(headerTitle, constraints);

        headerSubtitle.setForeground(new Color(190, 207, 232));
        headerSubtitle.setFont(headerSubtitle.getFont().deriveFont(Font.PLAIN, 12f));
        constraints.gridy = 2;
        constraints.insets = new Insets(3, 0, 0, 0);
        titleBlock.add(headerSubtitle, constraints);

        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 17));
        navigation.setOpaque(false);
        styleHeaderButton(stadiumNavButton);
        styleHeaderButton(bookingsNavButton);
        stadiumNavButton.addActionListener(event -> showStadiumDirectory());
        bookingsNavButton.addActionListener(event -> showBookings());
        navigation.add(stadiumNavButton);
        navigation.add(bookingsNavButton);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(navigation, BorderLayout.EAST);
        return header;
    }

    private void styleHeaderButton(JButton button) {
        button.setFont(button.getFont().deriveFont(Font.BOLD, 11f));
        button.setForeground(new Color(219, 234, 254));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(76, 112, 164)),
                new EmptyBorder(8, 12, 8, 12)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private JPanel buildStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(WHITE);
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                new EmptyBorder(7, 24, 7, 24)));
        JLabel label = new JLabel("STATUS");
        label.setForeground(MUTED);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        statusValue.setForeground(SUCCESS_DARK);
        statusValue.setFont(statusValue.getFont().deriveFont(Font.PLAIN, 11f));
        statusValue.setBorder(new EmptyBorder(0, 14, 0, 0));
        status.add(label, BorderLayout.WEST);
        status.add(statusValue, BorderLayout.CENTER);
        return status;
    }

    private void setHeader(String title, String subtitle) {
        headerTitle.setText(title);
        headerSubtitle.setText(subtitle);
    }

    // ---------------------------------------------------------------------
    // Stadium directory
    // ---------------------------------------------------------------------

    private void showStadiumDirectory() {
        currentScreen = "directory";
        selectedStadium = null;
        selectedEvent = null;
        setHeader("Choose your stadium", "Start with the venue, then choose the date and event you want to attend.");
        refreshDirectoryContent();
    }

    private void refreshDirectoryIfVisible() {
        if ("directory".equals(currentScreen)) {
            directorySearchTimer.restart();
        }
    }

    private void refreshDirectoryContent() {
        boolean restoreFocus = stadiumSearchField.isFocusOwner();
        int caretPosition = stadiumSearchField.getCaretPosition();
        contentHost.removeAll();
        contentHost.add(buildDirectoryContent(), BorderLayout.CENTER);
        contentHost.revalidate();
        contentHost.repaint();
        if (restoreFocus) {
            SwingUtilities.invokeLater(() -> {
                stadiumSearchField.requestFocusInWindow();
                stadiumSearchField.setCaretPosition(Math.min(caretPosition, stadiumSearchField.getText().length()));
            });
        }
    }

    private JPanel buildDirectoryContent() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(PAGE);
        page.setBorder(new EmptyBorder(22, 0, 22, 0));
        JPanel topStack = new JPanel();
        topStack.setOpaque(false);
        topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
        topStack.add(buildDirectoryHero());
        topStack.add(Box.createVerticalStrut(14));

        JPanel searchCard = createCard();
        searchCard.setLayout(new BorderLayout(14, 0));
        searchCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(14, 16, 14, 16)));
        JLabel searchLabel = new JLabel("FIND A VENUE");
        searchLabel.setForeground(MUTED);
        searchLabel.setFont(searchLabel.getFont().deriveFont(Font.BOLD, 10f));
        searchLabel.setPreferredSize(new Dimension(105, 38));
        searchCard.add(searchLabel, BorderLayout.WEST);
        searchCard.add(buildSearchBar(stadiumSearchField, "Search stadium, city, team or artist"), BorderLayout.CENTER);
        topStack.add(searchCard);
        page.add(topStack, BorderLayout.NORTH);

        String query = stadiumSearchField.getText().trim().toLowerCase(Locale.ENGLISH);
        List<Stadium> matches = new ArrayList<>();
        for (Stadium stadium : bookingService.getStadiums()) {
            boolean eventMatch = bookingService.getEvents(stadium.getId()).stream()
                    .anyMatch(event -> event.searchableText().contains(query));
            if (query.isEmpty() || stadium.searchableText().contains(query) || eventMatch) {
                matches.add(stadium);
            }
        }

        JPanel listPanel = new JPanel(new BorderLayout(8, 0));
        listPanel.setOpaque(false);
        JLabel resultLabel = new JLabel(matches.size() + " venue" + (matches.size() == 1 ? "" : "s")
                + (query.isEmpty() ? " available" : " matching your search"));
        resultLabel.setForeground(MUTED);
        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.PLAIN, 11f));
        listPanel.add(resultLabel, BorderLayout.NORTH);

        JPanel cards = new JPanel();
        cards.setOpaque(false);
        cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
        cards.setBorder(new EmptyBorder(0, 0, 4, 0));
        if (matches.isEmpty()) {
            JPanel empty = buildEmptyState("No stadiums found", "Try a different city, country or venue name.");
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
            cards.add(empty);
        } else {
            for (int start = 0; start < matches.size(); start += 2) {
                JPanel row = new JPanel(new GridBagLayout());
                row.setOpaque(false);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 192));
                for (int column = 0; column < 2 && start + column < matches.size(); column++) {
                    GridBagConstraints cardConstraints = new GridBagConstraints();
                    cardConstraints.gridx = column;
                    cardConstraints.weightx = 1.0;
                    cardConstraints.weighty = 0.0;
                    cardConstraints.fill = GridBagConstraints.HORIZONTAL;
                    cardConstraints.insets = new Insets(0, column == 0 ? 0 : 7, 0, column == 0 ? 7 : 0);
                    row.add(createStadiumCard(matches.get(start + column)), cardConstraints);
                }
                cards.add(row);
                cards.add(Box.createVerticalStrut(14));
            }
        }
        JScrollPane scroll = new JScrollPane(cards);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(PAGE);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        listPanel.add(scroll, BorderLayout.CENTER);
        page.add(listPanel, BorderLayout.CENTER);
        page.add(createBookingSteps(), BorderLayout.SOUTH);
        return page;
    }

    private JPanel buildDirectoryHero() {
        JPanel hero = createCard();
        hero.setLayout(new BorderLayout(18, 0));
        hero.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(20, 22, 20, 22)));
        JPanel copy = new JPanel(new GridBagLayout());
        copy.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        JLabel eyebrow = new JLabel("VENUE DIRECTORY");
        eyebrow.setForeground(BLUE_DARK);
        eyebrow.setFont(eyebrow.getFont().deriveFont(Font.BOLD, 10f));
        copy.add(eyebrow, constraints);
        JLabel title = new JLabel("Find your next live event");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        constraints.gridy = 1;
        constraints.insets = new Insets(5, 0, 0, 0);
        copy.add(title, constraints);
        JLabel subtitle = new JLabel("Browse stadiums, check what is on, and reserve the right seats in a few clear steps.");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 12f));
        constraints.gridy = 2;
        constraints.insets = new Insets(4, 0, 0, 0);
        copy.add(subtitle, constraints);

        JLabel directoryBadge = new JLabel("  LIVE SCHEDULES  ");
        directoryBadge.setOpaque(true);
        directoryBadge.setBackground(new Color(219, 234, 254));
        directoryBadge.setForeground(BLUE_DARK);
        directoryBadge.setFont(directoryBadge.getFont().deriveFont(Font.BOLD, 10f));
        directoryBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(191, 219, 254)), new EmptyBorder(9, 4, 9, 4)));
        JPanel badgeHolder = new JPanel(new GridBagLayout());
        badgeHolder.setOpaque(false);
        badgeHolder.add(directoryBadge);
        hero.add(copy, BorderLayout.CENTER);
        hero.add(badgeHolder, BorderLayout.EAST);
        return hero;
    }

    private JPanel createStadiumCard(Stadium stadium) {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(16, 12));
        card.setPreferredSize(new Dimension(520, 192));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(18, 18, 16, 18)));
        Color accent = colorFromHex(stadium.getAccentColor(), BLUE);

        JPanel titleRow = new JPanel(new BorderLayout(12, 0));
        titleRow.setOpaque(false);
        JLabel initials = new JLabel(stadium.initials(), SwingConstants.CENTER);
        initials.setOpaque(true);
        initials.setBackground(accent);
        initials.setForeground(WHITE);
        initials.setFont(initials.getFont().deriveFont(Font.BOLD, 16f));
        initials.setPreferredSize(new Dimension(52, 52));
        JPanel nameBlock = new JPanel(new GridBagLayout());
        nameBlock.setOpaque(false);
        GridBagConstraints nameConstraints = new GridBagConstraints();
        nameConstraints.anchor = GridBagConstraints.WEST;
        JLabel name = new JLabel(stadium.getName());
        name.setForeground(TEXT);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 16f));
        nameBlock.add(name, nameConstraints);
        JLabel location = new JLabel(stadium.getLocation());
        location.setForeground(MUTED);
        location.setFont(location.getFont().deriveFont(Font.PLAIN, 11f));
        nameConstraints.gridy = 1;
        nameConstraints.insets = new Insets(3, 0, 0, 0);
        nameBlock.add(location, nameConstraints);
        titleRow.add(initials, BorderLayout.WEST);
        titleRow.add(nameBlock, BorderLayout.CENTER);

        JLabel type = new JLabel(stadium.getVenueType().toUpperCase(Locale.ENGLISH));
        type.setForeground(accent.darker());
        type.setFont(type.getFont().deriveFont(Font.BOLD, 9f));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titleRow, BorderLayout.CENTER);
        top.add(type, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        JLabel description = new JLabel("<html><div style='width:360px'>" + stadium.getDescription() + "</div></html>");
        description.setForeground(MUTED);
        description.setFont(description.getFont().deriveFont(Font.PLAIN, 11f));
        card.add(description, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        List<StadiumEvent> events = bookingService.getEvents(stadium.getId());
        StadiumEvent next = events.isEmpty() ? null : events.get(0);
        JLabel facts = new JLabel(formatCapacity(stadium.getCapacity()) + " capacity   •   "
                + (next == null ? "No events listed" : next.getDateLabel()));
        facts.setForeground(TEXT);
        facts.setFont(facts.getFont().deriveFont(Font.PLAIN, 10f));
        JButton open = createOutlineButton("Open stadium", accent);
        open.addActionListener(event -> openStadium(stadium));
        footer.add(facts, BorderLayout.CENTER);
        footer.add(open, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createBookingSteps() {
        JPanel steps = createCard();
        steps.setLayout(new BorderLayout(20, 0));
        steps.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(14, 18, 14, 18)));
        JLabel heading = new JLabel("HOW IT WORKS");
        heading.setForeground(MUTED);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 10f));
        steps.add(heading, BorderLayout.WEST);
        JPanel row = new JPanel(new GridLayout(1, 3, 18, 0));
        row.setOpaque(false);
        row.add(createStep("1", "Choose a stadium"));
        row.add(createStep("2", "Pick a date and event"));
        row.add(createStep("3", "Select seats and confirm"));
        steps.add(row, BorderLayout.CENTER);
        return steps;
    }

    private JPanel createStep(String number, String text) {
        JPanel step = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        step.setOpaque(false);
        JLabel circle = new JLabel(number, SwingConstants.CENTER);
        circle.setOpaque(true);
        circle.setBackground(SKY);
        circle.setForeground(BLUE_DARK);
        circle.setFont(circle.getFont().deriveFont(Font.BOLD, 11f));
        circle.setPreferredSize(new Dimension(25, 25));
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        step.add(circle);
        step.add(label);
        return step;
    }

    private void openStadium(Stadium stadium) {
        selectedStadium = stadium;
        selectedEvent = null;
        selectedScheduleDate = null;
        scheduleListHost = null;
        scheduleResultLabel = null;
        pricingBreakdownHost = null;
        currentScreen = "directory";
        eventSearchField.setText("");
        currentScreen = "stadium";
        setHeader(stadium.getName(), stadium.getLocation() + "  •  " + stadium.getVenueType());
        contentHost.removeAll();
        contentHost.add(buildStadiumDashboard(stadium), BorderLayout.CENTER);
        contentHost.revalidate();
        contentHost.repaint();
        showStatus("Viewing the " + stadium.getName() + " schedule");
    }

    // ---------------------------------------------------------------------
    // Stadium schedule
    // ---------------------------------------------------------------------

    private JPanel buildStadiumDashboard(Stadium stadium) {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setBackground(PAGE);
        page.setBorder(new EmptyBorder(20, 0, 22, 0));
        page.add(buildStadiumHero(stadium), BorderLayout.NORTH);

        JPanel dashboardBody = new JPanel(new BorderLayout(14, 0));
        dashboardBody.setOpaque(false);
        dashboardBody.add(buildScheduleCard(stadium), BorderLayout.CENTER);
        JPanel notices = buildAnnouncementsCard(stadium);
        notices.setPreferredSize(new Dimension(410, 100));
        notices.setMinimumSize(new Dimension(360, 100));
        dashboardBody.add(notices, BorderLayout.EAST);
        page.add(dashboardBody, BorderLayout.CENTER);
        page.add(createBookingSteps(), BorderLayout.SOUTH);
        return page;
    }

    private JPanel buildStadiumHero(Stadium stadium) {
        Color accent = colorFromHex(stadium.getAccentColor(), BLUE);
        JPanel hero = createCard();
        hero.setLayout(new BorderLayout(18, 0));
        hero.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(17, 20, 17, 20)));

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        JButton back = createOutlineButton("← All stadiums", accent);
        back.addActionListener(event -> showStadiumDirectory());
        top.add(back, BorderLayout.WEST);
        JLabel venueType = new JLabel(stadium.getVenueType().toUpperCase(Locale.ENGLISH));
        venueType.setForeground(accent.darker());
        venueType.setFont(venueType.getFont().deriveFont(Font.BOLD, 10f));
        top.add(venueType, BorderLayout.EAST);
        hero.add(top, BorderLayout.NORTH);

        JPanel copy = new JPanel(new GridBagLayout());
        copy.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        JLabel title = new JLabel(stadium.getName());
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        copy.add(title, constraints);
        JLabel description = new JLabel(stadium.getDescription());
        description.setForeground(MUTED);
        description.setFont(description.getFont().deriveFont(Font.PLAIN, 12f));
        constraints.gridy = 1;
        constraints.insets = new Insets(5, 0, 0, 0);
        copy.add(description, constraints);
        JLabel address = new JLabel(stadium.getAddress());
        address.setForeground(MUTED);
        address.setFont(address.getFont().deriveFont(Font.PLAIN, 10f));
        constraints.gridy = 2;
        constraints.insets = new Insets(4, 0, 0, 0);
        copy.add(address, constraints);
        hero.add(copy, BorderLayout.CENTER);

        JPanel stats = new JPanel(new GridLayout(1, 3, 10, 0));
        stats.setOpaque(false);
        List<StadiumEvent> events = bookingService.getEvents(stadium.getId());
        StadiumEvent next = events.isEmpty() ? null : events.get(0);
        stats.add(createStat("LOCATION", stadium.getLocation()));
        stats.add(createStat("CAPACITY", formatCapacity(stadium.getCapacity())));
        stats.add(createStat("NEXT EVENT", next == null ? "—" : next.getDateLabel()));
        hero.add(stats, BorderLayout.EAST);
        return hero;
    }

    private JPanel createStat(String label, String value) {
        JPanel stat = new JPanel(new GridBagLayout());
        stat.setOpaque(false);
        stat.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(224, 232, 242)),
                new EmptyBorder(0, 16, 0, 0)));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        JLabel caption = new JLabel(label);
        caption.setForeground(MUTED);
        caption.setFont(caption.getFont().deriveFont(Font.BOLD, 9f));
        stat.add(caption, constraints);
        JLabel content = new JLabel(value);
        content.setForeground(TEXT);
        content.setFont(content.getFont().deriveFont(Font.BOLD, 12f));
        constraints.gridy = 1;
        constraints.insets = new Insets(4, 0, 0, 0);
        stat.add(content, constraints);
        return stat;
    }

    private JPanel buildAnnouncementsCard(Stadium stadium) {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(12, 16, 12, 16)));

        JPanel heading = new JPanel(new BorderLayout(10, 0));
        heading.setOpaque(false);
        JPanel titleBox = new JPanel(new GridBagLayout());
        titleBox.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        JLabel title = new JLabel("Notices & requests");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        titleBox.add(title, constraints);
        JLabel subtitle = new JLabel("Cancellations and emergency updates");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 10f));
        constraints.gridy = 1;
        constraints.insets = new Insets(3, 0, 0, 0);
        titleBox.add(subtitle, constraints);
        heading.add(titleBox, BorderLayout.CENTER);
        JButton request = createOutlineButton("Submit a special request", BLUE);
        request.addActionListener(event -> showSpecialRequestDialog(stadium));
        heading.add(request, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        List<StadiumAnnouncement> announcements = bookingService.getAnnouncements(stadium.getId());
        int visibleCount = 0;
        for (StadiumAnnouncement announcement : announcements) {
            if (!announcement.isActive()) {
                continue;
            }
            list.add(createAnnouncementRow(announcement));
            list.add(Box.createVerticalStrut(6));
            visibleCount++;
            if (visibleCount == 3) {
                break;
            }
        }
        if (visibleCount == 0) {
            JLabel empty = new JLabel("No active notices. Special requests can still be sent to the venue team.");
            empty.setForeground(MUTED);
            empty.setFont(empty.getFont().deriveFont(Font.PLAIN, 10f));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            list.add(empty);
        } else if (announcements.size() > visibleCount) {
            JLabel more = new JLabel((announcements.size() - visibleCount) + " additional notice(s) available at the venue desk.");
            more.setForeground(MUTED);
            more.setFont(more.getFont().deriveFont(Font.PLAIN, 9f));
            more.setAlignmentX(Component.LEFT_ALIGNMENT);
            list.add(more);
        }
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private JPanel createAnnouncementRow(StadiumAnnouncement announcement) {
        Color accent = announcementColor(announcement.getType());
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent), new EmptyBorder(2, 9, 2, 4)));

        JPanel copy = new JPanel(new GridBagLayout());
        copy.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        JLabel type = new JLabel(announcement.getType().getLabel().toUpperCase(Locale.ENGLISH));
        type.setForeground(accent.darker());
        type.setFont(type.getFont().deriveFont(Font.BOLD, 8f));
        copy.add(type, constraints);
        JLabel title = new JLabel("<html><div style='width:225px'>" + htmlText(announcement.getTitle()) + "</div></html>");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 11f));
        constraints.gridx = 1;
        constraints.insets = new Insets(0, 8, 0, 0);
        copy.add(title, constraints);
        JLabel message = new JLabel("<html><div style='width:250px'>" + htmlText(announcement.getMessage()) + "</div></html>");
        message.setForeground(MUTED);
        message.setFont(message.getFont().deriveFont(Font.PLAIN, 9f));
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(2, 0, 0, 0);
        copy.add(message, constraints);
        row.add(copy, BorderLayout.CENTER);

        StadiumEvent event = announcement.getEventId().isEmpty() ? null
                : StadiumData.getEvent(announcement.getEventId());
        if (event != null) {
            JLabel eventLabel = new JLabel(event.getDateLabel() + "  •  " + event.getTimeLabel());
            eventLabel.setForeground(accent.darker());
            eventLabel.setFont(eventLabel.getFont().deriveFont(Font.BOLD, 9f));
            row.add(eventLabel, BorderLayout.EAST);
        }
        return row;
    }

    private Color announcementColor(AnnouncementType type) {
        if (type == AnnouncementType.CANCELLATION) {
            return new Color(185, 28, 28);
        }
        if (type == AnnouncementType.EMERGENCY) {
            return new Color(180, 83, 9);
        }
        if (type == AnnouncementType.SPECIAL_REQUEST) {
            return PURPLE;
        }
        return BLUE;
    }

    private void showSpecialRequestDialog(Stadium stadium) {
        JComboBox<String> category = new JComboBox<>(new String[]{
                "Event cancellation", "Stadium emergency", "Safety notice",
                "Accessibility request", "Other request"});
        JTextArea message = new JTextArea(5, 42);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setFont(message.getFont().deriveFont(Font.PLAIN, 12f));
        message.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(8, 8, 8, 8));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(4, 4, 4, 4);
        JLabel categoryLabel = new JLabel("Request type");
        categoryLabel.setFont(categoryLabel.getFont().deriveFont(Font.BOLD, 11f));
        form.add(categoryLabel, constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        form.add(category, constraints);
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        JLabel messageLabel = new JLabel("Details");
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.BOLD, 11f));
        form.add(messageLabel, constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        form.add(message, constraints);

        int result = JOptionPane.showConfirmDialog(this, form,
                "Submit a special request to " + stadium.getName(),
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            bookingService.addSpecialRequest(stadium.getId(), String.valueOf(category.getSelectedItem()),
                    message.getText());
            openStadium(stadium);
            showStatus("Special request submitted to " + stadium.getName());
        } catch (IllegalArgumentException exception) {
            showWarning(exception.getMessage());
        }
    }

    private JPanel buildScheduleCard(Stadium stadium) {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(16, 18, 16, 18)));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel headingCopy = new JPanel(new GridBagLayout());
        headingCopy.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        JLabel title = new JLabel("Upcoming schedule");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
        headingCopy.add(title, constraints);
        JLabel subtitle = new JLabel("Choose a date, then select the game or concert you want to attend.");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 11f));
        constraints.gridy = 1;
        constraints.insets = new Insets(3, 0, 0, 0);
        headingCopy.add(subtitle, constraints);
        heading.add(headingCopy, BorderLayout.WEST);
        scheduleResultLabel = new JLabel();
        scheduleResultLabel.setForeground(BLUE_DARK);
        scheduleResultLabel.setFont(scheduleResultLabel.getFont().deriveFont(Font.BOLD, 11f));
        heading.add(scheduleResultLabel, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);

        JPanel filters = new JPanel(new BorderLayout(12, 0));
        filters.setOpaque(false);
        filters.add(buildSearchBar(eventSearchField, "Search teams, artists, sport or date"), BorderLayout.CENTER);
        scheduleDates = new ArrayList<>(StadiumData.getDates(stadium.getId()));
        String[] dateChoices = new String[scheduleDates.size() + 1];
        dateChoices[0] = "All dates";
        for (int index = 0; index < scheduleDates.size(); index++) {
            LocalDate date = scheduleDates.get(index);
            int eventCount = 0;
            for (StadiumEvent event : bookingService.getEvents(stadium.getId())) {
                if (date.equals(event.getDate())) {
                    eventCount++;
                }
            }
            dateChoices[index + 1] = date.format(DATE_FORMATTER) + "  •  "
                    + eventCount + (eventCount == 1 ? " event" : " events");
        }
        dateCombo = new JComboBox<>(dateChoices);
        dateCombo.setFont(dateCombo.getFont().deriveFont(Font.PLAIN, 12f));
        dateCombo.setPreferredSize(new Dimension(280, 38));
        dateCombo.setBackground(WHITE);
        dateCombo.addActionListener(event -> {
            int index = dateCombo.getSelectedIndex();
            selectedScheduleDate = index <= 0 ? null : scheduleDates.get(index - 1);
            refreshScheduleList();
        });
        filters.add(dateCombo, BorderLayout.EAST);

        JPanel centerStack = new JPanel(new BorderLayout(0, 10));
        centerStack.setOpaque(false);
        centerStack.add(filters, BorderLayout.NORTH);
        scheduleListHost = new JPanel(new BorderLayout());
        scheduleListHost.setOpaque(false);
        centerStack.add(scheduleListHost, BorderLayout.CENTER);
        card.add(centerStack, BorderLayout.CENTER);
        refreshScheduleList();
        return card;
    }

    private void refreshScheduleIfVisible() {
        if ("stadium".equals(currentScreen)) {
            refreshScheduleList();
        }
    }

    private void refreshScheduleList() {
        if (scheduleListHost == null || selectedStadium == null) {
            return;
        }
        List<StadiumEvent> events = new ArrayList<>();
        String query = eventSearchField.getText().trim().toLowerCase(Locale.ENGLISH);
        for (StadiumEvent event : bookingService.getEvents(selectedStadium.getId())) {
            boolean dateMatches = selectedScheduleDate == null || event.getDate().equals(selectedScheduleDate);
            boolean queryMatches = query.isEmpty() || event.searchableText().contains(query)
                    || selectedStadium.searchableText().contains(query);
            if (dateMatches && queryMatches) {
                events.add(event);
            }
        }

        scheduleListHost.removeAll();
        countdownLabels.clear();
        eventActionButtons.clear();
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        if (events.isEmpty()) {
            list.add(buildEmptyState("No events match", "Try another date or clear the search field."));
        } else {
            for (StadiumEvent event : events) {
                list.add(createEventCard(event));
                list.add(Box.createVerticalStrut(10));
            }
        }
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scheduleListHost.add(scroll, BorderLayout.CENTER);
        String dateText = selectedScheduleDate == null ? "all dates" : selectedScheduleDate.format(DATE_FORMATTER);
        scheduleResultLabel.setText(events.size() + " event" + (events.size() == 1 ? "" : "s")
                + " • " + dateText);
        scheduleListHost.revalidate();
        scheduleListHost.repaint();
    }

    private String eventCountdownText(StadiumEvent event) {
        StadiumAnnouncement blocking = bookingService.getBlockingAnnouncement(event);
        if (blocking != null) {
            return "Unavailable • " + blocking.getTitle();
        }
        if (!event.isBookingOpen()) {
            return "Closed • " + event.getBookingDeadlineLabel();
        }
        return "Booking stops in " + event.getCountdownLabel();
    }

    private String eventActionText(StadiumEvent event) {
        StadiumAnnouncement blocking = bookingService.getBlockingAnnouncement(event);
        if (blocking != null) {
            if (blocking.getType() == AnnouncementType.CANCELLATION) {
                return "Event cancelled";
            }
            if (blocking.getType() == AnnouncementType.EMERGENCY) {
                return "Emergency notice";
            }
        }
        if (!event.isBookingOpen()) {
            return "Booking closed";
        }
        return event.isGame() ? "Choose game" : "Choose concert";
    }

    private void updateCountdownDisplays() {
        for (Map.Entry<JLabel, StadiumEvent> entry : countdownLabels.entrySet()) {
            StadiumEvent event = entry.getValue();
            entry.getKey().setText(eventCountdownText(event));
            entry.getKey().setForeground(bookingService.isBookingOpen(event) ? MUTED : WARNING_FOREGROUND);
        }
        for (Map.Entry<JButton, StadiumEvent> entry : eventActionButtons.entrySet()) {
            StadiumEvent event = entry.getValue();
            entry.getKey().setText(eventActionText(event));
            entry.getKey().setEnabled(bookingService.isBookingOpen(event));
        }
        if (selectedEvent != null) {
            bookingCountdownValue.setText("Booking stops in " + selectedEvent.getCountdownLabel());
        }
    }

    private JPanel createEventCard(StadiumEvent event) {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(14, 16, 14, 16)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        JPanel time = new JPanel(new GridBagLayout());
        time.setOpaque(false);
        time.setPreferredSize(new Dimension(112, 90));
        GridBagConstraints timeConstraints = new GridBagConstraints();
        timeConstraints.anchor = GridBagConstraints.WEST;
        JLabel start = new JLabel(event.getTimeLabel());
        start.setForeground(event.isGame() ? BLUE_DARK : PURPLE);
        start.setFont(start.getFont().deriveFont(Font.BOLD, 20f));
        time.add(start, timeConstraints);
        JLabel date = new JLabel(event.getDateLabel());
        date.setForeground(MUTED);
        date.setFont(date.getFont().deriveFont(Font.PLAIN, 10f));
        timeConstraints.gridy = 1;
        timeConstraints.insets = new Insets(2, 0, 0, 0);
        time.add(date, timeConstraints);
        JLabel doors = new JLabel("Doors " + event.getDoorsLabel());
        doors.setForeground(MUTED);
        doors.setFont(doors.getFont().deriveFont(Font.PLAIN, 10f));
        timeConstraints.gridy = 2;
        timeConstraints.insets = new Insets(5, 0, 0, 0);
        time.add(doors, timeConstraints);
        card.add(time, BorderLayout.WEST);

        JPanel details = new JPanel(new GridBagLayout());
        details.setOpaque(false);
        GridBagConstraints detailConstraints = new GridBagConstraints();
        detailConstraints.anchor = GridBagConstraints.WEST;
        JLabel type = new JLabel(event.getType().getLabel().toUpperCase(Locale.ENGLISH));
        type.setForeground(event.isGame() ? BLUE_DARK : PURPLE);
        type.setFont(type.getFont().deriveFont(Font.BOLD, 9f));
        details.add(type, detailConstraints);
        JLabel title = new JLabel(event.getHeadline());
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        detailConstraints.gridy = 1;
        detailConstraints.insets = new Insets(3, 0, 0, 0);
        details.add(title, detailConstraints);
        JLabel eventDetails = new JLabel(event.getEventDetails());
        eventDetails.setForeground(MUTED);
        eventDetails.setFont(eventDetails.getFont().deriveFont(Font.PLAIN, 11f));
        detailConstraints.gridy = 2;
        detailConstraints.insets = new Insets(3, 0, 0, 0);
        details.add(eventDetails, detailConstraints);
        card.add(details, BorderLayout.CENTER);

        JPanel action = new JPanel(new GridBagLayout());
        action.setOpaque(false);
        boolean open = bookingService.isBookingOpen(event);
        JLabel availability = new JLabel(String.format(Locale.US, "%.3f%% vacant  •  %s seats",
                bookingService.getVacancyPercentage(event),
                formatCapacity(bookingService.getAvailableSeatCount(event))));
        availability.setForeground(open ? SUCCESS_DARK : WARNING_FOREGROUND);
        availability.setFont(availability.getFont().deriveFont(Font.BOLD, 10f));
        GridBagConstraints actionConstraints = new GridBagConstraints();
        actionConstraints.gridy = 0;
        actionConstraints.anchor = GridBagConstraints.EAST;
        action.add(availability, actionConstraints);
        JLabel deadline = new JLabel(eventCountdownText(event));
        deadline.setForeground(open ? MUTED : WARNING_FOREGROUND);
        deadline.setFont(deadline.getFont().deriveFont(Font.PLAIN, 9f));
        countdownLabels.put(deadline, event);
        actionConstraints.gridy = 1;
        actionConstraints.insets = new Insets(3, 0, 0, 0);
        action.add(deadline, actionConstraints);
        JButton choose = createPrimaryButton(eventActionText(event));
        choose.setEnabled(open);
        eventActionButtons.put(choose, event);
        choose.addActionListener(ignored -> openEvent(event));
        actionConstraints.gridy = 2;
        actionConstraints.insets = new Insets(7, 0, 0, 0);
        action.add(choose, actionConstraints);
        card.add(action, BorderLayout.EAST);
        return card;
    }

    // ---------------------------------------------------------------------
    // Booking screen
    // ---------------------------------------------------------------------

    private void openEvent(StadiumEvent event) {
        if (event == null || selectedStadium == null) {
            return;
        }
        if (!bookingService.isBookingOpen(event)) {
            showWarning(bookingService.getBookingRestrictionMessage(event));
            return;
        }
        selectedEvent = event;
        seatMapPanel.clearSelection();
        bookingService.selectEvent(event);
        seatMapPanel.refreshStatuses();
        currentScreen = "booking";
        setHeader("Book your seats", selectedStadium.getName() + "  •  " + event.getHeadline());
        contentHost.removeAll();
        contentHost.add(buildBookingScreen(), BorderLayout.CENTER);
        contentHost.revalidate();
        contentHost.repaint();
        updateBookingSummary();
        showStatus("Event selected: " + event.getHeadline());
    }

    private JPanel buildBookingScreen() {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setBackground(PAGE);
        page.setBorder(new EmptyBorder(20, 0, 22, 0));
        page.add(buildEventOverview(), BorderLayout.NORTH);
        page.add(buildBookingWorkspace(), BorderLayout.CENTER);
        return page;
    }

    private JPanel buildEventOverview() {
        Color accent = colorFromHex(selectedStadium.getAccentColor(), BLUE);
        JPanel overview = createCard();
        overview.setLayout(new BorderLayout(18, 0));
        overview.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(15, 20, 15, 20)));
        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);
        GridBagConstraints backConstraints = new GridBagConstraints();
        backConstraints.anchor = GridBagConstraints.WEST;
        JButton back = createOutlineButton("← Back to schedule", accent);
        back.addActionListener(event -> openStadium(selectedStadium));
        left.add(back, backConstraints);

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.weightx = 1;
        JLabel stadium = new JLabel(selectedStadium.getName() + "  /  " + selectedEvent.getType().getLabel());
        stadium.setForeground(accent.darker());
        stadium.setFont(stadium.getFont().deriveFont(Font.BOLD, 10f));
        labelConstraints.gridy = 1;
        labelConstraints.insets = new Insets(8, 0, 0, 0);
        left.add(stadium, labelConstraints);
        JLabel title = new JLabel(selectedEvent.getHeadline());
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 21f));
        labelConstraints.gridy = 2;
        labelConstraints.insets = new Insets(4, 0, 0, 0);
        left.add(title, labelConstraints);
        JLabel details = new JLabel(selectedEvent.getEventDetails());
        details.setForeground(MUTED);
        details.setFont(details.getFont().deriveFont(Font.PLAIN, 12f));
        labelConstraints.gridy = 3;
        labelConstraints.insets = new Insets(3, 0, 0, 0);
        left.add(details, labelConstraints);
        JLabel description = new JLabel("<html><div style='width:600px'>" +
                selectedEvent.getDescription() + "</div></html>");
        description.setForeground(MUTED);
        description.setFont(description.getFont().deriveFont(Font.PLAIN, 10f));
        labelConstraints.gridy = 4;
        labelConstraints.insets = new Insets(4, 0, 0, 0);
        left.add(description, labelConstraints);

        JPanel when = new JPanel(new GridBagLayout());
        when.setOpaque(false);
        when.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER), new EmptyBorder(0, 20, 0, 0)));
        GridBagConstraints whenConstraints = new GridBagConstraints();
        whenConstraints.anchor = GridBagConstraints.EAST;
        JLabel date = new JLabel(selectedEvent.getDateLabel());
        date.setForeground(TEXT);
        date.setFont(date.getFont().deriveFont(Font.BOLD, 14f));
        when.add(date, whenConstraints);
        JLabel time = new JLabel("Starts " + selectedEvent.getTimeLabel() + "  •  Doors " + selectedEvent.getDoorsLabel());
        time.setForeground(MUTED);
        time.setFont(time.getFont().deriveFont(Font.PLAIN, 11f));
        whenConstraints.gridy = 1;
        whenConstraints.insets = new Insets(4, 0, 0, 0);
        when.add(time, whenConstraints);
        JLabel deadline = new JLabel("Booking deadline: " + selectedEvent.getBookingDeadlineLabel());
        deadline.setForeground(bookingService.isBookingOpen(selectedEvent) ? MUTED : WARNING_FOREGROUND);
        deadline.setFont(deadline.getFont().deriveFont(Font.PLAIN, 10f));
        whenConstraints.gridy = 2;
        whenConstraints.insets = new Insets(5, 0, 0, 0);
        when.add(deadline, whenConstraints);
        JLabel vacancy = new JLabel(String.format(Locale.US, "%.3f%% of seats vacant",
                bookingService.getVacancyPercentage(selectedEvent)));
        vacancy.setForeground(SUCCESS_DARK);
        vacancy.setFont(vacancy.getFont().deriveFont(Font.BOLD, 11f));
        whenConstraints.gridy = 3;
        whenConstraints.insets = new Insets(3, 0, 0, 0);
        when.add(vacancy, whenConstraints);
        bookingCountdownValue.setForeground(BLUE_DARK);
        bookingCountdownValue.setFont(bookingCountdownValue.getFont().deriveFont(Font.BOLD, 11f));
        bookingCountdownValue.setText("Booking stops in " + selectedEvent.getCountdownLabel());
        whenConstraints.gridy = 4;
        whenConstraints.insets = new Insets(4, 0, 0, 0);
        when.add(bookingCountdownValue, whenConstraints);
        overview.add(left, BorderLayout.CENTER);
        overview.add(when, BorderLayout.EAST);
        return overview;
    }

    private JPanel buildBookingWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout(0, 14));
        workspace.setOpaque(false);
        workspace.add(buildContactCard(), BorderLayout.NORTH);

        JPanel bookingBody = new JPanel(new BorderLayout(14, 0));
        bookingBody.setOpaque(false);
        bookingBody.add(buildSeatMapCard(), BorderLayout.CENTER);
        JPanel pricing = buildPricingBreakdownCard();
        pricing.setPreferredSize(new Dimension(350, 100));
        pricing.setMinimumSize(new Dimension(320, 100));
        bookingBody.add(pricing, BorderLayout.EAST);
        workspace.add(bookingBody, BorderLayout.CENTER);
        workspace.add(buildBookingSummary(), BorderLayout.SOUTH);
        return workspace;
    }

    private JPanel buildContactCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(14, 18, 14, 18)));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("Your details");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        heading.add(title, BorderLayout.WEST);
        JLabel hint = new JLabel("Just three details are needed");
        hint.setForeground(MUTED);
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 10f));
        heading.add(hint, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        addField(fields, 0, "Name", nameField);
        addField(fields, 1, "Email", emailField);
        addField(fields, 2, "Phone", phoneField);
        card.add(fields, BorderLayout.CENTER);
        return card;
    }

    private void addField(JPanel parent, int column, String labelText, JTextField field) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, column == 0 ? 0 : 12, 0, 0);
        JPanel fieldBox = new JPanel(new BorderLayout(0, 5));
        fieldBox.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setForeground(MUTED);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        fieldBox.add(label, BorderLayout.NORTH);
        styleTextField(field);
        fieldBox.add(field, BorderLayout.CENTER);
        parent.add(fieldBox, constraints);
    }

    private JPanel buildSeatMapCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(14, 18, 14, 18)));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("Choose your seats");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        heading.add(title, BorderLayout.WEST);
        JLabel hint = new JLabel("Select up to 6 available seats");
        hint.setForeground(MUTED);
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11f));
        heading.add(hint, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);
        card.add(seatMapPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildPricingBreakdownCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(13, 18, 12, 18)));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel titleBox = new JPanel(new GridBagLayout());
        titleBox.setOpaque(false);
        GridBagConstraints titleConstraints = new GridBagConstraints();
        titleConstraints.anchor = GridBagConstraints.WEST;
        JLabel title = new JLabel("Price outline");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        titleBox.add(title, titleConstraints);
        JLabel subtitle = new JLabel("Every charge is shown before you confirm");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 10f));
        titleConstraints.gridy = 1;
        titleConstraints.insets = new Insets(3, 0, 0, 0);
        titleBox.add(subtitle, titleConstraints);
        heading.add(titleBox, BorderLayout.WEST);
        JLabel feeHint = new JLabel("One booking fee per reservation");
        feeHint.setForeground(MUTED);
        feeHint.setFont(feeHint.getFont().deriveFont(Font.PLAIN, 10f));
        heading.add(feeHint, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);

        pricingBreakdownHost = new JPanel();
        pricingBreakdownHost.setOpaque(false);
        pricingBreakdownHost.setLayout(new BoxLayout(pricingBreakdownHost, BoxLayout.Y_AXIS));
        card.add(pricingBreakdownHost, BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        JLabel note = new JLabel("Seat prices vary by row and section.");
        note.setForeground(MUTED);
        note.setFont(note.getFont().deriveFont(Font.PLAIN, 10f));
        actions.add(note, BorderLayout.CENTER);
        confirmBookingButton = createPrimaryButton("Confirm booked seats");
        confirmBookingButton.addActionListener(event -> confirmBooking());
        actions.add(confirmBookingButton, BorderLayout.EAST);
        card.add(actions, BorderLayout.SOUTH);
        refreshPricingBreakdown();
        return card;
    }

    private JPanel buildBookingSummary() {
        JPanel summary = new JPanel(new BorderLayout(18, 0));
        summary.setBackground(NAVY);
        summary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(NAVY_SOFT), new EmptyBorder(13, 18, 13, 18)));

        JPanel selection = new JPanel(new GridBagLayout());
        selection.setOpaque(false);
        selection.setPreferredSize(new Dimension(430, 52));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        JLabel selectedHeading = new JLabel("YOUR SELECTION");
        selectedHeading.setForeground(new Color(170, 195, 229));
        selectedHeading.setFont(selectedHeading.getFont().deriveFont(Font.BOLD, 10f));
        selection.add(selectedHeading, constraints);
        selectedSeatsValue.setForeground(WHITE);
        selectedSeatsValue.setFont(selectedSeatsValue.getFont().deriveFont(Font.BOLD, 14f));
        constraints.gridy = 1;
        constraints.insets = new Insets(4, 0, 0, 0);
        selection.add(selectedSeatsValue, constraints);

        JPanel total = new JPanel(new GridBagLayout());
        total.setOpaque(false);
        total.setPreferredSize(new Dimension(110, 52));
        GridBagConstraints totalConstraints = new GridBagConstraints();
        totalConstraints.anchor = GridBagConstraints.EAST;
        JLabel totalHeading = new JLabel("TOTAL");
        totalHeading.setForeground(new Color(170, 195, 229));
        totalHeading.setFont(totalHeading.getFont().deriveFont(Font.BOLD, 10f));
        total.add(totalHeading, totalConstraints);
        totalValue.setForeground(new Color(147, 197, 253));
        totalValue.setFont(totalValue.getFont().deriveFont(Font.BOLD, 21f));
        totalConstraints.gridy = 1;
        totalConstraints.insets = new Insets(4, 0, 0, 0);
        total.add(totalValue, totalConstraints);

        JPanel availability = new JPanel(new GridBagLayout());
        availability.setOpaque(false);
        availability.setPreferredSize(new Dimension(120, 52));
        GridBagConstraints availabilityConstraints = new GridBagConstraints();
        availabilityConstraints.anchor = GridBagConstraints.EAST;
        JLabel availableHeading = new JLabel("VACANCY");
        availableHeading.setForeground(new Color(170, 195, 229));
        availableHeading.setFont(availableHeading.getFont().deriveFont(Font.BOLD, 10f));
        availability.add(availableHeading, availabilityConstraints);
        bookingAvailabilityValue.setForeground(new Color(167, 243, 208));
        bookingAvailabilityValue.setFont(bookingAvailabilityValue.getFont().deriveFont(Font.BOLD, 16f));
        availabilityConstraints.gridy = 1;
        availabilityConstraints.insets = new Insets(4, 0, 0, 0);
        availability.add(bookingAvailabilityValue, availabilityConstraints);

        JPanel middle = new JPanel(new BorderLayout(15, 0));
        middle.setOpaque(false);
        middle.add(selection, BorderLayout.WEST);
        middle.add(total, BorderLayout.CENTER);
        middle.add(availability, BorderLayout.EAST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 3));
        actions.setOpaque(false);
        clearSelectionButton = createSecondaryButton("Clear seats");
        clearSelectionButton.addActionListener(event -> clearSelection());
        actions.add(clearSelectionButton);
        summary.add(middle, BorderLayout.CENTER);
        summary.add(actions, BorderLayout.EAST);
        return summary;
    }

    private void onSeatToggled(Seat seat) {
        boolean selected = seatMapPanel.getSelectedSeats().stream()
                .anyMatch(item -> item.getKey().equals(seat.getKey()));
        showStatus((selected ? "Selected seat " : "Removed seat ") + seat.display());
    }

    private void refreshPricingBreakdown() {
        if (pricingBreakdownHost == null) {
            return;
        }
        pricingBreakdownHost.removeAll();
        List<Seat> selectedSeats = seatMapPanel.getSelectedSeats();
        if (selectedSeats.isEmpty()) {
            JLabel empty = new JLabel("Select one or more vacant seats to see the full price outline.");
            empty.setForeground(MUTED);
            empty.setFont(empty.getFont().deriveFont(Font.PLAIN, 11f));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            pricingBreakdownHost.add(empty);
        } else {
            for (Seat seat : selectedSeats) {
                pricingBreakdownHost.add(createPriceRow(
                        seat.display() + "  •  " + bookingService.getRowTier(seat.getKey()),
                        currency(seat.getPrice()), false));
            }
            pricingBreakdownHost.add(Box.createVerticalStrut(4));
            pricingBreakdownHost.add(createPriceRow("Seat subtotal",
                    currency(bookingService.totalFor(selectedSeats)), false));
            pricingBreakdownHost.add(createPriceRow("Booking fee (once)",
                    currency(bookingService.getBookingFee()), false));
            pricingBreakdownHost.add(createPriceRow("Total due",
                    currency(bookingService.getTotalCharge(selectedSeats)), true));
        }
        pricingBreakdownHost.revalidate();
        pricingBreakdownHost.repaint();
        if (confirmBookingButton != null) {
            confirmBookingButton.setEnabled(!selectedSeats.isEmpty() && bookingService.isBookingOpen());
        }
    }

    private JPanel createPriceRow(String label, String value, boolean emphasis) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, emphasis ? 29 : 24));
        JLabel left = new JLabel(label);
        left.setForeground(emphasis ? TEXT : MUTED);
        left.setFont(left.getFont().deriveFont(emphasis ? Font.BOLD : Font.PLAIN,
                emphasis ? 12f : 11f));
        JLabel right = new JLabel(value);
        right.setForeground(emphasis ? BLUE_DARK : TEXT);
        right.setFont(right.getFont().deriveFont(Font.BOLD, emphasis ? 13f : 11f));
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(left, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private void updateBookingSummary() {
        if (selectedEvent == null) {
            return;
        }
        List<Seat> selectedSeats = seatMapPanel.getSelectedSeats();
        selectedSeatsValue.setText(selectedSeats.isEmpty() ? "No seats selected" : joinSeatNames(selectedSeats));
        totalValue.setText(currency(bookingService.getTotalCharge(selectedSeats)));
        bookingAvailabilityValue.setText(String.format(Locale.US, "%.3f%%",
                bookingService.getVacancyPercentage()));
        bookingAvailabilityValue.setForeground(bookingService.getVacancyPercentage() <= 10.0
                ? WARNING_FOREGROUND : new Color(167, 243, 208));
        if (clearSelectionButton != null) {
            clearSelectionButton.setEnabled(!selectedSeats.isEmpty());
        }
        if (confirmBookingButton != null) {
            confirmBookingButton.setEnabled(!selectedSeats.isEmpty() && bookingService.isBookingOpen());
        }
        refreshPricingBreakdown();
        updateCountdownDisplays();
    }

    private void clearSelection() {
        seatMapPanel.clearSelection();
        showStatus("Seat selection cleared");
    }

    private void confirmBooking() {
        if (selectedEvent == null) {
            return;
        }
        List<Seat> selectedSeats = seatMapPanel.getSelectedSeats();
        if (selectedSeats.isEmpty()) {
            showWarning("Select at least one seat before confirming.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Confirm " + selectedSeats.size() + " seat" + (selectedSeats.size() == 1 ? "" : "s")
                        + " for " + currency(bookingService.getTotalCharge(selectedSeats)) + "?\n\n"
                        + "Seat subtotal: " + currency(bookingService.totalFor(selectedSeats))
                        + "  •  Booking fee: " + currency(bookingService.getBookingFee()) + "\n"
                        + selectedEvent.getHeadline() + "  •  " + selectedEvent.getWhenLabel(),
                "Confirm booked seats", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            Booking booking = bookingService.book(nameField.getText(), emailField.getText(),
                    phoneField.getText(), selectedSeats);
            seatMapPanel.clearSelection();
            seatMapPanel.refreshStatuses();
            clearContactFields();
            updateBookingSummary();
            showStatus("Seat" + (booking.getSeats().size() == 1 ? "" : "s") + " booked: "
                    + booking.getSeatDisplay());
            JOptionPane.showMessageDialog(this,
                    "Your seat booking is confirmed.\n\n"
                            + "Booked seat" + (booking.getSeats().size() == 1 ? "" : "s") + ": "
                            + booking.getSeatDisplay() + "\n"
                            + "Reference: " + booking.getReference() + "\n"
                            + "Event: " + booking.getEvent() + "\n"
                            + "Booked before: " + selectedEvent.getBookingDeadlineLabel() + "\n"
                            + "Total: " + currency(booking.getTotal()),
                    "Booking confirmed", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException exception) {
            showWarning(exception.getMessage());
        }
    }

    private void clearContactFields() {
        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");
    }

    // ---------------------------------------------------------------------
    // Booking history
    // ---------------------------------------------------------------------

    private void showBookings() {
        currentScreen = "directory";
        bookingSearchField.setText("");
        currentScreen = "bookings";
        setHeader("My bookings", "Review, search and cancel reservations for every stadium and event.");
        contentHost.removeAll();
        contentHost.add(buildBookingsContent(), BorderLayout.CENTER);
        contentHost.revalidate();
        contentHost.repaint();
        refreshBookings();
        showStatus("Booking history");
    }

    private void refreshBookingsIfVisible() {
        if ("bookings".equals(currentScreen)) {
            refreshBookings();
        }
    }

    private JPanel buildBookingsContent() {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setBackground(PAGE);
        page.setBorder(new EmptyBorder(20, 0, 22, 0));

        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        JPanel titleBox = new JPanel(new GridBagLayout());
        titleBox.setOpaque(false);
        GridBagConstraints backConstraints = new GridBagConstraints();
        backConstraints.anchor = GridBagConstraints.WEST;
        JButton back = createOutlineButton("← Back to stadiums", BLUE);
        back.addActionListener(event -> showStadiumDirectory());
        titleBox.add(back, backConstraints);

        GridBagConstraints titleConstraints = new GridBagConstraints();
        titleConstraints.gridx = 0;
        titleConstraints.anchor = GridBagConstraints.WEST;
        titleConstraints.fill = GridBagConstraints.HORIZONTAL;
        titleConstraints.weightx = 1;
        JLabel title = new JLabel("Booking history");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 21f));
        titleConstraints.gridy = 1;
        titleConstraints.insets = new Insets(7, 0, 0, 0);
        titleBox.add(title, titleConstraints);
        JLabel subtitle = new JLabel("Every confirmed or cancelled reservation in one place.");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 11f));
        titleConstraints.gridy = 2;
        titleConstraints.insets = new Insets(3, 0, 0, 0);
        titleBox.add(subtitle, titleConstraints);

        GridBagConstraints topTitleConstraints = new GridBagConstraints();
        topTitleConstraints.gridx = 0;
        topTitleConstraints.weightx = 1;
        topTitleConstraints.fill = GridBagConstraints.HORIZONTAL;
        topTitleConstraints.anchor = GridBagConstraints.WEST;
        top.add(titleBox, topTitleConstraints);

        JPanel search = new JPanel(new BorderLayout(10, 0));
        search.setOpaque(false);
        search.setPreferredSize(new Dimension(390, 42));
        search.add(buildSearchBar(bookingSearchField, "Search bookings"), BorderLayout.CENTER);
        GridBagConstraints topSearchConstraints = new GridBagConstraints();
        topSearchConstraints.gridx = 1;
        topSearchConstraints.anchor = GridBagConstraints.NORTHEAST;
        topSearchConstraints.fill = GridBagConstraints.NONE;
        top.add(search, topSearchConstraints);
        page.add(top, BorderLayout.NORTH);

        JPanel tableCard = createCard();
        tableCard.setLayout(new BorderLayout(0, 10));
        tableCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(14, 14, 14, 14)));
        configureBookingTable();
        JScrollPane tableScroll = new JScrollPane(bookingTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(WHITE);
        tableScroll.getVerticalScrollBar().setUnitIncrement(18);
        tableCard.add(tableScroll, BorderLayout.CENTER);
        page.add(tableCard, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottom.setOpaque(false);
        JButton refresh = createSecondaryButton("Refresh");
        refresh.addActionListener(event -> refreshBookings());
        cancelBookingButton = createSecondaryButton("Cancel selected booking");
        cancelBookingButton.setForeground(new Color(185, 28, 28));
        cancelBookingButton.addActionListener(event -> cancelSelectedBooking());
        bottom.add(refresh);
        bottom.add(cancelBookingButton);
        page.add(bottom, BorderLayout.SOUTH);
        return page;
    }

    private void configureBookingTable() {
        bookingTable.setBackground(WHITE);
        bookingTable.setForeground(TEXT);
        bookingTable.setRowHeight(34);
        bookingTable.setShowVerticalLines(false);
        bookingTable.setGridColor(new Color(235, 240, 247));
        bookingTable.setIntercellSpacing(new Dimension(0, 1));
        bookingTable.setSelectionBackground(new Color(219, 234, 254));
        bookingTable.setSelectionForeground(TEXT);
        bookingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookingTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        bookingTable.setFillsViewportHeight(true);
        JTableHeader header = bookingTable.getTableHeader();
        header.setBackground(SKY);
        header.setForeground(BLUE_DARK);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 11f));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(100, 38));
        TableCellRenderer renderer = new BookingTableRenderer();
        for (int column = 0; column < bookingTable.getColumnCount(); column++) {
            bookingTable.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }
        int[] widths = {100, 175, 270, 190, 190, 95, 120};
        for (int column = 0; column < widths.length; column++) {
            bookingTable.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        }
    }

    private void refreshBookings() {
        if (bookingTableModel == null) {
            return;
        }
        String query = bookingSearchField.getText().trim().toLowerCase(Locale.ENGLISH);
        List<Booking> visible = new ArrayList<>();
        for (Booking booking : bookingService.getBookings()) {
            if (query.isEmpty() || bookingSearchText(booking).contains(query)) {
                visible.add(booking);
            }
        }
        bookingTableModel.setRowCount(0);
        for (Booking booking : visible) {
            StadiumEvent event = StadiumData.getEvent(booking.getEventId());
            Stadium stadium = StadiumData.getStadium(booking.getStadiumId());
            bookingTableModel.addRow(new Object[]{
                    booking.getReference(),
                    stadium == null ? "Legacy venue" : stadium.getName(),
                    event == null ? booking.getEvent() : event.getHeadline(),
                    bookingWhenLabel(booking, event),
                    booking.getSeatDisplay(),
                    currency(booking.getTotal()),
                    booking.getStatus().name()
            });
        }
        updateCancelButton();
    }

    private String bookingSearchText(Booking booking) {
        Stadium stadium = StadiumData.getStadium(booking.getStadiumId());
        StadiumEvent event = StadiumData.getEvent(booking.getEventId());
        String eventText = event == null ? booking.getEvent()
                : event.getHeadline() + " " + event.getEventDetails() + " " + bookingWhenLabel(booking, event);
        return String.join(" ", booking.getReference(), stadium == null ? "" : stadium.getName(),
                eventText, booking.getSeatDisplay(), booking.getStatus().name()).toLowerCase(Locale.ENGLISH);
    }

    private String bookingWhenLabel(Booking booking, StadiumEvent event) {
        if (booking.getEventDate() != null && booking.getEventStartTime() != null) {
            return booking.getEventDate().format(DATE_FORMATTER) + "  •  "
                    + booking.getEventStartTime().format(TIME_FORMATTER);
        }
        return event == null ? "—" : event.getWhenLabel();
    }

    private void updateCancelButton() {
        if (cancelBookingButton != null) {
            cancelBookingButton.setEnabled(bookingTable.getSelectedRow() >= 0);
        }
    }

    private void cancelSelectedBooking() {
        int row = bookingTable.getSelectedRow();
        if (row < 0) {
            showWarning("Select a booking from the table first.");
            return;
        }
        String reference = String.valueOf(bookingTableModel.getValueAt(row, 0));
        String status = String.valueOf(bookingTableModel.getValueAt(row, 6));
        if ("CANCELLED".equals(status)) {
            showWarning("That booking is already cancelled.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Cancel booking " + reference + "?\n\nThe seats will become available again.",
                "Cancel booking", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        if (bookingService.cancel(reference)) {
            seatMapPanel.refreshStatuses();
            refreshBookings();
            updateBookingSummary();
            showStatus("Booking " + reference + " cancelled");
        } else {
            showWarning("The booking could not be cancelled.");
        }
    }

    // ---------------------------------------------------------------------
    // Shared UI helpers
    // ---------------------------------------------------------------------

    private JPanel buildSearchBar(JTextField field, String hint) {
        styleSearchField(field);
        if (field instanceof SearchField) {
            ((SearchField) field).setHint(hint);
        }
        JPanel bar = new JPanel(new BorderLayout(7, 0));
        bar.setBackground(WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(4, 9, 4, 9)));
        JLabel icon = new JLabel("⌕");
        icon.setForeground(BLUE);
        icon.setFont(icon.getFont().deriveFont(Font.BOLD, 20f));
        icon.setPreferredSize(new Dimension(20, 28));
        field.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        bar.add(icon, BorderLayout.WEST);
        bar.add(field, BorderLayout.CENTER);
        return bar;
    }

    private void styleSearchField(JTextField field) {
        field.setFont(field.getFont().deriveFont(Font.PLAIN, 12f));
        field.setForeground(TEXT);
        field.setBackground(WHITE);
        field.setCaretColor(BLUE);
        field.setPreferredSize(new Dimension(220, 30));
    }

    private void styleTextField(JTextField field) {
        field.setFont(field.getFont().deriveFont(Font.PLAIN, 13f));
        field.setForeground(TEXT);
        field.setBackground(new Color(249, 251, 255));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(8, 10, 8, 10)));
        field.setPreferredSize(new Dimension(180, 37));
    }

    private JPanel createCard() {
        JPanel card = new JPanel();
        card.setBackground(WHITE);
        return card;
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 11f));
        button.setForeground(WHITE);
        button.setBackground(BLUE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BLUE_DARK), new EmptyBorder(9, 14, 9, 14)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent event) {
                if (button.isEnabled()) {
                    button.setBackground(BLUE_DARK);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent event) {
                if (button.isEnabled()) {
                    button.setBackground(BLUE);
                }
            }
        });
        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 11f));
        button.setForeground(TEXT);
        button.setBackground(WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 202, 218)), new EmptyBorder(9, 13, 9, 13)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createOutlineButton(String text, Color accent) {
        JButton button = new JButton(text);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 11f));
        button.setForeground(accent.darker());
        button.setBackground(WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent), new EmptyBorder(8, 12, 8, 12)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JPanel buildEmptyState(String title, String message) {
        JPanel empty = createCard();
        empty.setLayout(new GridBagLayout());
        empty.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(28, 20, 28, 20)));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        JLabel heading = new JLabel(title);
        heading.setForeground(TEXT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 15f));
        empty.add(heading, constraints);
        JLabel detail = new JLabel(message);
        detail.setForeground(MUTED);
        detail.setFont(detail.getFont().deriveFont(Font.PLAIN, 11f));
        constraints.gridy = 1;
        constraints.insets = new Insets(6, 0, 0, 0);
        empty.add(detail, constraints);
        return empty;
    }

    private void showStatus(String message) {
        statusValue.setText(message == null || message.trim().isEmpty() ? "Ready" : message);
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Please check your booking",
                JOptionPane.WARNING_MESSAGE);
    }

    private String joinSeatNames(List<Seat> seats) {
        StringBuilder result = new StringBuilder();
        for (Seat seat : seats) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(seat.display());
        }
        return result.toString();
    }

    private String htmlText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String currency(double amount) {
        return String.format(Locale.US, "$%.2f", amount);
    }

    private String formatCapacity(int capacity) {
        return NumberFormat.getIntegerInstance(Locale.US).format(capacity);
    }

    private Color colorFromHex(String value, Color fallback) {
        try {
            return Color.decode(value);
        } catch (Exception exception) {
            return fallback;
        }
    }

    private final class BookingTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected,
                    hasFocus, row, column);
            if (!isSelected) {
                String status = String.valueOf(table.getValueAt(row, 6));
                if ("CANCELLED".equals(status)) {
                    component.setForeground(CANCELLED);
                } else if (column == 6) {
                    component.setForeground(SUCCESS_DARK);
                } else if (column == 0) {
                    component.setForeground(BLUE_DARK);
                }
            }
            setBorder(new EmptyBorder(0, 8, 0, 8));
            return component;
        }
    }

    private static final class SearchField extends JTextField {
        private String hint = "";

        SearchField(String hint) {
            this.hint = hint;
            setOpaque(true);
        }

        void setHint(String hint) {
            this.hint = hint == null ? "" : hint;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!getText().isEmpty() || hint.isEmpty()) {
                return;
            }
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            copy.setColor(new Color(148, 163, 184));
            Insets insets = getInsets();
            copy.drawString(hint, insets.left + 2, insets.top + getFontMetrics(getFont()).getAscent());
            copy.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // The default Swing look and feel is a suitable fallback.
            }
            new StadiumBookingApp().setVisible(true);
        });
    }
}
