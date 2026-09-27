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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
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
import javax.swing.plaf.basic.BasicButtonUI;
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
    private final JLabel statusValue = new JLabel(Messages.get("status.chooseStadium"));
    private final SearchField stadiumSearchField =
            new SearchField("Search stadium, city, team or artist");
    private final SearchField eventSearchField =
            new SearchField("Search teams, artists, sport or date");
    private final SearchField bookingSearchField =
            new SearchField("Search reference, stadium, event or status");
    private final SearchField liveSearchField =
            new SearchField("Search teams, artists, venues or dates");
    private final JTextField nameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JLabel selectedSeatsValue = new JLabel(text("booking.noSeats"));
    private final JLabel totalValue = new JLabel("$0.00");
    private final JLabel bookingAvailabilityValue = new JLabel();
    private final JLabel bookingCountdownValue = new JLabel();
    private JPanel pricingBreakdownHost;
    private final Map<JLabel, StadiumEvent> countdownLabels = new LinkedHashMap<>();
    private final Map<JButton, StadiumEvent> eventActionButtons = new LinkedHashMap<>();
    private final DefaultTableModel bookingTableModel;
    private final JTable bookingTable;
    private JComboBox<Messages.Language> languageCombo;
    private final JButton backNavButton = new FeedbackButton(Messages.get("nav.back"));
    private final JButton stadiumNavButton = new FeedbackButton(Messages.get("nav.venues"));
    private final JButton bookingsNavButton = new FeedbackButton(Messages.get("nav.bookings"));
    private final JButton seatLedgerNavButton = new FeedbackButton(Messages.get("nav.bookedSeats"));
    private final JButton occupancyNavButton = new FeedbackButton(Messages.get("nav.occupancy"));
    private final JButton staffNavButton = new FeedbackButton(Messages.get("nav.staff"));

    private Stadium selectedStadium;
    private StadiumEvent selectedEvent;
    private LocalDate selectedScheduleDate;
    private List<LocalDate> scheduleDates = new ArrayList<>();
    private List<StadiumEvent> ledgerEventOptions = new ArrayList<>();
    private JComboBox<String> liveDateCombo;
    private JComboBox<String> liveTypeCombo;
    private JPanel liveListHost;
    private JLabel liveResultLabel;
    private JPanel scheduleListHost;
    private JLabel scheduleResultLabel;
    private JComboBox<String> dateCombo;
    private JButton clearSelectionButton;
    private JButton confirmBookingButton;
    private JButton cancelBookingButton;
    private JComboBox<String> ledgerStadiumCombo;
    private JComboBox<String> ledgerEventCombo;
    private DefaultTableModel ledgerTableModel;
    private JTable ledgerTable;
    private JLabel ledgerSummaryLabel;
    private JLabel ledgerSectionLabel;
    private String currentScreen = "directory";
    private Database database;
    private StaffDirectory staffDirectory;
    private StaffSession staffSession;
    private JLabel staffBadge;
    private final String sessionOwner = UUID.randomUUID().toString();
    private JLabel holdCountdownValue;
    private String ledgerReturnScreen = "directory";
    private final Timer directorySearchTimer = new Timer(140, event -> {
        if ("directory".equals(currentScreen)) {
            refreshDirectoryContent();
        }
    });
    /** Ticks once a second to keep countdowns and seat holds current. */
    private final Timer countdownTimer;
    private final Timer liveSearchTimer = new Timer(200, event -> {
        if ("schedules".equals(currentScreen)) {
            refreshLiveSchedules();
        }
    });

    public StadiumBookingApp() {
        super("Stadium Select");
        directorySearchTimer.setRepeats(false);
        liveSearchTimer.setRepeats(false);
        // Bookings and staff accounts live in one database, so a single Database
        // instance is shared between them.
        database = openDatabase();
        bookingService = new BookingService(database);
        staffDirectory = new StaffDirectory(database);
        staffSession = new StaffSession(staffDirectory);
        seatMapPanel = new SeatMapPanel(bookingService, this::onSeatToggled,
                this::updateBookingSummary, this::showStatus);
        seatMapPanel.setHoldOwner(sessionOwner);
        bookingTableModel = new DefaultTableModel(
                new Object[]{"Reference", "Stadium", "Event", "When", "Seats", "Total", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bookingTable = new JTable(bookingTableModel);
        bookingTable.getSelectionModel().addListSelectionListener(event -> updateCancelButton());
        bookingTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }
                int row = bookingTable.rowAtPoint(event.getPoint());
                if (row >= 0) {
                    bookingTable.setRowSelectionInterval(row, row);
                    showBookingDetails(row);
                }
            }
        });

        countdownTimer = new Timer(1_000, event -> {
            updateCountdownDisplays();
            seatMapPanel.getHoldService().purgeExpired();
            seatMapPanel.refreshStatuses();
            updateHoldCountdown();
        });
        countdownTimer.setRepeats(true);

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
        // Live suggestion lists that refresh as each field is typed into.
        new SearchSuggestions(stadiumSearchField, this::stadiumSuggestions);
        new SearchSuggestions(eventSearchField, this::eventSuggestions);
        new SearchSuggestions(bookingSearchField, this::bookingSuggestions);
        new SearchSuggestions(liveSearchField, this::eventSuggestions);
        addDocumentListener(liveSearchField, liveSearchTimer::restart);
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

    /** Rebuilt whenever the language changes. */
    private JLabel brandLabel;

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
        JLabel brand = new JLabel(text("app.tagline"));
        brandLabel = brand;
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

        languageCombo = new JComboBox<>(Messages.Language.values());
        languageCombo.setSelectedItem(Messages.getLanguage());
        languageCombo.setFont(languageCombo.getFont().deriveFont(Font.BOLD, 10f));
        languageCombo.setFocusable(false);
        languageCombo.setPreferredSize(new Dimension(110, 28));
        languageCombo.setToolTipText("Change the interface language");
        languageCombo.addActionListener(event -> {
            Messages.setLanguage((Messages.Language) languageCombo.getSelectedItem());
            applyLanguage();
        });

        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 17));
        navigation.setOpaque(false);
        navigation.add(languageCombo);
        styleHeaderButton(backNavButton);
        styleHeaderButton(stadiumNavButton);
        styleHeaderButton(bookingsNavButton);
        styleHeaderButton(seatLedgerNavButton);
        styleHeaderButton(occupancyNavButton);
        styleHeaderButton(staffNavButton);
        backNavButton.addActionListener(event -> goBack());
        stadiumNavButton.addActionListener(event -> showStadiumDirectory());
        bookingsNavButton.addActionListener(event -> showBookings());
        seatLedgerNavButton.addActionListener(event -> showSeatLedger());
        occupancyNavButton.addActionListener(event -> showOccupancyReport());
        staffNavButton.addActionListener(event -> promptForStaffAction());
        describe(backNavButton, "Back", "Return to the previous screen");
        describe(stadiumNavButton, "Venues", "Show every stadium and concert venue");
        describe(bookingsNavButton, "My bookings", "Sign in and review your reservations");
        describe(seatLedgerNavButton, "Booked seats",
                "Sign in and see which seats are already taken at each venue and event");
        describe(occupancyNavButton, "Occupancy",
                "Sign in and see how full each venue and section is");
        describe(staffNavButton, "Staff",
                "Sign in, change your PIN, or manage staff accounts");
        navigation.add(backNavButton);
        navigation.add(stadiumNavButton);
        navigation.add(bookingsNavButton);
        navigation.add(seatLedgerNavButton);
        navigation.add(occupancyNavButton);
        navigation.add(staffNavButton);

        header.add(titleBlock, BorderLayout.WEST);
        staffBadge = new JLabel("Not signed in");
        staffBadge.setForeground(new Color(191, 219, 254));
        staffBadge.setFont(staffBadge.getFont().deriveFont(Font.PLAIN, 10f));
        JPanel badgeBox = new JPanel(new BorderLayout(0, 1));
        badgeBox.setOpaque(false);
        badgeBox.add(navigation, BorderLayout.CENTER);
        badgeBox.add(staffBadge, BorderLayout.SOUTH);
        header.add(badgeBox, BorderLayout.EAST);
        return header;
    }

    private void styleHeaderButton(JButton button) {
        button.setFont(button.getFont().deriveFont(Font.BOLD, 11f));
        button.setForeground(new Color(219, 234, 254));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        // Keeps the navy header visible through the button; the outline and text
        // carry the hover and pressed feedback instead of a background fill.
        button.setOpaque(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(76, 112, 164)),
                new EmptyBorder(8, 12, 8, 12)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addInteractionFeedback(button);
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

    /** Shorthand for the current language's wording. */
    private static String text(String key) {
        return Messages.get(key);
    }

    private static String text(String key, Object... arguments) {
        return Messages.get(key, arguments);
    }

    /** Re-applies the current language to the header, then redraws the screen. */
    private void applyLanguage() {
        if (brandLabel != null) {
            brandLabel.setText(text("app.tagline"));
        }
        occupancyNavButton.setText(text("nav.occupancy"));
        staffNavButton.setText(text("nav.staff"));
        updateStaffBadge();
        seatMapPanel.retranslate();
        backNavButton.setText(text("nav.back"));
        stadiumNavButton.setText(text("nav.venues"));
        bookingsNavButton.setText(text("nav.bookings"));
        seatLedgerNavButton.setText(text("nav.bookedSeats"));
        if ("directory".equals(currentScreen)) {
            setHeader(text("directory.title"), text("directory.subtitle"));
            refreshDirectoryContent();
        } else if ("booking".equals(currentScreen) && selectedStadium != null) {
            refreshBookingScreen();
        }
    }

    /** Rebuilds the booking screen so its wording follows the language. */
    private void refreshBookingScreen() {
        if (selectedEvent == null || selectedStadium == null) {
            return;
        }
        setHeader(text("booking.title"),
                selectedStadium.getName() + "  •  " + selectedEvent.getHeadline());
        contentHost.removeAll();
        contentHost.add(buildBookingScreen(), BorderLayout.CENTER);
        contentHost.revalidate();
        contentHost.repaint();
        updateBookingSummary();
    }

    private void setHeader(String title, String subtitle) {
        headerTitle.setText(title);
        headerSubtitle.setText(subtitle);
        backNavButton.setEnabled(!"directory".equals(currentScreen));
    }

    private static final String BACK_LABEL = "← Back";

    /**
     * Shows a modal dialog with custom buttons and returns the label of the button the
     * user pressed.
     *
     * <p>{@link JOptionPane#showOptionDialog} returns the <em>index</em> of the chosen
     * option rather than the option object itself, so the index is mapped back to its
     * label here. Without this mapping every custom button would compare unequal to its
     * own label and the action would be silently discarded. Closing the dialog with the
     * window button returns {@link JOptionPane#CLOSED_OPTION} and yields {@code null}.
     */
    private String showDialogChoice(Component parent, Object message, String title,
                                    int messageType, String initialChoice, String... choices) {
        int index = JOptionPane.showOptionDialog(parent, message, title,
                JOptionPane.DEFAULT_OPTION, messageType, null, choices, initialChoice);
        if (index < 0 || index >= choices.length) {
            return null;
        }
        return choices[index];
    }

    private void goBack() {
        if ("booking".equals(currentScreen) && selectedStadium != null) {
            openStadium(selectedStadium);
        } else if ("seats".equals(currentScreen)) {
            if ("booking".equals(ledgerReturnScreen) && selectedEvent != null) {
                openEvent(selectedEvent);
            } else {
                showStadiumDirectory();
            }
        } else if ("stadium".equals(currentScreen) || "bookings".equals(currentScreen)
                || "schedules".equals(currentScreen) || "occupancy".equals(currentScreen)
                || "staff".equals(currentScreen)) {
            showStadiumDirectory();
        }
    }

    // ---------------------------------------------------------------------
    // Stadium directory
    // ---------------------------------------------------------------------

    private void showStadiumDirectory() {
        currentScreen = "directory";
        selectedStadium = null;
        selectedEvent = null;
        setHeader(Messages.get("directory.title"), Messages.get("directory.subtitle"));
        refreshDirectoryContent();
        showStatus("Choose a stadium to begin");
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
        JLabel searchLabel = new JLabel(text("directory.findVenue"));
        searchLabel.setForeground(MUTED);
        searchLabel.setFont(searchLabel.getFont().deriveFont(Font.BOLD, 10f));
        searchLabel.setPreferredSize(new Dimension(105, 38));
        searchCard.add(searchLabel, BorderLayout.WEST);
        searchCard.add(buildSearchBar(stadiumSearchField, text("directory.search.hint")), BorderLayout.CENTER);
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
        JLabel eyebrow = new JLabel(text("directory.hero.eyebrow"));
        eyebrow.setForeground(BLUE_DARK);
        eyebrow.setFont(eyebrow.getFont().deriveFont(Font.BOLD, 10f));
        copy.add(eyebrow, constraints);
        JLabel title = new JLabel(text("directory.hero.title"));
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        constraints.gridy = 1;
        constraints.insets = new Insets(5, 0, 0, 0);
        copy.add(title, constraints);
        JLabel subtitle = new JLabel(Messages.get("directory.hero.subtitle"));
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 12f));
        constraints.gridy = 2;
        constraints.insets = new Insets(4, 0, 0, 0);
        copy.add(subtitle, constraints);

        // Opens the cross-venue schedule, so it is a real button with the same
        // hover and press feedback as the rest of the interface.
        JButton liveSchedules = new FeedbackButton("  " + text("nav.liveSchedules") + "  ");
        liveSchedules.setFont(liveSchedules.getFont().deriveFont(Font.BOLD, 10f));
        liveSchedules.setForeground(BLUE_DARK);
        liveSchedules.setBackground(new Color(219, 234, 254));
        liveSchedules.setFocusPainted(false);
        liveSchedules.setOpaque(true);
        liveSchedules.setContentAreaFilled(true);
        liveSchedules.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(191, 219, 254)), new EmptyBorder(9, 8, 9, 8)));
        liveSchedules.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        liveSchedules.setToolTipText("Browse every upcoming game and concert across all venues");
        addInteractionFeedback(liveSchedules);
        liveSchedules.addActionListener(event -> showLiveSchedules());
        JPanel badgeHolder = new JPanel(new GridBagLayout());
        badgeHolder.setOpaque(false);
        badgeHolder.add(liveSchedules);
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
        JButton open = createOutlineButton(Messages.get("directory.viewDetails"), accent);
        open.addActionListener(event -> showStadiumDetails(stadium));
        footer.add(facts, BorderLayout.CENTER);
        footer.add(open, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        // Tapping anywhere on the card opens the venue, not only the button.
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Open " + stadium.getName());
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    showStadiumDetails(stadium);
                }
            }
        });
        for (java.awt.Component child : card.getComponents()) {
            attachCardClick(child, stadium);
        }
        return card;
    }

    /** Lets a click anywhere on a card, not just its button, open the venue. */
    private void attachCardClick(java.awt.Component component, Stadium stadium) {
        if (component instanceof AbstractButton) {
            return; // buttons keep their own listener
        }
        component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    showStadiumDetails(stadium);
                }
            }
        });
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                attachCardClick(child, stadium);
            }
        }
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

    // ---------------------------------------------------------------------
    // Stadium details
    // ---------------------------------------------------------------------

    /**
     * Everything the application knows about one venue, in one screen.
     *
     * <p>The picture, the address, the description, the four sections with their
     * real prices, what is on there and how full it is. The schedule and the seat
     * map are one click away, so choosing a venue still leads to booking.
     */
    private void showStadiumDetails(Stadium stadium) {
        currentScreen = "stadium-details";
        selectedStadium = stadium;
        StadiumDetails details = StadiumDetails.of(stadium, bookingService);
        setHeader(stadium.getName(),
                details.getSummary());
        contentHost.removeAll();
        contentHost.add(buildStadiumDetailsContent(stadium, details), BorderLayout.CENTER);
        contentHost.revalidate();
        contentHost.repaint();
        showStatus(stadium.getName() + "  •  " + stadium.getLocation());
    }

    private JPanel buildStadiumDetailsContent(Stadium stadium, StadiumDetails details) {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setBackground(PAGE);
        page.setBorder(new EmptyBorder(16, 0, 20, 0));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        top.setOpaque(false);
        JButton back = createOutlineButton("\u2190 All stadiums", BLUE);
        back.addActionListener(event -> showStadiumDirectory());
        top.add(back);
        page.add(top, BorderLayout.NORTH);

        JPanel middle = new JPanel();
        middle.setOpaque(false);
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));

        // The picture beside the headline figures, rather than swallowing the page.
        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setOpaque(false);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 268));
        StadiumPhotoPanel picture = new StadiumPhotoPanel(stadium);
        JPanel pictureCard = createCard();
        pictureCard.setLayout(new BorderLayout(0, 0));
        pictureCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(8, 8, 8, 8)));
        pictureCard.add(picture, BorderLayout.CENTER);
        pictureCard.setPreferredSize(new Dimension(520, 252));
        topRow.add(pictureCard, BorderLayout.WEST);

        JPanel factsCard = createCard();
        factsCard.setLayout(new BorderLayout(0, 10));
        factsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(12, 14, 12, 14)));
        factsCard.add(buildQuickFacts(stadium, details), BorderLayout.CENTER);
        factsCard.add(buildHighlights(details), BorderLayout.SOUTH);
        topRow.add(factsCard, BorderLayout.CENTER);
        middle.add(topRow);

        middle.add(Box.createVerticalStrut(12));

        JPanel lower = new JPanel(new BorderLayout(12, 0));
        lower.setOpaque(false);
        JPanel about = buildAboutCard(stadium);
        about.setPreferredSize(new Dimension(320, 100));
        JPanel sections = buildSectionsCard(details);
        sections.setPreferredSize(new Dimension(430, 100));
        JPanel side = buildSideCard(details, stadium);
        side.setPreferredSize(new Dimension(400, 100));
        lower.add(about, BorderLayout.WEST);
        lower.add(sections, BorderLayout.CENTER);
        lower.add(side, BorderLayout.EAST);
        middle.add(lower);

        page.add(middle, BorderLayout.CENTER);
        page.add(buildDetailsActions(stadium), BorderLayout.SOUTH);
        return page;
    }

    /** The handful of facts worth reading first, beside the picture. */
    private JPanel buildHighlights(StadiumDetails details) {
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        StadiumEvent next = details.getNextEvent();
        if (next != null) {
            text.add(sideText("<b>Next up</b>  " + next.getHeadline(), "#1e293b", 12f));
            text.add(sideText(Messages.dateLabel(next.getDate()) + "  •  "
                    + next.getTimeLabel() + "  •  Doors " + next.getDoorsLabel(),
                    "#64748b", 10f));
        } else {
            text.add(sideText("No events are scheduled here yet.", "#64748b", 11f));
        }
        text.add(Box.createVerticalStrut(8));
        text.add(sideText("Booking closes " + (next == null ? "\u2014"
                : next.getBookingDeadlineLabel()) + ". Six seats per reservation, and no "
                + "limit on how many reservations one person may hold.", "#64748b", 10f));
        return text;
    }

    /** Capacity, shape, venue type and price span, under the picture. */
    private JPanel buildQuickFacts(Stadium stadium, StadiumDetails details) {
        JPanel facts = new JPanel(new GridLayout(2, 2, 16, 8));
        facts.setOpaque(false);
        facts.add(factTile("Capacity", StadiumPhotoPanel.compact(stadium.getCapacity()),
                "published figure"));
        facts.add(factTile("Shape", stadium.getShapeLabel().replace(" stadium", ""),
                stadium.getShape() == StadiumShape.BOX ? "four straight stands" : "continuous bowl"));
        facts.add(factTile("Seat price", details.getPriceSpan(), "plus "
                + BookingService.formatMoney(details.getBookingFee()) + " fee"));
        facts.add(factTile("On sale now", details.getEventCount() + " events",
                String.format(Locale.US, "%.2f%% vacant", details.getVacancyPercentage())));
        return facts;
    }

    private JPanel factTile(String heading, String value, String note) {
        JPanel tile = new JPanel(new GridBagLayout());
        tile.setOpaque(false);
        tile.setBorder(new EmptyBorder(4, 0, 4, 0));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        JLabel label = new JLabel(heading.toUpperCase(Locale.ENGLISH));
        label.setForeground(MUTED);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 9f));
        tile.add(label, constraints);
        constraints.gridy = 1;
        constraints.insets = new Insets(2, 0, 0, 0);
        JLabel amount = new JLabel(value);
        amount.setForeground(TEXT);
        amount.setFont(amount.getFont().deriveFont(Font.BOLD, 13f));
        tile.add(amount, constraints);
        constraints.gridy = 2;
        constraints.insets = new Insets(1, 0, 0, 0);
        JLabel sub = new JLabel(note);
        sub.setForeground(MUTED);
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 9f));
        tile.add(sub, constraints);
        return tile;
    }

    private JPanel buildAboutCard(Stadium stadium) {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(12, 14, 12, 14)));

        JLabel title = new JLabel("About this venue");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        card.add(title, BorderLayout.NORTH);

        // One HTML document rather than a stack of labels: the wrapping, the
        // alignment and the column widths are then the editor pane's problem and
        // not a row of panels fighting over a fixed height.
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:SansSerif; margin:0;'>");
        detailRow(html, "Where", stadium.getAddress());
        detailRow(html, "City", stadium.getCity() + ", " + stadium.getCountry());
        detailRow(html, "Type", stadium.getVenueType());
        detailRow(html, "Footprint", stadium.getShapeLabel());
        detailRow(html, "Seats on sale", String.valueOf(stadium.getSeatCount()));
        detailRow(html, "Sections", stadium.getSections().size() + " independent");
        html.append("<p style='margin-top:10px; color:#1e293b; font-size:11px;'>")
                .append(stadium.getDescription()).append("</p>");
        html.append("</body></html>");
        card.add(htmlScroll(html.toString(), 11f), BorderLayout.CENTER);
        return card;
    }

    /** One label-and-value line in the About card. */
    private void detailRow(StringBuilder html, String heading, String value) {
        html.append("<div style='margin-bottom:5px;'>")
                .append("<span style='color:#64748b; font-size:10px;'>").append(heading)
                .append("</span><br>")
                .append("<span style='color:#1e293b; font-size:11px;'>").append(value)
                .append("</span></div>");
    }

    /**
     * A read-only HTML pane that wraps its content to the width it is given.
     * Used instead of stacked labels, which is what was clipping the text.
     */
    private JComponent htmlScroll(String html, float baseSize) {
        JEditorPane pane = new JEditorPane("text/html", html);
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setFont(pane.getFont().deriveFont(Font.PLAIN, baseSize));
        JScrollPane scroll = new JScrollPane(pane);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getHorizontalScrollBar().setVisible(false);
        // An HTML pane opens showing the bottom of its document, which hid the
        // first lines of the About card. Pin it back to the top once sized.
        pane.setCaretPosition(0);
        SwingUtilities.invokeLater(() -> {
            pane.setCaretPosition(0);
            scroll.getViewport().setViewPosition(new java.awt.Point(0, 0));
        });
        return scroll;
    }

    /**
     * What is on, availability and notices, in one scrolling column. Split across
     * two cards they were squeezed to nothing by the height left under the picture.
     */
    private JPanel buildSideCard(StadiumDetails details, Stadium stadium) {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(12, 14, 12, 14)));

        JLabel title = new JLabel("At this venue");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        card.add(title, BorderLayout.NORTH);

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:SansSerif; margin:0;'>");

        htmlHeading(html, "What is on here", details.getEventCount() + " upcoming");
        html.append("<div style='color:#64748b; font-size:9px; margin:-2px 0 4px 0;'>")
                .append(details.getGameCount()).append(" games, ")
                .append(details.getConcertCount()).append(" concerts</div>");
        html.append("<table width='100%' cellpadding='0' cellspacing='0'>");
        for (StadiumEvent event : details.getEvents()) {
            boolean open = bookingService.isBookingOpen(event);
            html.append("<tr><td width='106' valign='top' style='color:#64748b; font-size:9px;'>")
                    .append(Messages.dateLabel(event.getDate())).append("</td>")
                    .append("<td valign='top' style='font-size:10px; color:")
                    .append(open ? "#1e293b" : "#94a3b8").append(";'>")
                    .append(event.getHeadline());
            if (!open) {
                html.append(" <span style='color:#b91c1c; font-weight:bold;'>closed</span>");
            }
            html.append("</td></tr>");
        }
        html.append("</table>");

        if (!details.getTeams().isEmpty()) {
            html.append("<p style='margin:8px 0 0 0; color:#64748b; font-size:10px;'>")
                    .append("<b>Clubs and teams:</b> ")
                    .append(joinNames(details.getTeams())).append("</p>");
        }
        if (!details.getArtists().isEmpty()) {
            html.append("<p style='margin:6px 0 0 0; color:#64748b; font-size:10px;'>")
                    .append("<b>Artists:</b> ")
                    .append(joinNames(details.getArtists())).append("</p>");
        }

        htmlHeading(html, "Availability",
                String.format(Locale.US, "%.2f%% vacant", details.getVacancyPercentage()));
        html.append("<div style='color:#475569; font-size:10px;'>")
                .append(String.format(Locale.US, "%s of %s seats sold across the %d events.",
                        String.valueOf(details.getSeatsBooked()),
                        String.valueOf(details.getSeatsOnSale()),
                        details.getEventCount())).append("</div>");
        if (details.getBlockedEvents().isEmpty()) {
            html.append("<div style='color:#15803d; font-size:10px;'>")
                    .append("Every event here is open for booking.</div>");
        } else {
            html.append("<div style='color:#b91c1c; font-size:10px;'><b>")
                    .append(details.getBlockedEvents().size())
                    .append(" event(s) cannot be booked</b> because of a cancellation or an ")
                    .append("emergency notice, and are marked on the schedule.</div>");
        }

        if (details.hasNotices()) {
            htmlHeading(html, "Notices", details.getNotices().size() + " posted");
            for (StadiumAnnouncement notice : details.getNotices()) {
                html.append("<div style='color:#475569; font-size:10px; margin-bottom:6px;'>")
                        .append("<b>").append(notice.getTitle()).append("</b><br>")
                        .append(notice.getMessage()).append("</div>");
            }
        }
        html.append("</body></html>");

        card.add(htmlScroll(html.toString(), 11f), BorderLayout.CENTER);

        JButton request = createSecondaryButton("Submit a special request");
        request.addActionListener(event -> showSpecialRequestDialog(stadium));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(request);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private void htmlHeading(StringBuilder html, String title, String note) {
        html.append("<div style='margin-top:12px; margin-bottom:2px;'>")
                .append("<span style='color:#1e293b; font-size:11px; font-weight:bold;'>")
                .append(title).append("</span> ")
                .append("<span style='color:#94a3b8; font-size:9px;'>").append(note)
                .append("</span></div>");
    }

    private String joinNames(List<String> names) {
        return String.join(", ", names);
    }

    /** The four sections with their real geometry and real prices. */
    private JPanel buildSectionsCard(StadiumDetails details) {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(12, 14, 12, 14)));

        JLabel title = new JLabel("Seating sections");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        JLabel note = new JLabel("Front rows are the dearest in every section.");
        note.setForeground(MUTED);
        note.setFont(note.getFont().deriveFont(Font.PLAIN, 10f));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(title, BorderLayout.NORTH);
        heading.add(note, BorderLayout.SOUTH);
        card.add(heading, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"", "Section", "Rows", "Seats per row", "Seats", "Price per seat"},
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        Stadium stadium = details.getStadium();
        Color accent = StadiumPhotoPanel.colorOf(stadium.getAccentColor(), BLUE);
        for (StadiumDetails.SectionFacts facts : details.getSections()) {
            model.addRow(new Object[]{"", facts.getId() + "  " + facts.getLabel(),
                    facts.getRows(), facts.getSeatsPerRow(), facts.getSeatCount(),
                    facts.getPriceRange()});
        }
        JTable table = new JTable(model);
        table.setBackground(WHITE);
        table.setForeground(TEXT);
        table.setRowHeight(30);
        table.setGridColor(new Color(235, 240, 247));
        table.getTableHeader().setBackground(SKY);
        table.getTableHeader().setForeground(BLUE_DARK);
        table.getTableHeader().setFont(table.getTableHeader().getFont()
                .deriveFont(Font.BOLD, 11f));
        int[] widths = {24, 138, 54, 92, 70, 196};
        for (int column = 0; column < widths.length; column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        }
        // A colour chip per section, matching the picture above it.
        table.getColumnModel().getColumn(0).setCellRenderer(
                new javax.swing.table.DefaultTableCellRenderer() {
                    @Override
                    public java.awt.Component getTableCellRendererComponent(JTable owner,
                                                                           Object value,
                                                                           boolean selected,
                                                                           boolean focused,
                                                                           int row,
                                                                           int column) {
                        java.awt.Component component =
                                super.getTableCellRendererComponent(owner, value, selected,
                                        focused, row, column);
                        component.setBackground(StadiumPhotoPanel.sectionColor(row, accent));
                        return component;
                    }
                });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        card.add(scroll, BorderLayout.CENTER);

        JLabel totals = new JLabel(String.format(Locale.US,
                "%s seats in total across four sections  •  booking fee %s per reservation",
                String.valueOf(stadium.getSeatCount()),
                BookingService.formatMoney(details.getBookingFee())));
        totals.setForeground(MUTED);
        totals.setFont(totals.getFont().deriveFont(Font.PLAIN, 10f));
        card.add(totals, BorderLayout.SOUTH);
        return card;
    }

    /** Shortens text that would run past the right edge. */
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

    /** A short wrapped line, pinned left, for the block beside the picture. */
    private JLabel sideText(String html, String colour, float size) {
        JLabel label = new JLabel("<html><div style='width:560px'>" + html + "</div></html>");
        label.setForeground(Color.decode(colour));
        label.setFont(label.getFont().deriveFont(Font.PLAIN, size));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /** The way through to the schedule and the seat map. */
    private JPanel buildDetailsActions(Stadium stadium) {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        actions.setOpaque(false);
        JButton schedule = createPrimaryButton("See the schedule and book seats");
        schedule.addActionListener(event -> openStadium(stadium));
        actions.add(schedule);
        JButton ledger = createSecondaryButton("See seats already booked here");
        ledger.addActionListener(event -> {
            selectedStadium = stadium;
            showSeatLedger();
        });
        actions.add(ledger);
        return actions;
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
        JButton back = createOutlineButton(Messages.get("common.allVenues"), accent);
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
        JLabel title = new JLabel(Messages.get("stadium.notices"));
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
        JButton request = createOutlineButton(Messages.get("stadium.specialRequest"), BLUE);
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

        String result = showDialogChoice(this, form,
                "Submit a special request to " + stadium.getName(),
                JOptionPane.PLAIN_MESSAGE, "Submit request", BACK_LABEL, "Submit request");
        if (!"Submit request".equals(result)) {
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
        JLabel title = new JLabel(Messages.get("stadium.upcoming"));
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
        headingCopy.add(title, constraints);
        JLabel subtitle = new JLabel(Messages.get("stadium.subtitle"));
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
        filters.add(buildSearchBar(eventSearchField, text("stadium.search.hint")), BorderLayout.CENTER);
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

    /** Shows how long the customer's held seats stay reserved for them. */
    private void updateHoldCountdown() {
        if (holdCountdownValue == null) {
            return;
        }
        List<Seat> selected = seatMapPanel.getSelectedSeats();
        if (selected.isEmpty()) {
            holdCountdownValue.setText("—");
            return;
        }
        long seconds = 0;
        for (Seat seat : selected) {
            seconds = Math.max(seconds, seatMapPanel.holdSecondsRemaining(seat.getKey()));
        }
        long minutes = seconds / 60;
        long remainder = seconds % 60;
        holdCountdownValue.setText(String.format(Locale.US, "%d:%02d", minutes, remainder));
        holdCountdownValue.setForeground(seconds < 60
                ? new Color(253, 186, 116) : new Color(253, 230, 138));
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
        return createEventCard(event, false);
    }

    /**
     * @param showVenue adds the venue name, needed when events from more than
     *                 one stadium are listed together
     */
    private JPanel createEventCard(StadiumEvent event, boolean showVenue) {
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
        String detailText = event.getEventDetails();
        Stadium venue = showVenue ? StadiumData.getStadium(event.getStadiumId()) : null;
        if (venue != null) {
            detailText = detailText + "  •  " + venue.getName() + ", " + venue.getCity();
        }
        JLabel eventDetails = new JLabel(detailText);
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
        choose.addActionListener(ignored -> {
            if (showVenue) {
                openEventFromSchedule(event);
            } else {
                openEvent(event);
            }
        });
        actionConstraints.gridy = 2;
        actionConstraints.insets = new Insets(7, 0, 0, 0);
        action.add(choose, actionConstraints);
        card.add(action, BorderLayout.EAST);
        return card;
    }

    // ---------------------------------------------------------------------
    // Live schedules (cross-venue)
    // ---------------------------------------------------------------------

    /**
     * Shows every upcoming game and concert across all venues, soonest first, so
     * a customer can find something to attend without choosing a stadium first.
     */
    private void showLiveSchedules() {
        currentScreen = "schedules";
        setHeader(text("nav.liveSchedules"), "Every upcoming game and concert across all eleven Ugandan venues.");
        contentHost.removeAll();
        contentHost.add(buildLiveSchedulesContent(), BorderLayout.CENTER);
        contentHost.revalidate();
        contentHost.repaint();
        loadLiveFilters();
        refreshLiveSchedules();
        showStatus("Live schedules");
    }

    private JPanel buildLiveSchedulesContent() {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setBackground(PAGE);
        page.setBorder(new EmptyBorder(20, 0, 22, 0));

        JPanel top = new JPanel(new BorderLayout(16, 0));
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
        JLabel title = new JLabel("All upcoming events");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 21f));
        titleConstraints.gridy = 1;
        titleConstraints.insets = new Insets(7, 0, 0, 0);
        titleBox.add(title, titleConstraints);
        JLabel subtitle = new JLabel("Soonest first, across every venue. Pick an event to choose seats.");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 11f));
        titleConstraints.gridy = 2;
        titleConstraints.insets = new Insets(3, 0, 0, 0);
        titleBox.add(subtitle, titleConstraints);
        top.add(titleBox, BorderLayout.WEST);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filters.setOpaque(false);
        JPanel searchWrap = new JPanel(new BorderLayout(0, 3));
        searchWrap.setOpaque(false);
        searchWrap.setPreferredSize(new Dimension(320, 56));
        searchWrap.add(buildSearchBar(liveSearchField, text("stadium.search.hint")),
                BorderLayout.CENTER);
        filters.add(searchWrap);
        liveDateCombo = new JComboBox<>();
        liveDateCombo.setPreferredSize(new Dimension(180, 38));
        liveDateCombo.setBackground(WHITE);
        liveDateCombo.addActionListener(event -> refreshLiveSchedules());
        liveTypeCombo = new JComboBox<>(new String[]{"All types", "Games", "Concerts"});
        liveTypeCombo.setPreferredSize(new Dimension(130, 38));
        liveTypeCombo.setBackground(WHITE);
        liveTypeCombo.addActionListener(event -> refreshLiveSchedules());
        filters.add(labelled("Date", liveDateCombo));
        filters.add(labelled("Type", liveTypeCombo));
        top.add(filters, BorderLayout.EAST);
        page.add(top, BorderLayout.NORTH);

        JPanel listCard = createCard();
        listCard.setLayout(new BorderLayout(0, 10));
        listCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(14, 14, 14, 14)));
        JPanel listHeader = new JPanel(new BorderLayout());
        listHeader.setOpaque(false);
        liveResultLabel = new JLabel("—");
        liveResultLabel.setForeground(MUTED);
        liveResultLabel.setFont(liveResultLabel.getFont().deriveFont(Font.PLAIN, 10f));
        listHeader.add(liveResultLabel, BorderLayout.WEST);
        listCard.add(listHeader, BorderLayout.NORTH);
        liveListHost = new JPanel(new BorderLayout());
        liveListHost.setOpaque(false);
        listCard.add(liveListHost, BorderLayout.CENTER);
        page.add(listCard, BorderLayout.CENTER);
        return page;
    }

    /** Fills the date filter with every date that has at least one event. */
    private void loadLiveFilters() {
        if (liveDateCombo == null) {
            return;
        }
        liveDateCombo.removeAllItems();
        liveDateCombo.addItem("All dates");
        List<LocalDate> dates = new ArrayList<>();
        for (StadiumEvent event : StadiumData.getEvents()) {
            if (!dates.contains(event.getDate())) {
                dates.add(event.getDate());
            }
        }
        java.util.Collections.sort(dates);
        for (LocalDate date : dates) {
            liveDateCombo.addItem(dayLabel(date));
        }
        liveDateCombo.setSelectedIndex(0);
    }

    /** "Today" / "Tomorrow" / "Sat, 3 Oct 2026". */
    private String dayLabel(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.equals(today)) {
            return "Today · " + date.format(DATE_FORMATTER);
        }
        if (date.equals(today.plusDays(1))) {
            return "Tomorrow · " + date.format(DATE_FORMATTER);
        }
        return date.format(DATE_FORMATTER);
    }

    private void refreshLiveSchedules() {
        if (liveListHost == null) {
            return;
        }
        String query = liveSearchField.getText().trim().toLowerCase(Locale.ENGLISH);
        int dateIndex = liveDateCombo == null ? 0 : liveDateCombo.getSelectedIndex();
        int typeIndex = liveTypeCombo == null ? 0 : liveTypeCombo.getSelectedIndex();
        LocalDate dateFilter = null;
        if (dateIndex > 0) {
            List<LocalDate> dates = new ArrayList<>();
            for (StadiumEvent event : StadiumData.getEvents()) {
                if (!dates.contains(event.getDate())) {
                    dates.add(event.getDate());
                }
            }
            java.util.Collections.sort(dates);
            dateFilter = dates.get(dateIndex - 1);
        }

        List<StadiumEvent> events = new ArrayList<>();
        for (StadiumEvent event : StadiumData.getEvents()) {
            if (dateFilter != null && !event.getDate().equals(dateFilter)) {
                continue;
            }
            if (typeIndex == 1 && !event.isGame()) {
                continue;
            }
            if (typeIndex == 2 && event.isGame()) {
                continue;
            }
            if (!query.isEmpty() && !matchesLiveQuery(event, query)) {
                continue;
            }
            events.add(event);
        }
        events.sort(java.util.Comparator.comparing(StadiumEvent::getDate)
                .thenComparing(StadiumEvent::getStartTime)
                .thenComparing(StadiumEvent::getId));

        countdownLabels.clear();
        eventActionButtons.clear();
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        if (events.isEmpty()) {
            list.add(buildEmptyState("No events match",
                    "Try a different date, type or search word."));
        } else {
            LocalDate current = null;
            for (StadiumEvent event : events) {
                if (!event.getDate().equals(current)) {
                    current = event.getDate();
                    list.add(buildDayHeader(current));
                    list.add(Box.createVerticalStrut(2));
                }
                list.add(createEventCard(event, true));
                list.add(Box.createVerticalStrut(10));
            }
        }
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        liveListHost.removeAll();
        liveListHost.add(scroll, BorderLayout.CENTER);
        liveResultLabel.setText(events.size() + " event" + (events.size() == 1 ? "" : "s")
                + " across " + StadiumData.getStadiums().size() + " venues"
                + (query.isEmpty() ? "" : "  •  matching \"" + liveSearchField.getText().trim() + "\""));
        liveListHost.revalidate();
        liveListHost.repaint();
    }

    private boolean matchesLiveQuery(StadiumEvent event, String query) {
        if (event.searchableText().contains(query)) {
            return true;
        }
        return venueMatches(event, query);
    }

    /**
     * Whether a venue should match a search word. Deliberately ignores the venue
     * description, which mentions words such as "Cranes" and would otherwise drag
     * in every event at that ground when the user searched for a team.
     */
    private boolean venueMatches(StadiumEvent event, String query) {
        Stadium stadium = StadiumData.getStadium(event.getStadiumId());
        if (stadium == null) {
            return false;
        }
        return (stadium.getName() + " " + stadium.getCity() + " " + stadium.getCountry()
                + " " + stadium.getVenueType()).toLowerCase(Locale.ENGLISH).contains(query);
    }

    private JPanel buildDayHeader(LocalDate date) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel label = new JLabel(dayLabel(date));
        label.setForeground(BLUE_DARK);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
        header.add(label);
        return header;
    }

    /** Opens an event picked from the cross-venue schedule. */
    private void openEventFromSchedule(StadiumEvent event) {
        Stadium stadium = StadiumData.getStadium(event.getStadiumId());
        if (stadium == null) {
            showWarning("That venue is no longer available.");
            return;
        }
        selectedStadium = stadium;
        openEvent(event);
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
        setHeader(Messages.get("booking.title"), selectedStadium.getName() + "  •  " + event.getHeadline());
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
        JButton back = createOutlineButton(Messages.get("common.backToSchedule"), accent);
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
        JLabel title = new JLabel(text("booking.yourDetails"));
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        heading.add(title, BorderLayout.WEST);
        JLabel hint = new JLabel(text("booking.threeDetails"));
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
        describe(field, labelText, describeField(labelText));
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
        JLabel title = new JLabel(text("booking.chooseSeats"));
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        heading.add(title, BorderLayout.WEST);
        JLabel hint = new JLabel(Messages.get("booking.limitHint"));
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
        JLabel title = new JLabel(text("booking.priceOutline"));
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        titleBox.add(title, titleConstraints);
        JLabel subtitle = new JLabel(Messages.get("booking.priceSubtitle"));
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 10f));
        titleConstraints.gridy = 1;
        titleConstraints.insets = new Insets(3, 0, 0, 0);
        titleBox.add(subtitle, titleConstraints);
        heading.add(titleBox, BorderLayout.WEST);
        JLabel feeHint = new JLabel(Messages.get("booking.feeHint"));
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
        JLabel note = new JLabel(Messages.get("booking.priceHint"));
        note.setForeground(MUTED);
        note.setFont(note.getFont().deriveFont(Font.PLAIN, 10f));
        actions.add(note, BorderLayout.CENTER);
        confirmBookingButton = createPrimaryButton(text("booking.confirm"));
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
        JLabel selectedHeading = new JLabel(Messages.get("booking.yourSelection"));
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
        JLabel totalHeading = new JLabel(text("booking.total"));
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
        JLabel availableHeading = new JLabel(text("booking.vacancy"));
        availableHeading.setForeground(new Color(170, 195, 229));
        availableHeading.setFont(availableHeading.getFont().deriveFont(Font.BOLD, 10f));
        availability.add(availableHeading, availabilityConstraints);
        bookingAvailabilityValue.setForeground(new Color(167, 243, 208));
        bookingAvailabilityValue.setFont(bookingAvailabilityValue.getFont().deriveFont(Font.BOLD, 16f));
        availabilityConstraints.gridy = 1;
        availabilityConstraints.insets = new Insets(4, 0, 0, 0);
        availability.add(bookingAvailabilityValue, availabilityConstraints);

        JPanel holds = new JPanel(new GridBagLayout());
        holds.setOpaque(false);
        holds.setPreferredSize(new Dimension(150, 52));
        GridBagConstraints holdConstraints = new GridBagConstraints();
        holdConstraints.anchor = GridBagConstraints.EAST;
        JLabel holdHeading = new JLabel(text("booking.seatsHeld"));
        holdHeading.setForeground(new Color(170, 195, 229));
        holdHeading.setFont(holdHeading.getFont().deriveFont(Font.BOLD, 10f));
        holds.add(holdHeading, holdConstraints);
        holdCountdownValue = new JLabel("—");
        holdCountdownValue.setForeground(new Color(253, 230, 138));
        holdCountdownValue.setFont(holdCountdownValue.getFont().deriveFont(Font.BOLD, 16f));
        holdConstraints.gridy = 1;
        holdConstraints.insets = new Insets(4, 0, 0, 0);
        holds.add(holdCountdownValue, holdConstraints);

        JPanel rightSide = new JPanel(new GridBagLayout());
        rightSide.setOpaque(false);
        GridBagConstraints rightConstraints = new GridBagConstraints();
        rightSide.add(availability, rightConstraints);
        rightConstraints.gridx = 1;
        rightSide.add(holds, rightConstraints);

        JPanel middle = new JPanel(new BorderLayout(15, 0));
        middle.setOpaque(false);
        middle.add(selection, BorderLayout.WEST);
        middle.add(total, BorderLayout.CENTER);
        middle.add(rightSide, BorderLayout.EAST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 3));
        actions.setOpaque(false);
        clearSelectionButton = createSecondaryButton(text("booking.clearSeats"));
        clearSelectionButton.addActionListener(event -> clearSelection());
        JButton viewBooked = createSecondaryButton(Messages.get("booking.viewBooked"));
        viewBooked.setToolTipText("See every seat already taken for this event");
        viewBooked.addActionListener(event -> showSeatLedger());
        actions.add(clearSelectionButton);
        actions.add(viewBooked);
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
            JLabel empty = new JLabel(Messages.get("booking.emptyPrice"));
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
        selectedSeatsValue.setText(selectedSeats.isEmpty()
                ? Messages.get("booking.noSeats") : joinSeatNames(selectedSeats));
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
        updateHoldCountdown();
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
        // A reservation is spread across the four sections so one booking never
        // takes every seat from a single stand. The dialog shows the final seats.
        List<Seat> spreadSeats = bookingService.allocateSpreadSeats(selectedSeats);
        boolean spread = !sameSeats(spreadSeats, selectedSeats);
        String choice = showDialogChoice(this,
                "Confirm " + spreadSeats.size() + " seat" + (spreadSeats.size() == 1 ? "" : "s")
                        + " for " + currency(bookingService.getTotalCharge(spreadSeats)) + "?\n\n"
                        + "Seats: " + joinSeatNames(spreadSeats) + "\n"
                        + sectionBreakdown(spreadSeats) + "\n"
                        + (spread ? "Your seats are spread across the sections.\n\n" : "")
                        + "Seat subtotal: " + currency(bookingService.totalFor(spreadSeats))
                        + "  •  Booking fee: " + currency(bookingService.getBookingFee()) + "\n"
                        + selectedEvent.getHeadline() + "  •  " + selectedEvent.getWhenLabel(),
                "Confirm booked seats", JOptionPane.QUESTION_MESSAGE,
                "Confirm booked seats", BACK_LABEL, "Confirm booked seats");
        if (!"Confirm booked seats".equals(choice)) {
            return;
        }
        try {
            Booking booking = bookingService.book(nameField.getText(), emailField.getText(),
                    phoneField.getText(), spreadSeats);
            seatMapPanel.getHoldService().releaseAllFor(sessionOwner);
            seatMapPanel.clearSelection();
            seatMapPanel.refreshStatuses();
            clearContactFields();
            updateBookingSummary();
            showStatus("Seat" + (booking.getSeats().size() == 1 ? "" : "s") + " booked: "
                    + booking.getSeatDisplay());
            settleAndIssueTicket(booking, selectedEvent);
        } catch (IllegalArgumentException exception) {
            showWarning(exception.getMessage());
        }
    }

    private boolean sameSeats(List<Seat> first, List<Seat> second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int index = 0; index < first.size(); index++) {
            if (!first.get(index).getKey().equals(second.get(index).getKey())) {
                return false;
            }
        }
        return true;
    }

    /** Lists how the chosen seats divide up between sections A to D. */
    private String sectionBreakdown(List<Seat> seats) {
        Map<String, Integer> perSection = new LinkedHashMap<>();
        for (Seat seat : seats) {
            perSection.merge(seat.getKey().getSection(), 1, Integer::sum);
        }
        if (perSection.size() <= 1) {
            return "";
        }
        StringBuilder text = new StringBuilder("By section:  ");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : perSection.entrySet()) {
            if (!first) {
                text.append("   ");
            }
            text.append(entry.getKey()).append(' ').append(entry.getValue());
            first = false;
        }
        return text.toString();
    }

    /**
     * Asks how the booking will be paid for, then settles it and shows the ticket.
     * Cash at the venue needs nothing extra; mobile money is clearly marked as a
     * simulation because no provider credentials ship with this build.
     */
    private void settleAndIssueTicket(Booking booking, StadiumEvent event) {
        JComboBox<PaymentRecord.Method> methods =
                new JComboBox<>(PaymentRecord.Method.values());
        methods.setSelectedItem(PaymentRecord.Method.CASH_AT_VENUE);
        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.add(new JLabel(Messages.get("payment.question")), BorderLayout.NORTH);
        form.add(methods, BorderLayout.CENTER);
        JLabel note = new JLabel(Messages.get("common.totalDue") + ": " + currency(booking.getTotal())
                + (PaymentRecord.isRealProviderConfigured()
                ? "" : "  •  mobile money is simulated, no money moves"));
        note.setForeground(MUTED);
        note.setFont(note.getFont().deriveFont(Font.PLAIN, 10f));
        form.add(note, BorderLayout.SOUTH);

        String choice = showDialogChoice(this, form, "Payment",
                JOptionPane.PLAIN_MESSAGE, "Pay", "Pay", BACK_LABEL);
        if (!"Pay".equals(choice)) {
            return;
        }
        PaymentRecord.Method method = (PaymentRecord.Method) methods.getSelectedItem();
        PaymentRecord payment = method == PaymentRecord.Method.MOBILE_MONEY
                ? PaymentRecord.simulatedMobileMoney(booking.getTotal(), phoneField.getText())
                : PaymentRecord.cashAtVenue(booking.getTotal());

        showStatus("Booking " + booking.getReference() + " • " + payment.describe());
        showBookingTicket(booking, event, payment);
        refreshBookings();
    }

    private void showBookingTicket(Booking booking, StadiumEvent event, PaymentRecord payment) {
        Stadium stadium = StadiumData.getStadium(booking.getStadiumId());
        JTextArea ticket = new JTextArea(
                TicketBuilder.text(booking, stadium, event) + "\n  Payment: " + payment.describe() + "\n",
                24, 58);
        ticket.setEditable(false);
        ticket.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        ticket.setCaretPosition(0);
        ticket.setBackground(new Color(250, 252, 255));

        String choice = showDialogChoice(this, new JScrollPane(ticket),
                "Ticket " + booking.getReference(), JOptionPane.INFORMATION_MESSAGE,
                "Save as text", "Save as text", "Print", BACK_LABEL);
        if ("Save as text".equals(choice)) {
            saveTicketToFile(booking, ticket.getText());
        } else if ("Print".equals(choice)) {
            printTicket(booking, ticket.getText());
        }
    }

    private void saveTicketToFile(Booking booking, String contents) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("ticket-" + booking.getReference() + ".txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            java.nio.file.Files.writeString(chooser.getSelectedFile().toPath(), contents);
            showStatus("Ticket saved to " + chooser.getSelectedFile().getName());
        } catch (java.io.IOException exception) {
            showWarning("The ticket could not be saved: " + exception.getMessage());
        }
    }

    private void printTicket(Booking booking, String contents) {
        java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
        job.setJobName("Ticket " + booking.getReference());
        job.setPrintable(new java.awt.print.Printable() {
            @Override
            public int print(java.awt.Graphics graphics, java.awt.print.PageFormat format,
                             int pageIndex) {
                if (pageIndex > 0) {
                    return NO_SUCH_PAGE;
                }
                Graphics2D copy = (Graphics2D) graphics;
                copy.translate(format.getImageableX(), format.getImageableY());
                java.util.List<String> lines = java.util.Arrays.asList(contents.split("\n"));
                java.awt.FontMetrics metrics = copy.getFontMetrics();
                int y = 0;
                for (String line : lines) {
                    copy.drawString(line, 0, y);
                    y += metrics.getHeight();
                }
                return PAGE_EXISTS;
            }
        });
        if (job.printDialog()) {
            showStatus("Ticket sent to the printer");
        }
    }

    /** Writes every booking to a CSV file the user chooses. */
    private void exportBookingsToCsv() {
        List<Booking> bookings = bookingService.getBookings();
        if (bookings.isEmpty()) {
            showWarning("There are no bookings to export yet.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("bookings.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            java.nio.file.Files.writeString(chooser.getSelectedFile().toPath(),
                    TicketBuilder.csv(bookings));
            showStatus("Exported " + bookings.size() + " bookings to "
                    + chooser.getSelectedFile().getName());
        } catch (java.io.IOException exception) {
            showWarning("The export could not be written: " + exception.getMessage());
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
        if (!promptForSignIn()) {
            showStatus("Sign in to view bookings");
            return;
        }
        currentScreen = "directory";
        bookingSearchField.setText("");
        currentScreen = "bookings";
        setHeader(text("nav.bookings"), "Review, search and cancel reservations for every stadium and event.");
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
        JLabel title = new JLabel(text("bookings.title"));
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 21f));
        titleConstraints.gridy = 1;
        titleConstraints.insets = new Insets(7, 0, 0, 0);
        titleBox.add(title, titleConstraints);
        JLabel subtitle = new JLabel(Messages.get("bookings.subtitle"));
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
        search.add(buildSearchBar(bookingSearchField, text("bookings.search.hint")), BorderLayout.CENTER);
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
        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);
        JLabel tableHint = new JLabel(Messages.get("bookings.hint"));
        tableHint.setForeground(MUTED);
        tableHint.setFont(tableHint.getFont().deriveFont(Font.PLAIN, 10f));
        tableHeader.add(tableHint, BorderLayout.WEST);
        tableCard.add(tableHeader, BorderLayout.NORTH);
        configureBookingTable();
        JScrollPane tableScroll = new JScrollPane(bookingTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(WHITE);
        tableScroll.getVerticalScrollBar().setUnitIncrement(18);
        tableCard.add(tableScroll, BorderLayout.CENTER);
        page.add(tableCard, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottom.setOpaque(false);
        JButton refresh = createSecondaryButton(text("bookings.refresh"));
        refresh.addActionListener(event -> refreshBookings());
        JButton export = createSecondaryButton(text("bookings.export"));
        export.setToolTipText("Write every booking to a spreadsheet file");
        export.addActionListener(event -> exportBookingsToCsv());
        cancelBookingButton = createSecondaryButton(Messages.get("bookings.cancelSelected"), new Color(185, 28, 28));
        cancelBookingButton.addActionListener(event -> cancelSelectedBooking());
        bottom.add(refresh);
        bottom.add(export);
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

    private void showBookingDetails(int row) {
        if (row < 0 || row >= bookingTableModel.getRowCount()) {
            return;
        }
        String reference = String.valueOf(bookingTableModel.getValueAt(row, 0));
        Booking booking = null;
        for (Booking candidate : bookingService.getBookings()) {
            if (candidate.getReference().equals(reference)) {
                booking = candidate;
                break;
            }
        }
        if (booking == null) {
            return;
        }

        Stadium stadium = StadiumData.getStadium(booking.getStadiumId());
        StadiumEvent event = StadiumData.getEvent(booking.getEventId());
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(8, 10, 8, 10));

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("Booking " + booking.getReference());
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        header.add(title, BorderLayout.WEST);
        JLabel status = new JLabel(booking.getStatus().name());
        status.setForeground(booking.isConfirmed() ? SUCCESS_DARK : CANCELLED);
        status.setFont(status.getFont().deriveFont(Font.BOLD, 11f));
        header.add(status, BorderLayout.EAST);
        content.add(header);
        content.add(Box.createVerticalStrut(6));

        addDetailSection(content, "BOOKING");
        addDetailRow(content, "Reference", booking.getReference());
        addDetailRow(content, "Status", booking.getStatus().name());
        addDetailRow(content, "Created", CREATED_FORMATTER.format(booking.getCreatedAt()));

        addDetailSection(content, "STADIUM");
        addDetailRow(content, "Venue", stadium == null ? "Legacy venue" : stadium.getName());
        addDetailRow(content, "Location", stadium == null ? "—" : stadium.getLocation());
        addDetailRow(content, "Address", stadium == null ? "—" : stadium.getAddress());
        addDetailRow(content, "Capacity", stadium == null ? "—"
                : formatCapacity(stadium.getSeatCount()) + " seats");

        addDetailSection(content, "EVENT");
        addDetailRow(content, "Event", event == null ? booking.getEvent() : event.getHeadline());
        addDetailRow(content, "Type", event == null ? "—" : event.getType().getLabel());
        addDetailRow(content, "Details", event == null ? "—" : event.getEventDetails());
        addDetailRow(content, "Date and time", bookingWhenLabel(booking, event));
        addDetailRow(content, "Doors", event == null ? "—" : event.getDoorsLabel());
        addDetailRow(content, "Booking deadline", event == null ? "—"
                : event.getBookingDeadlineLabel());

        addDetailSection(content, "BOOKED BY");
        addDetailRow(content, "Full name", maskedName(booking));
        addDetailRow(content, "Contact", maskedContact(booking));

        addDetailSection(content, "SEATS AND PAYMENT");
        addDetailRow(content, "Booked seats", booking.getSeatDisplay());
        addDetailRow(content, "Seat count", String.valueOf(booking.getSeats().size()));
        double seatSubtotal = Math.max(0.0, booking.getTotal() - BookingService.BOOKING_FEE);
        addDetailRow(content, "Seat subtotal", currency(seatSubtotal));
        addDetailRow(content, "Booking fee", currency(BookingService.BOOKING_FEE));
        addDetailRow(content, "Total charged", currency(booking.getTotal()));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(WHITE);
        scroll.setPreferredSize(new Dimension(650, 620));
        showDialogChoice(this, scroll, "Complete booking details",
                JOptionPane.INFORMATION_MESSAGE, BACK_LABEL, BACK_LABEL);
    }

    private void addDetailSection(JPanel parent, String title) {
        JLabel section = new JLabel(title);
        section.setForeground(BLUE_DARK);
        section.setFont(section.getFont().deriveFont(Font.BOLD, 10f));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setBorder(new EmptyBorder(10, 0, 4, 0));
        parent.add(section);
    }

    private void addDetailRow(JPanel parent, String label, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        JLabel key = new JLabel(label);
        key.setForeground(MUTED);
        key.setFont(key.getFont().deriveFont(Font.BOLD, 10f));
        key.setPreferredSize(new Dimension(145, 25));
        JLabel content = new JLabel("<html><div style='width:330px'>"
                + htmlText(value == null ? "—" : value) + "</div></html>");
        content.setForeground(TEXT);
        content.setFont(content.getFont().deriveFont(Font.PLAIN, 11f));
        row.add(key, BorderLayout.WEST);
        row.add(content, BorderLayout.CENTER);
        parent.add(row);
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
        String choice = showDialogChoice(this,
                "Cancel booking " + reference + "?\n\nThe seats will become available again.",
                "Cancel booking", JOptionPane.WARNING_MESSAGE,
                "Cancel booking", BACK_LABEL, "Cancel booking");
        if (!"Cancel booking".equals(choice)) {
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
    // Occupancy report
    // ---------------------------------------------------------------------

    /**
     * Reports how full each venue is, section by section, across every event.
     * This is the figure a venue manager actually needs, so it is computed from
     * the database rather than from the current screen.
     */
    private void showOccupancyReport() {
        if (!promptForSignIn()) {
            showStatus("Sign in to view the occupancy report");
            return;
        }
        currentScreen = "occupancy";
        setHeader(Messages.get("occupancy.title"), Messages.get("occupancy.subtitle"));
        contentHost.removeAll();
        contentHost.add(buildOccupancyContent(), BorderLayout.CENTER);
        contentHost.revalidate();
        contentHost.repaint();
        showStatus("Occupancy report");
    }

    private JPanel buildOccupancyContent() {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setBackground(PAGE);
        page.setBorder(new EmptyBorder(20, 0, 22, 0));

        JPanel top = new JPanel(new BorderLayout(16, 0));
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
        JLabel title = new JLabel(Messages.get("occupancy.allVenues"));
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 21f));
        titleConstraints.gridy = 1;
        titleConstraints.insets = new Insets(7, 0, 0, 0);
        titleBox.add(title, titleConstraints);
        JLabel subtitle = new JLabel(Messages.get("occupancy.note"));
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 11f));
        titleConstraints.gridy = 2;
        titleConstraints.insets = new Insets(3, 0, 0, 0);
        titleBox.add(subtitle, titleConstraints);
        top.add(titleBox, BorderLayout.WEST);
        page.add(top, BorderLayout.NORTH);

        JPanel tableCard = createCard();
        tableCard.setLayout(new BorderLayout(0, 10));
        tableCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(14, 14, 14, 14)));

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Venue", "City", "Events", "Capacity", "Seats booked",
                        "Vacancy", "A", "B", "C", "D"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setBackground(WHITE);
        table.setForeground(TEXT);
        table.setRowHeight(30);
        table.setGridColor(new Color(235, 240, 247));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setBackground(SKY);
        table.getTableHeader().setForeground(BLUE_DARK);
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD, 11f));
        int[] widths = {230, 110, 70, 100, 110, 90, 70, 70, 70, 70};
        for (int column = 0; column < widths.length; column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        }

        // Capacity is counted across every event at a venue, so a ground staging
        // eight matches is measured against eight times its seat count.
        OccupancyReport report = OccupancyReport.compute(bookingService);
        for (OccupancyReport.Row row : report.getRows()) {
            model.addRow(new Object[]{
                    row.getStadium().getName(), row.getStadium().getCity(), row.getEvents(),
                    formatCapacity(row.getSeatCapacity()), formatCapacity(row.getSeatsBooked()),
                    String.format(Locale.US, "%.2f%%", row.getVacancyPercentage()),
                    row.getSeatsInSection("A"), row.getSeatsInSection("B"),
                    row.getSeatsInSection("C"), row.getSeatsInSection("D")});
        }
        model.addRow(new Object[]{"All venues", "", report.getTotalEvents(),
                formatCapacity(report.getTotalCapacity()),
                formatCapacity(report.getTotalBooked()),
                String.format(Locale.US, "%.2f%%", report.getOverallVacancyPercentage()),
                "", "", "", ""});

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        tableCard.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottom.setOpaque(false);
        JButton export = createSecondaryButton(Messages.get("occupancy.export"));
        export.addActionListener(event -> exportOccupancyCsv());
        bottom.add(export);
        tableCard.add(bottom, BorderLayout.SOUTH);
        page.add(tableCard, BorderLayout.CENTER);
        return page;
    }

    private void exportOccupancyCsv() {
        StringBuilder sheet = new StringBuilder(
                "Venue,City,Events,Capacity,SeatsBooked,VacancyPercent\n");
        for (OccupancyReport.Row row : OccupancyReport.compute(bookingService).getRows()) {
            sheet.append('"').append(row.getStadium().getName()).append("\",")
                    .append('"').append(row.getStadium().getCity()).append("\",")
                    .append(row.getEvents()).append(',')
                    .append(row.getSeatCapacity()).append(',')
                    .append(row.getSeatsBooked()).append(',')
                    .append(String.format(Locale.US, "%.2f", row.getVacancyPercentage()))
                    .append('\n');
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("occupancy.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            java.nio.file.Files.writeString(chooser.getSelectedFile().toPath(), sheet.toString());
            showStatus("Occupancy report saved to " + chooser.getSelectedFile().getName());
        } catch (java.io.IOException exception) {
            showWarning("The report could not be saved: " + exception.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Booked seats ledger
    // ---------------------------------------------------------------------

    /**
     * Shows every seat already taken, per stadium and per event, so a customer
     * can see the occupancy of the venue they are booking on.
     */
    private void showSeatLedger() {
        if (!promptForSignIn()) {
            showStatus("Sign in to view booked seats");
            return;
        }
        Stadium target = selectedStadium != null ? selectedStadium : StadiumData.getStadium("namboole");
        StadiumEvent targetEvent = selectedEvent != null && target != null
                && target.getId().equals(selectedEvent.getStadiumId())
                ? selectedEvent : firstEventFor(target);
        ledgerReturnScreen = "booking".equals(currentScreen) ? "booking" : "directory";
        currentScreen = "seats";
        setHeader(text("nav.bookedSeats"), text("seats.subtitle"));
        contentHost.removeAll();
        contentHost.add(buildSeatLedgerContent(), BorderLayout.CENTER);
        contentHost.revalidate();
        contentHost.repaint();
        selectLedgerStadium(target);
        selectLedgerEvent(targetEvent);
        refreshSeatLedger();
        showStatus("Booked seats");
    }

    private StadiumEvent firstEventFor(Stadium stadium) {
        List<StadiumEvent> events = StadiumData.getEvents(stadium.getId());
        return events.isEmpty() ? StadiumData.getEvents().get(0) : events.get(0);
    }

    private JPanel buildSeatLedgerContent() {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setBackground(PAGE);
        page.setBorder(new EmptyBorder(20, 0, 22, 0));

        // BorderLayout has a single NORTH slot, so the title row and the summary
        // card are stacked in one wrapper instead of overlapping each other.
        JPanel north = new JPanel(new GridBagLayout());
        north.setOpaque(false);
        GridBagConstraints northConstraints = new GridBagConstraints();
        northConstraints.gridx = 0;
        northConstraints.gridy = 0;
        northConstraints.weightx = 1;
        northConstraints.fill = GridBagConstraints.HORIZONTAL;
        northConstraints.anchor = GridBagConstraints.NORTH;

        JPanel top = new JPanel(new BorderLayout(16, 0));
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
        JLabel title = new JLabel(text("seats.title"));
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 21f));
        titleConstraints.gridy = 1;
        titleConstraints.insets = new Insets(7, 0, 0, 0);
        titleBox.add(title, titleConstraints);
        JLabel subtitle = new JLabel(Messages.get("occupancy.vacancyNote"));
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 11f));
        titleConstraints.gridy = 2;
        titleConstraints.insets = new Insets(3, 0, 0, 0);
        titleBox.add(subtitle, titleConstraints);
        top.add(titleBox, BorderLayout.WEST);

        JPanel pickers = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pickers.setOpaque(false);
        ledgerStadiumCombo = new JComboBox<>();
        for (Stadium stadium : StadiumData.getStadiums()) {
            ledgerStadiumCombo.addItem(stadium.getName());
        }
        ledgerStadiumCombo.setPreferredSize(new Dimension(280, 38));
        ledgerStadiumCombo.setBackground(WHITE);
        styleCombo(ledgerStadiumCombo);
        ledgerStadiumCombo.addActionListener(event -> {
            Stadium stadium = StadiumData.getStadium(ledgerStadiumCombo.getSelectedIndex() >= 0
                    ? StadiumData.getStadiums().get(ledgerStadiumCombo.getSelectedIndex()).getId() : null);
            if (stadium != null) {
                loadLedgerEvents(stadium);
            }
        });
        ledgerEventCombo = new JComboBox<>();
        ledgerEventCombo.setPreferredSize(new Dimension(340, 38));
        ledgerEventCombo.setBackground(WHITE);
        styleCombo(ledgerEventCombo);
        ledgerEventCombo.addActionListener(event -> refreshSeatLedger());
        pickers.add(labelled("Stadium", ledgerStadiumCombo));
        pickers.add(labelled("Event", ledgerEventCombo));
        top.add(pickers, BorderLayout.EAST);
        north.add(top, northConstraints);

        JPanel summaryCard = createCard();
        summaryCard.setLayout(new BorderLayout(0, 8));
        summaryCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(14, 16, 14, 16)));
        ledgerSummaryLabel = new JLabel("—");
        ledgerSummaryLabel.setForeground(TEXT);
        ledgerSummaryLabel.setFont(ledgerSummaryLabel.getFont().deriveFont(Font.BOLD, 14f));
        ledgerSectionLabel = new JLabel("—");
        ledgerSectionLabel.setForeground(MUTED);
        ledgerSectionLabel.setFont(ledgerSectionLabel.getFont().deriveFont(Font.PLAIN, 11f));
        summaryCard.add(ledgerSummaryLabel, BorderLayout.NORTH);
        summaryCard.add(ledgerSectionLabel, BorderLayout.CENTER);
        northConstraints.gridy = 1;
        northConstraints.insets = new Insets(14, 0, 0, 0);
        north.add(summaryCard, northConstraints);
        page.add(north, BorderLayout.NORTH);

        ledgerTableModel = new DefaultTableModel(
                new Object[]{"Seat", "Section", "Row", "Number", "Price tier", "Price",
                        "Reference", "Booked by", "Booked on"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ledgerTable = new JTable(ledgerTableModel);
        configureLedgerTable();
        JScrollPane scroll = new JScrollPane(ledgerTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        JPanel tableCard = createCard();
        tableCard.setLayout(new BorderLayout(0, 10));
        tableCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(14, 14, 14, 14)));
        JLabel hint = new JLabel(Messages.get("seats.everyConfirmed"));
        hint.setForeground(MUTED);
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 10f));
        tableCard.add(hint, BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);
        page.add(tableCard, BorderLayout.CENTER);
        return page;
    }

    private JPanel labelled(String text, JComponent field) {
        JPanel box = new JPanel(new BorderLayout(0, 3));
        box.setOpaque(false);
        JLabel caption = new JLabel(text);
        caption.setForeground(MUTED);
        caption.setFont(caption.getFont().deriveFont(Font.BOLD, 10f));
        box.add(caption, BorderLayout.NORTH);
        box.add(field, BorderLayout.CENTER);
        return box;
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setFont(combo.getFont().deriveFont(Font.PLAIN, 11f));
        combo.setFocusable(false);
    }

    private void configureLedgerTable() {
        ledgerTable.setBackground(WHITE);
        ledgerTable.setForeground(TEXT);
        ledgerTable.setRowHeight(32);
        ledgerTable.setShowVerticalLines(false);
        ledgerTable.setGridColor(new Color(235, 240, 247));
        ledgerTable.setIntercellSpacing(new Dimension(0, 1));
        ledgerTable.setSelectionBackground(new Color(219, 234, 254));
        ledgerTable.setSelectionForeground(TEXT);
        ledgerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ledgerTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        ledgerTable.setFillsViewportHeight(true);
        JTableHeader header = ledgerTable.getTableHeader();
        header.setBackground(SKY);
        header.setForeground(BLUE_DARK);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 11f));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(100, 38));
        int[] widths = {95, 135, 60, 80, 120, 120, 100, 150, 150};
        for (int column = 0; column < widths.length; column++) {
            ledgerTable.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        }
    }

    private void selectLedgerStadium(Stadium stadium) {
        if (ledgerStadiumCombo == null || stadium == null) {
            return;
        }
        List<Stadium> stadiums = StadiumData.getStadiums();
        int index = stadiums.indexOf(stadium);
        if (index >= 0) {
            ledgerStadiumCombo.setSelectedIndex(index);
            loadLedgerEvents(stadium);
        }
    }

    private void loadLedgerEvents(Stadium stadium) {
        if (ledgerEventCombo == null) {
            return;
        }
        int previousIndex = ledgerEventCombo.getSelectedIndex();
        String previousId = previousIndex >= 0 && previousIndex < ledgerEventOptions.size()
                ? ledgerEventOptions.get(previousIndex).getId() : null;
        ledgerEventCombo.removeAllItems();
        ledgerEventOptions = new ArrayList<>();
        for (StadiumEvent event : StadiumData.getEvents(stadium.getId())) {
            ledgerEventOptions.add(event);
            ledgerEventCombo.addItem(ledgerEventLabel(event));
        }
        int restore = -1;
        for (int index = 0; index < ledgerEventOptions.size(); index++) {
            if (ledgerEventOptions.get(index).getId().equals(previousId)) {
                restore = index;
                break;
            }
        }
        if (restore >= 0) {
            ledgerEventCombo.setSelectedIndex(restore);
        } else if (ledgerEventCombo.getItemCount() > 0) {
            ledgerEventCombo.setSelectedIndex(0);
        }
    }

    private String ledgerEventLabel(StadiumEvent event) {
        return event.getHeadline() + "  •  " + event.getWhenLabel();
    }

    private void selectLedgerEvent(StadiumEvent event) {
        if (ledgerEventCombo == null || event == null) {
            return;
        }
        for (int index = 0; index < ledgerEventOptions.size(); index++) {
            if (event.getId().equals(ledgerEventOptions.get(index).getId())) {
                ledgerEventCombo.setSelectedIndex(index);
                return;
            }
        }
    }

    /** Rebuilds the ledger table and its summary for the chosen stadium and event. */
    private void refreshSeatLedger() {
        if (ledgerTableModel == null || ledgerStadiumCombo == null) {
            return;
        }
        int stadiumIndex = ledgerStadiumCombo.getSelectedIndex();
        if (stadiumIndex < 0) {
            return;
        }
        Stadium stadium = StadiumData.getStadiums().get(stadiumIndex);
        int eventIndex = ledgerEventCombo == null ? -1 : ledgerEventCombo.getSelectedIndex();
        StadiumEvent event = eventIndex >= 0 && eventIndex < ledgerEventOptions.size()
                ? ledgerEventOptions.get(eventIndex) : null;

        Map<SeatKey, Booking> owner = new LinkedHashMap<>();
        for (Booking booking : bookingService.getBookingsForEvent(event)) {
            for (SeatKey key : booking.getSeats()) {
                owner.putIfAbsent(key, booking);
            }
        }
        List<SeatKey> booked = new ArrayList<>(bookingService.getBookedSeatKeys(event));
        booked.sort(SeatKey::compareTo);

        int total = bookingService.getTotalSeatCount(event);
        ledgerTableModel.setRowCount(0);
        Map<String, Integer> perSection = new LinkedHashMap<>();
        for (SeatKey key : booked) {
            Booking booking = owner.get(key);
            Stadium venue = StadiumData.getStadium(stadium.getId());
            SeatSection section = venue == null ? null : venue.getSection(key.getSection());
            perSection.merge(key.getSection(), 1, Integer::sum);
            ledgerTableModel.addRow(new Object[]{
                    key.display(),
                    section == null ? key.getSection() : section.getLabel(),
                    key.getRow(),
                    key.getNumber(),
                    bookingService.getRowTierName(key.getRow(),
                            section == null ? 1 : section.getRows()),
                    currency(bookingService.getSeatPrice(event, key)),
                    booking == null ? "—" : booking.getReference(),
                    maskedName(booking),
                    booking == null ? "—" : formatStamp(booking.getCreatedAt())
            });
        }

        double vacancy = bookingService.getVacancyPercentage(event);
        // Two decimals, so a nearly empty ground never reads as a flat "100.0%".
        String vacancyText = String.format(Locale.US, "%.2f", vacancy);
        if (vacancyText.endsWith("00")) {
            vacancyText = vacancyText.substring(0, vacancyText.length() - 3);
        }
        ledgerSummaryLabel.setText(booked.size() + (booked.size() == 1 ? " seat" : " seats")
                + " booked of " + formatCapacity(total) + "  •  "
                + vacancyText + "% vacant  •  "
                + (event == null ? "—" : event.getHeadline()));
        StringBuilder sections = new StringBuilder("By section:  ");
        if (stadium != null) {
            for (SeatSection section : stadium.getSections()) {
                if (sections.length() > "By section:  ".length()) {
                    sections.append("     ");
                }
                sections.append(section.getId()).append(" ")
                        .append(section.getLabel()).append("  ")
                        .append(perSection.getOrDefault(section.getId(), 0))
                        .append('/').append(formatCapacity(section.getSeatCount()));
            }
        }
        ledgerSectionLabel.setText(sections.toString());
    }

    private String formatStamp(java.time.Instant instant) {
        if (instant == null) {
            return "—";
        }
        return java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    // ---------------------------------------------------------------------
    // Shared UI helpers
    // ---------------------------------------------------------------------

    private JPanel buildSearchBar(JTextField field, String hint) {
        styleSearchField(field);
        describe(field, "Search", "Type to search. Use the arrow keys and Enter to choose a suggestion.");
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

    private String describeField(String labelText) {
        switch (labelText) {
            case "Name":
                return "Full name of the person booking, at least two characters";
            case "Email":
                return "Email address used for the booking";
            case "Phone":
                return "Phone number, digits, spaces and an optional leading plus";
            default:
                return null;
        }
    }

    /** Gives a field a label a screen reader can announce. */
    private void describe(JComponent component, String name, String description) {
        component.getAccessibleContext().setAccessibleName(name);
        if (description != null) {
            component.getAccessibleContext().setAccessibleDescription(description);
        }
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

    /**
     * Button delegate that paints a flat fill taken straight from
     * {@link AbstractButton#getBackground()}.
     *
     * <p>The stock look-and-feel delegate repaints the button with its own
     * "pressed" colour, which overrode the background the application set and made
     * the pressed state impossible to control. Painting the fill here guarantees
     * the hover and pressed colours are exactly the ones requested.
     */
    private static final class FlatButtonUI extends BasicButtonUI {
        @Override
        public void paint(Graphics g, JComponent c) {
            if (!(c instanceof AbstractButton button)) {
                super.paint(g, c);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                boolean enabled = button.isEnabled();
                // Unfilled buttons (header navigation) keep the panel behind them.
                if (c.isOpaque()) {
                    g2.setColor(enabled ? button.getBackground()
                            : blend(button.getBackground(), PAGE, 0.62));
                    g2.fillRect(0, 0, c.getWidth(), c.getHeight());
                }
                String label = button.getText();
                if (label == null || label.isEmpty()) {
                    return;
                }
                Insets insets = c.getInsets();
                int availableWidth = c.getWidth() - insets.left - insets.right;
                int availableHeight = c.getHeight() - insets.top - insets.bottom;
                if (availableWidth <= 0 || availableHeight <= 0) {
                    return;
                }
                java.awt.FontMetrics metrics = g2.getFontMetrics(button.getFont());
                g2.setColor(enabled ? button.getForeground()
                        : blend(button.getForeground(), PAGE, 0.35));
                g2.drawString(label,
                        insets.left + (availableWidth - metrics.stringWidth(label)) / 2,
                        insets.top + (availableHeight + metrics.getAscent() - metrics.getDescent()) / 2);
            } finally {
                g2.dispose();
            }
        }
    }

    /** A {@link JButton} that keeps {@link FlatButtonUI} installed. */
    private static final class FeedbackButton extends JButton {
        FeedbackButton(String text) {
            super(text);
        }

        @Override
        public void updateUI() {
            if (!(getUI() instanceof FlatButtonUI)) {
                setUI(new FlatButtonUI());
            }
        }
    }

    /**
     * Blends {@code from} towards {@code to}. An {@code amount} of 0 keeps
     * {@code from} unchanged and 1 returns {@code to}.
     */
    private static Color blend(Color from, Color to, double amount) {
        double keep = 1.0 - amount;
        return new Color(
                (int) Math.round(from.getRed() * keep + to.getRed() * amount),
                (int) Math.round(from.getGreen() * keep + to.getGreen() * amount),
                (int) Math.round(from.getBlue() * keep + to.getBlue() * amount));
    }

    /**
     * Rebuilds a button border with a new outline colour while keeping the
     * original outline thickness and inner padding, so hovering never shifts
     * the layout.
     */
    private static Border recolourBorder(Border base, Color lineColor) {
        if (base instanceof CompoundBorder compound) {
            int thickness = compound.getOutsideBorder() instanceof LineBorder line
                    ? line.getThickness() : 1;
            return BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(lineColor, thickness), compound.getInsideBorder());
        }
        if (base instanceof LineBorder line) {
            return BorderFactory.createLineBorder(lineColor, line.getThickness());
        }
        return base;
    }

    /**
     * Gives a button clear hover, pressed and released feedback so it is always
     * obvious which control the pointer is over and which one is being clicked.
     *
     * <p>Feedback is built from the button's own colours so it works for every
     * style. Unfilled buttons (such as the header navigation) cannot show a
     * background change, so their outline and text brighten instead. The current
     * button label is also echoed into the status bar and the tooltip.
     */
    private void addInteractionFeedback(JButton button) {
        Color baseBackground = button.getBackground();
        Color baseForeground = button.getForeground();
        Border baseBorder = button.getBorder();
        boolean filled = button.isContentAreaFilled();

        Color hoverBackground = filled ? blend(baseBackground, WHITE, 0.16) : baseBackground;
        Color hoverForeground = filled ? baseForeground : blend(baseForeground, WHITE, 0.55);
        Color hoverOutline = blend(baseForeground, WHITE, 0.25);
        Color pressBackground = filled ? blend(baseBackground, Color.BLACK, 0.22) : baseBackground;
        Color pressForeground = filled ? baseForeground : blend(baseForeground, WHITE, 0.85);
        Color pressOutline = filled ? blend(baseForeground, WHITE, 0.75) : WHITE;

        String label = button.getText();
        if (button.getToolTipText() == null) {
            button.setToolTipText(label == null || label.isBlank() ? null : label.trim());
        }

        Runnable rest = () -> {
            button.setBackground(baseBackground);
            button.setForeground(baseForeground);
            button.setBorder(baseBorder);
        };
        Runnable hover = () -> {
            button.setBackground(hoverBackground);
            button.setForeground(hoverForeground);
            button.setBorder(recolourBorder(baseBorder, hoverOutline));
        };
        Runnable press = () -> {
            button.setBackground(pressBackground);
            button.setForeground(pressForeground);
            button.setBorder(recolourBorder(baseBorder, pressOutline));
        };

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (!button.isEnabled()) {
                    return;
                }
                hover.run();
                if (label != null && !label.isBlank()) {
                    showStatus("Hover: " + label.trim());
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                rest.run();
            }

            @Override
            public void mousePressed(MouseEvent event) {
                if (!button.isEnabled()) {
                    return;
                }
                press.run();
                button.repaint();
                if (label != null && !label.isBlank()) {
                    showStatus("Clicking: " + label.trim());
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (!button.isEnabled()) {
                    return;
                }
                hover.run();
                button.repaint();
            }
        });
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new FeedbackButton(text);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 11f));
        button.setForeground(WHITE);
        button.setBackground(BLUE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BLUE_DARK), new EmptyBorder(9, 14, 9, 14)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addInteractionFeedback(button);
        return button;
    }

    private JButton createSecondaryButton(String text) {
        return createSecondaryButton(text, TEXT);
    }

    private JButton createSecondaryButton(String text, Color foreground) {
        JButton button = new FeedbackButton(text);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 11f));
        button.setForeground(foreground);
        button.setBackground(WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 202, 218)), new EmptyBorder(9, 13, 9, 13)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addInteractionFeedback(button);
        return button;
    }

    private JButton createOutlineButton(String text, Color accent) {
        JButton button = new FeedbackButton(text);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 11f));
        button.setForeground(accent.darker());
        button.setBackground(WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent), new EmptyBorder(8, 12, 8, 12)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addInteractionFeedback(button);
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

    /**
     * Customer names are only shown in full to signed-in staff. Everyone else sees
     * a partial name so the bookings screen is still useful without exposing
     * contact details.
     */
    private String maskedName(Booking booking) {
        if (booking == null) {
            return "—";
        }
        if (staffSession.canViewCustomerDetails()) {
            return booking.getCustomerName();
        }
        String name = booking.getCustomerName() == null ? "" : booking.getCustomerName().trim();
        if (name.isEmpty()) {
            return "—";
        }
        String[] parts = name.split("\\s+");
        String first = parts[0];
        String initial = parts.length > 1 ? " " + parts[parts.length - 1].charAt(0) + "." : "";
        return first + initial;
    }

    private String maskedContact(Booking booking) {
        if (booking == null) {
            return "—";
        }
        if (staffSession.canViewCustomerDetails()) {
            return booking.getEmail() + "  •  " + booking.getPhone();
        }
        return "Sign in as staff to view contact details";
    }

    // ---------------------------------------------------------------------
    // Staff accounts
    // ---------------------------------------------------------------------

    /** The file the bookings and staff accounts are kept in. */
    private static java.nio.file.Path databaseFile() {
        return new java.io.File("stadium-bookings.dat").toPath();
    }

    /**
     * Opens the database, asking for the password first when the file is encrypted.
     *
     * <p>Whether a password is needed has to be read from a sidecar file, because an
     * encrypted database cannot be opened to report its own state.
     */
    private Database openDatabase() {
        java.nio.file.Path file = databaseFile();
        if (!Database.requiresPassword(file)) {
            return new Database(file);
        }
        JPasswordField field = new JPasswordField(16);
        field.setFont(field.getFont().deriveFont(Font.PLAIN, 13f));
        JPanel form = new JPanel(new BorderLayout(0, 8));
        JLabel prompt = new JLabel("This database is encrypted. Enter its password:");
        prompt.setForeground(TEXT);
        form.add(prompt, BorderLayout.NORTH);
        form.add(field, BorderLayout.CENTER);
        JLabel warning = new JLabel("The password cannot be recovered. Without it the "
                + "bookings cannot be opened.\nNote: this stops the file being opened, but "
                + "the customer details inside it are not scrambled.");
        warning.setForeground(new Color(185, 28, 28));
        warning.setFont(warning.getFont().deriveFont(Font.PLAIN, 10f));
        form.add(warning, BorderLayout.SOUTH);

        for (int attempt = 0; attempt < 3; attempt++) {
            String choice = showDialogChoice(this, form, "Database password",
                    JOptionPane.QUESTION_MESSAGE, "Unlock", "Unlock", "Exit");
            if (!"Unlock".equals(choice)) {
                System.exit(0);
            }
            char[] password = field.getPassword();
            field.setText("");
            try {
                Database opened = new Database(file, password);
                // Prove the password works before handing it to the rest of the app.
                try (java.sql.Connection connection = opened.open()) {
                    connection.isValid(2);
                } catch (java.sql.SQLException exception) {
                    java.util.Arrays.fill(password, '\0');
                    showWarning("That password did not unlock the database.");
                    continue;
                }
                return opened;
            } catch (java.io.IOException exception) {
                java.util.Arrays.fill(password, '\0');
                showWarning("That password did not unlock the database.");
            }
        }
        JOptionPane.showMessageDialog(this,
                "The database could not be opened. Please check the password and try again.",
                "Database locked", JOptionPane.ERROR_MESSAGE);
        System.exit(0);
        return null;
    }

    /**
     * Creates the first manager account on a fresh database.
     *
     * <p>The starting PIN is generated at random and shown once, so there is no
     * PIN in the source and no shared default that everyone knows. The account is
     * marked as needing a new PIN before it can be used.
     */
    private void runFirstRunSetup() {
        if (!staffDirectory.isEmpty()) {
            return;
        }
        JTextField name = new JTextField(18);
        name.setFont(name.getFont().deriveFont(Font.PLAIN, 13f));
        JTextField username = new JTextField(18);
        username.setFont(username.getFont().deriveFont(Font.PLAIN, 13f));
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(4, 0, 4, 10);
        JLabel heading = new JLabel("Create the first staff account");
        heading.setForeground(TEXT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 14f));
        constraints.gridwidth = 2;
        form.add(heading, constraints);
        constraints.gridwidth = 1;
        constraints.gridy = 1;
        form.add(new JLabel("Name"), constraints);
        constraints.gridx = 1;
        form.add(name, constraints);
        constraints.gridx = 0;
        constraints.gridy = 2;
        form.add(new JLabel("Username"), constraints);
        constraints.gridx = 1;
        form.add(username, constraints);
        JLabel hint = new JLabel("Letters, numbers, dot, dash or underscore. This account "
                + "manages staff and can see customer contact details.");
        hint.setForeground(MUTED);
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 10f));
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        form.add(hint, constraints);

        for (int attempt = 0; attempt < 5; attempt++) {
            String choice = showDialogChoice(this, form, "First run setup",
                    JOptionPane.PLAIN_MESSAGE, "Create", "Create", "Continue without an account");
            if (!"Create".equals(choice)) {
                showStatus("No staff account created. Bookings still work, but contact "
                        + "details stay hidden");
                return;
            }
            String refusal = staffDirectory.create(username.getText(), name.getText(),
                    StaffDirectory.startingPin(), StaffDirectory.Role.MANAGER, true);
            if (refusal == null) {
                showStartingPin(username.getText());
                return;
            }
            showWarning(refusal);
        }
    }

    /** Shows the generated PIN once, with instructions to change it. */
    private void showStartingPin(String username) {
        String pin = staffDirectory.lastCreatedPin(username);
        String message = "Your staff account is ready.\n\n"
                + "Username:  " + username + "\n"
                + "PIN:       " + pin + "\n\n"
                + "This PIN is shown once. You will be asked to change it the first "
                + "time you sign in. Write it down now.";
        String choice = showDialogChoice(this, message, "Your starting PIN",
                JOptionPane.INFORMATION_MESSAGE, "I have written it down", "I have written it down");
        staffDirectory.clearLastCreatedPin();
        showStatus("Staff account created for " + username);
        if (BACK_LABEL.equals(choice)) {
            showWarning("Your starting PIN is " + pin + " — write it down, it is not shown again.");
        }
    }

    /**
     * Asks for the staff username and PIN. Returns true when signed in; false means
     * the user chose to skip, which leaves the management screens closed.
     */
    private boolean promptForSignIn() {
        if (staffSession.isSignedIn()) {
            if (staffSession.mustChangePin() && !promptToChangePin("You must change your PIN "
                    + "before you can continue")) {
                return false;
            }
            return true;
        }
        JTextField username = new JTextField(14);
        username.setFont(username.getFont().deriveFont(Font.PLAIN, 13f));
        JPasswordField pin = new JPasswordField(10);
        pin.setFont(pin.getFont().deriveFont(Font.PLAIN, 13f));
        JPanel form = new JPanel(new BorderLayout(8, 6));
        form.add(new JLabel("Staff sign-in"), BorderLayout.NORTH);
        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(2, 0, 2, 8);
        fields.add(new JLabel("Username"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        fields.add(username, constraints);
        constraints.gridx = 0;
        constraints.gridy = 1;
        fields.add(new JLabel("PIN"), constraints);
        constraints.gridx = 1;
        fields.add(pin, constraints);
        form.add(fields, BorderLayout.CENTER);

        while (true) {
            String choice = showDialogChoice(this, form, "Staff sign-in",
                    JOptionPane.PLAIN_MESSAGE, "Sign in", "Sign in", "Continue without signing in");
            if (!"Sign in".equals(choice)) {
                return false;
            }
            String refusal = staffSession.signIn(username.getText(), new String(pin.getPassword()));
            pin.setText("");
            if (refusal == null) {
                showStatus("Signed in as " + staffSession.getDisplayName());
                updateStaffBadge();
                if (staffSession.mustChangePin()
                        && !promptToChangePin("Choose your own PIN before you continue")) {
                    staffSession.signOut();
                    updateStaffBadge();
                    return false;
                }
                return true;
            }
            showWarning(refusal);
        }
    }

    /**
     * Asks for a new PIN. Returns false when the user backs out, which for a
     * forced change means they cannot proceed.
     *
     * @param explanation why the change is being demanded
     */
    private boolean promptToChangePin(String explanation) {
        String username = staffSession.getUsername();
        JPasswordField current = new JPasswordField(10);
        JPasswordField replacement = new JPasswordField(10);
        JPasswordField confirm = new JPasswordField(10);
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(3, 0, 3, 10);
        constraints.gridwidth = 2;
        JLabel heading = new JLabel(explanation);
        heading.setForeground(TEXT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 13f));
        form.add(heading, constraints);
        constraints.gridwidth = 1;
        constraints.gridy = 1;
        form.add(new JLabel("Current PIN"), constraints);
        constraints.gridx = 1;
        form.add(current, constraints);
        constraints.gridx = 0;
        constraints.gridy = 2;
        form.add(new JLabel("New PIN"), constraints);
        constraints.gridx = 1;
        form.add(replacement, constraints);
        constraints.gridx = 0;
        constraints.gridy = 3;
        form.add(new JLabel("Confirm new PIN"), constraints);
        constraints.gridx = 1;
        form.add(confirm, constraints);
        JLabel rules = new JLabel("6 to 12 digits. Avoid 123456 and other easy guesses.");
        rules.setForeground(MUTED);
        rules.setFont(rules.getFont().deriveFont(Font.PLAIN, 10f));
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        form.add(rules, constraints);

        while (true) {
            String choice = showDialogChoice(this, form, "Change PIN",
                    JOptionPane.PLAIN_MESSAGE, "Change PIN", "Change PIN", BACK_LABEL);
            if (BACK_LABEL.equals(choice)) {
                current.setText("");
                replacement.setText("");
                confirm.setText("");
                return false;
            }
            String entered = new String(replacement.getPassword());
            if (!entered.equals(new String(confirm.getPassword()))) {
                showWarning("The two new PINs do not match.");
                continue;
            }
            String refusal = staffDirectory.changePin(username,
                    new String(current.getPassword()), entered);
            current.setText("");
            replacement.setText("");
            confirm.setText("");
            if (refusal == null) {
                showStatus("Your PIN has been changed");
                return true;
            }
            showWarning(refusal);
        }
    }

    /**
     * The Staff button: shows who is signed in and offers the actions available.
     */
    private void promptForStaffAction() {
        if (!staffSession.isSignedIn()) {
            if (!promptForSignIn()) {
                return;
            }
            return;
        }
        String name = staffSession.getDisplayName();
        String role = staffSession.getRole().getLabel();
        String choice = showDialogChoice(this,
                "Signed in as " + name + " (" + role + ")\n"
                        + (staffSession.canViewCustomerDetails()
                        ? "You can see customer contact details."
                        : "You cannot see customer contact details."),
                "Staff", JOptionPane.PLAIN_MESSAGE, "Choose",
                "Change my PIN", "Manage staff", "Access trail",
                staffSession.canViewCustomerDetails()
                        ? "Require a database password" : "Sign out",
                "Sign out");
        if ("Require a database password".equals(choice)) {
            protectDatabase();
            return;
        }
        if ("Change my PIN".equals(choice)) {
            promptToChangePin("Choose a new PIN");
        } else if ("Manage staff".equals(choice)) {
            showStaffManagement();
        } else if ("Access trail".equals(choice)) {
            showAccessTrail();
        } else if ("Sign out".equals(choice)) {
            signOutStaff();
        }
    }

    /** The access trail on its own, for a user who cannot manage accounts. */
    private void showAccessTrail() {
        currentScreen = "staff";
        setHeader("Access trail", "Every sign-in, refused attempt and PIN change.");
        contentHost.removeAll();
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setBackground(PAGE);
        page.setBorder(new EmptyBorder(20, 0, 22, 0));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        top.setOpaque(false);
        JButton back = createOutlineButton("\u2190 Back to stadiums", BLUE);
        back.addActionListener(event -> showStadiumDirectory());
        top.add(back);
        page.add(top, BorderLayout.NORTH);
        page.add(buildAccessLogCard(), BorderLayout.CENTER);
        contentHost.add(page, BorderLayout.CENTER);
        contentHost.revalidate();
        contentHost.repaint();
        showStatus("Access trail");
    }

    /**
     * Requires a password before the database can be opened.
     *
     * <p>The wording here is deliberately precise. It stops the file being opened by
     * anyone without the password, but this build's database does not encrypt the
     * contents, so the customer details inside remain readable to somebody with a
     * hex editor. Saying "encrypted" would be a claim the software cannot support.
     */
    private void protectDatabase() {
        DatabaseProtection protection = new DatabaseProtection(databaseFile());
        if (protection.isAlreadyProtected()) {
            showStatus("This database already needs a password");
            return;
        }
        JPasswordField first = new JPasswordField(16);
        JPasswordField second = new JPasswordField(16);
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(4, 0, 4, 10);
        form.add(new JLabel("New database password"), constraints);
        constraints.gridx = 1;
        form.add(first, constraints);
        constraints.gridx = 0;
        constraints.gridy = 1;
        form.add(new JLabel("Confirm password"), constraints);
        constraints.gridx = 1;
        form.add(second, constraints);
        JLabel warning = new JLabel("<html><div style='width:420px'>"
                + "<b>Read this before you continue.</b><br><br>"
                + "This will stop the booking file being opened without the password, so a "
                + "stray copy or a database tool cannot read it.<br><br>"
                + "It will <b>not</b> encrypt the contents: this build uses a database that "
                + "leaves the customer details readable in the file to anyone using a hex "
                + "editor.<br><br>"
                + "The password cannot be recovered. If you lose it, every booking and staff "
                + "account is gone. You will be asked for it every time you open the "
                + "application.</div></html>");
        warning.setForeground(new Color(146, 64, 14));
        warning.setFont(warning.getFont().deriveFont(Font.PLAIN, 10f));
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        form.add(warning, constraints);

        while (true) {
            String choice = showDialogChoice(this, form, "Require a database password",
                    JOptionPane.PLAIN_MESSAGE, "Protect", "Protect this database", BACK_LABEL);
            if (BACK_LABEL.equals(choice)) {
                return;
            }
            String entered = new String(first.getPassword());
            first.setText("");
            second.setText("");
            if (!entered.equals(new String(second.getPassword()))) {
                showWarning("The two passwords do not match.");
                continue;
            }
            if (entered.length() < 8) {
                showWarning("Use a password of at least 8 characters.");
                continue;
            }
            DatabaseProtection.Outcome outcome = protection.encrypt(entered.toCharArray());
            java.util.Arrays.fill(entered.toCharArray(), '\0');
            if (!outcome.isPerformed()) {
                showWarning(outcome.getProblem());
                return;
            }
            showStatus("The database now needs a password. It will be asked for next time.");
            showDialogChoice(this,
                    "The booking file now needs a password.\n\n"
                            + "You will be asked for it every time you open the application, "
                            + "because there is nowhere safe for it to be stored.\n\n"
                            + "It is not stored anywhere, so write it down. If you lose it, "
                            + "the bookings and staff accounts cannot be recovered.",
                    "Password required", JOptionPane.INFORMATION_MESSAGE,
                    "I have written it down", "I have written it down");
            return;
        }
    }

    /** Signs out and clears anything the previous user was allowed to see. */
    private void signOutStaff() {
        if (!staffSession.isSignedIn()) {
            return;
        }
        String name = staffSession.getDisplayName();
        staffSession.signOut();
        updateStaffBadge();
        showStatus("Signed out of " + name);
    }

    /** Reflects who is signed in, or that nobody is. */
    private void updateStaffBadge() {
        if (staffBadge == null) {
            return;
        }
        if (staffSession.isSignedIn()) {
            staffBadge.setText(staffSession.getDisplayName()
                    + "  •  " + staffSession.getRole().getLabel());
            staffBadge.setForeground(new Color(190, 242, 100));
        } else {
            staffBadge.setText("Not signed in");
            staffBadge.setForeground(new Color(191, 219, 254));
        }
    }

    /**
     * The staff screen: who has an account, add or disable one, reset a forgotten
     * PIN, and read the access trail.
     */
    private void showStaffManagement() {
        if (!promptForSignIn()) {
            return;
        }
        if (!staffSession.canViewReports()) {
            showWarning("Your role cannot manage staff accounts.");
            return;
        }
        currentScreen = "staff";
        setHeader("Staff", "Accounts, roles and the record of who has looked at customer details.");
        contentHost.removeAll();
        contentHost.add(buildStaffContent(), BorderLayout.CENTER);
        contentHost.revalidate();
        contentHost.repaint();
        showStatus("Staff accounts");
    }

    private JPanel buildStaffContent() {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setBackground(PAGE);
        page.setBorder(new EmptyBorder(20, 0, 22, 0));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        top.setOpaque(false);
        JButton back = createOutlineButton("← Back to stadiums", BLUE);
        back.addActionListener(event -> showStadiumDirectory());
        top.add(back);
        page.add(top, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(0, 2, 14, 14));
        cards.setOpaque(false);

        cards.add(buildStaffAccountsCard());
        cards.add(buildAccessLogCard());
        page.add(cards, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildStaffAccountsCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 10));
        JLabel title = new JLabel("Staff accounts");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        card.add(title, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Name", "Username", "Role", "State", "Last used", ""}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setBackground(WHITE);
        table.setForeground(TEXT);
        table.setRowHeight(30);
        table.setGridColor(new Color(235, 240, 247));
        table.getTableHeader().setBackground(SKY);
        table.getTableHeader().setForeground(BLUE_DARK);
        int[] widths = {170, 150, 110, 120, 130, 150};
        for (int column = 0; column < widths.length; column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        }

        List<StaffDirectory.Account> accounts = staffDirectory.list();
        for (StaffDirectory.Account account : accounts) {
            String state = !account.isActive() ? "Deactivated"
                    : account.isLocked() ? "Locked"
                    : account.mustChangePin() ? "Must change PIN"
                    : "Active";
            model.addRow(new Object[]{account.getDisplayName(), account.getUsername(),
                    account.getRole().getLabel(), state,
                    account.getLastUsedAt() == null ? "Never"
                            : java.time.format.DateTimeFormatter.ofPattern("d MMM HH:mm",
                                    java.util.Locale.ENGLISH)
                                    .withZone(java.time.ZoneId.systemDefault())
                                    .format(account.getLastUsedAt()),
                    account.getUsername()});
        }
        table.getColumnModel().getColumn(5).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(value == null ? "" : "Manage");
                setHorizontalAlignment(CENTER);
            }
        });
        // A cell editor cannot host a button, so the action is wired to clicks on
        // the last column the same way the booking rows are.
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() != 1) {
                    return;
                }
                int column = table.columnAtPoint(event.getPoint());
                int row = table.rowAtPoint(event.getPoint());
                if (column != 5 || row < 0) {
                    return;
                }
                Object username = model.getValueAt(row, 5);
                if (username != null) {
                    manageStaffAccount(String.valueOf(username));
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(WHITE);
        card.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton add = createPrimaryButton("Add staff account");
        add.addActionListener(event -> promptForNewStaff());
        actions.add(add);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildAccessLogCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 10));
        JLabel title = new JLabel("Access trail");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        JLabel subtitle = new JLabel("Every sign-in, refused attempt and PIN change.");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 10f));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(title, BorderLayout.NORTH);
        heading.add(subtitle, BorderLayout.SOUTH);
        card.add(heading, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"When", "Username", "Action", "Detail"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setBackground(WHITE);
        table.setForeground(TEXT);
        table.setRowHeight(28);
        table.setGridColor(new Color(235, 240, 247));
        table.getTableHeader().setBackground(SKY);
        table.getTableHeader().setForeground(BLUE_DARK);
        int[] widths = {140, 150, 200, 180};
        for (int column = 0; column < widths.length; column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        }
        java.time.format.DateTimeFormatter stamp = java.time.format.DateTimeFormatter
                .ofPattern("d MMM HH:mm:ss", java.util.Locale.ENGLISH)
                .withZone(java.time.ZoneId.systemDefault());
        for (StaffDirectory.AccessEntry entry : staffDirectory.recentAccess(120)) {
            model.addRow(new Object[]{stamp.format(entry.getAt()), entry.getUsername(),
                    entry.getAction().replace('_', ' '),
                    entry.getDetail() == null ? "" : entry.getDetail()});
        }
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(WHITE);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    /** Asks for a new account and creates it with a PIN shown once. */
    private void promptForNewStaff() {
        JTextField name = new JTextField(16);
        JTextField username = new JTextField(16);
        JComboBox<StaffDirectory.Role> role =
                new JComboBox<>(StaffDirectory.Role.values());
        role.setSelectedItem(StaffDirectory.Role.CLERK);
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(4, 0, 4, 10);
        form.add(new JLabel("Name"), constraints);
        constraints.gridx = 1;
        form.add(name, constraints);
        constraints.gridx = 0;
        constraints.gridy = 1;
        form.add(new JLabel("Username"), constraints);
        constraints.gridx = 1;
        form.add(username, constraints);
        constraints.gridx = 0;
        constraints.gridy = 2;
        form.add(new JLabel("Role"), constraints);
        constraints.gridx = 1;
        form.add(role, constraints);

        String choice = showDialogChoice(this, form, "Add staff account",
                JOptionPane.PLAIN_MESSAGE, "Create", "Create", BACK_LABEL);
        if (!"Create".equals(choice)) {
            return;
        }
        String refusal = staffDirectory.create(username.getText(), name.getText(),
                StaffDirectory.startingPin(), (StaffDirectory.Role) role.getSelectedItem(), true);
        if (refusal != null) {
            showWarning(refusal);
            return;
        }
        String created = username.getText().trim().toLowerCase(java.util.Locale.ENGLISH);
        showDialogChoice(this, "Account created.\n\nUsername:  " + created
                        + "\nPIN:       " + staffDirectory.lastCreatedPin(created)
                        + "\n\nThis PIN is shown once. They must change it at first sign-in.",
                "Starting PIN", JOptionPane.INFORMATION_MESSAGE,
                "I have written it down", "I have written it down");
        staffDirectory.clearLastCreatedPin();
        staffDirectory.log(staffSession.getUsername(), "ACCOUNT_CREATED", created);
        contentHost.removeAll();
        contentHost.add(buildStaffContent(), BorderLayout.CENTER);
        contentHost.revalidate();
        contentHost.repaint();
    }

    /** Resets a forgotten PIN, deactivates an account, or changes a role. */
    private void manageStaffAccount(String username) {
        StaffDirectory.Account account = staffDirectory.list().stream()
                .filter(candidate -> candidate.getUsername().equals(username))
                .findFirst()
                .orElse(null);
        if (account == null) {
            showWarning("That account no longer exists.");
            return;
        }
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(4, 0, 4, 10);
        form.add(new JLabel("Name"), constraints);
        constraints.gridx = 1;
        form.add(new JLabel(account.getDisplayName()), constraints);
        constraints.gridx = 0;
        constraints.gridy = 1;
        form.add(new JLabel("Username"), constraints);
        constraints.gridx = 1;
        form.add(new JLabel(account.getUsername()), constraints);
        constraints.gridx = 0;
        constraints.gridy = 2;
        form.add(new JLabel("Role"), constraints);
        JComboBox<StaffDirectory.Role> role =
                new JComboBox<>(StaffDirectory.Role.values());
        role.setSelectedItem(account.getRole());
        constraints.gridx = 1;
        form.add(role, constraints);

        String choice = showDialogChoice(this, form, "Manage " + account.getDisplayName(),
                JOptionPane.PLAIN_MESSAGE, "Save", "Save", BACK_LABEL);
        if (!"Save".equals(choice)) {
            return;
        }
        String refusal = staffDirectory.changeRole(username,
                (StaffDirectory.Role) role.getSelectedItem());
        if (refusal != null) {
            showWarning(refusal);
            return;
        }
        staffDirectory.log(staffSession.getUsername(), "ROLE_CHANGED",
                username + " -> " + role.getSelectedItem());
        if (account.isActive()) {
            String action = showDialogChoice(this,
                    "What would you like to do with " + account.getDisplayName() + "?",
                    "Manage account", JOptionPane.PLAIN_MESSAGE, "Choose",
                    "Reset their PIN", "Deactivate the account", BACK_LABEL);
            if ("Reset their PIN".equals(action)) {
                String pin = StaffDirectory.startingPin();
                String problem = staffDirectory.resetPin(username, pin);
                if (problem != null) {
                    showWarning(problem);
                } else {
                    showDialogChoice(this, "New PIN for " + account.getDisplayName()
                                    + ":\n\n" + pin
                                    + "\n\nShown once. They must change it at next sign-in.",
                            "PIN reset", JOptionPane.INFORMATION_MESSAGE,
                            "I have written it down", "I have written it down");
                    staffDirectory.log(staffSession.getUsername(), "PIN_RESET", username);
                }
            } else if ("Deactivate the account".equals(action)) {
                if (account.getUsername().equals(staffSession.getUsername())) {
                    showWarning("You cannot deactivate your own account.");
                } else {
                    staffDirectory.setActive(username, false);
                    showStatus(account.getDisplayName() + " has been deactivated");
                }
            }
        }
        contentHost.removeAll();
        contentHost.add(buildStaffContent(), BorderLayout.CENTER);
        contentHost.revalidate();
        contentHost.repaint();
    }

    private void showWarning(String message) {
        showDialogChoice(this, message, "Please check your booking",
                JOptionPane.WARNING_MESSAGE, BACK_LABEL, BACK_LABEL);
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
        return BookingService.formatMoney(amount);
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

    /** One row in a live suggestion list. */
    private static final class Suggestion {
        final String label;
        final String detail;
        final String value;

        Suggestion(String label, String detail, String value) {
            this.label = label;
            this.detail = detail == null ? "" : detail;
            this.value = value;
        }
    }

    /**
     * A live suggestion list attached to a search field. The list refreshes shortly
     * after every keystroke and can be driven with the arrow keys, Enter, Escape or
     * the mouse.
     *
     * <p>Choosing a row writes the suggestion back into the field, which fires the
     * field's document listener and so re-runs the existing search filter.
     */
    private final class SearchSuggestions {
        private static final int MAX_ROWS = 8;
        private final JTextField field;
        private final Function<String, List<Suggestion>> source;
        private final Timer debounce;
        private final JPopupMenu popup = new JPopupMenu();
        private final JList<Suggestion> list = new JList<>();

        SearchSuggestions(JTextField field, Function<String, List<Suggestion>> source) {
            this.field = field;
            this.source = source;
            this.debounce = new Timer(110, event -> refresh());
            this.debounce.setRepeats(false);

            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setCellRenderer(new SuggestionRenderer());
            list.setBackground(WHITE);
            list.setFixedCellHeight(44);
            list.setBorder(new EmptyBorder(4, 0, 4, 0));
            list.setVisibleRowCount(MAX_ROWS);

            // A non-focusable popup never steals focus, so the field keeps the caret
            // and the arrow keys keep working while the list is open.
            popup.setFocusable(false);
            popup.setBackground(WHITE);
            popup.setBorder(BorderFactory.createLineBorder(BORDER));
            popup.add(list);
            list.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    int index = list.locationToIndex(event.getPoint());
                    if (index >= 0) {
                        list.setSelectedIndex(index);
                        accept(index);
                    }
                }
            });

            field.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    schedule();
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    schedule();
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    schedule();
                }
            });
            field.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent event) {
                    handleKey(event);
                }
            });
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent event) {
                    schedule();
                }

                @Override
                public void focusLost(FocusEvent event) {
                    hideLater();
                }
            });
        }

        private void schedule() {
            debounce.restart();
        }

        private void hideLater() {
            // Let a click on the popup finish before dismissing it.
            Timer closer = new Timer(120, event -> popup.setVisible(false));
            closer.setRepeats(false);
            closer.start();
        }

        private void refresh() {
            List<Suggestion> suggestions = source.apply(field.getText());
            if (suggestions.isEmpty()) {
                suggestions = List.of(new Suggestion("No matches",
                        "Try a different word", null));
            }
            if (suggestions.size() > MAX_ROWS) {
                suggestions = new ArrayList<>(suggestions.subList(0, MAX_ROWS));
            }
            DefaultListModel<Suggestion> model = new DefaultListModel<>();
            for (Suggestion suggestion : suggestions) {
                model.addElement(suggestion);
            }
            list.setModel(model);
            list.setSelectedIndex(0);
            if (!field.isFocusOwner()) {
                return;
            }
            popup.setPopupSize(Math.max(field.getWidth(), 340),
                    Math.min(MAX_ROWS, suggestions.size()) * 46 + 8);
            popup.show(field, 0, field.getHeight() + 2);
        }

        private void handleKey(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                if (popup.isVisible()) {
                    popup.setVisible(false);
                    event.consume();
                }
                return;
            }
            if (event.getKeyCode() == KeyEvent.VK_DOWN && popup.isVisible()) {
                if (list.getSelectedIndex() < list.getModel().getSize() - 1) {
                    list.setSelectedIndex(list.getSelectedIndex() + 1);
                }
                event.consume();
                return;
            }
            if (event.getKeyCode() == KeyEvent.VK_UP && popup.isVisible()) {
                if (list.getSelectedIndex() > 0) {
                    list.setSelectedIndex(list.getSelectedIndex() - 1);
                }
                event.consume();
                return;
            }
            if (event.getKeyCode() == KeyEvent.VK_ENTER && popup.isVisible()) {
                accept(list.getSelectedIndex());
                event.consume();
            }
        }

        private void accept(int index) {
            popup.setVisible(false);
            if (index < 0 || index >= list.getModel().getSize()) {
                return;
            }
            Suggestion suggestion = list.getModel().getElementAt(index);
            if (suggestion.value == null) {
                return;
            }
            field.setText(suggestion.value);
            field.setCaretPosition(field.getText().length());
            field.requestFocusInWindow();
        }
    }

    private final class SuggestionRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> source, Object value, int index,
                                                      boolean selected, boolean focused) {
            if (!(value instanceof Suggestion suggestion)) {
                return super.getListCellRendererComponent(source, value, index, selected, focused);
            }
            JPanel row = new JPanel(new BorderLayout(0, 1));
            row.setOpaque(true);
            row.setBackground(suggestion.value == null ? WHITE
                    : selected ? new Color(219, 234, 254) : WHITE);
            JLabel label = new JLabel(suggestion.label);
            label.setForeground(suggestion.value == null ? MUTED : TEXT);
            label.setFont(label.getFont().deriveFont(
                    suggestion.value == null ? Font.PLAIN : Font.BOLD, 11f));
            JLabel detail = new JLabel(suggestion.detail);
            detail.setForeground(suggestion.value == null ? new Color(160, 172, 188)
                    : selected ? BLUE_DARK : MUTED);
            detail.setFont(detail.getFont().deriveFont(Font.PLAIN, 10f));
            row.add(label, BorderLayout.NORTH);
            row.add(detail, BorderLayout.CENTER);
            row.setBorder(new EmptyBorder(6, 12, 6, 12));
            return row;
        }
    }

    private List<Suggestion> stadiumSuggestions(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ENGLISH);
        List<Suggestion> suggestions = new ArrayList<>();
        int venues = 0;
        for (Stadium stadium : StadiumData.getStadiums()) {
            List<StadiumEvent> events = StadiumData.getEvents(stadium.getId());
            boolean eventMatch = events.stream()
                    .anyMatch(event -> event.searchableText().contains(q));
            if (!q.isEmpty() && !stadium.searchableText().contains(q) && !eventMatch) {
                continue;
            }
            venues++;
            StadiumEvent next = events.isEmpty() ? null : events.get(0);
            suggestions.add(new Suggestion(stadium.getName(),
                    stadium.getLocation() + "  •  " + formatCapacity(stadium.getCapacity()) + " seats"
                            + (next == null ? "" : "  •  next " + next.getDate().format(DATE_FORMATTER)),
                    stadium.getName()));
        }
        if (q.isEmpty()) {
            return suggestions;
        }
        // Teams and artists hosted at the venue are useful shortcuts too.
        for (String name : distinctTeamsAndArtists(q)) {
            suggestions.add(new Suggestion(name, "Team or artist appearing at a venue", name));
        }
        if (suggestions.isEmpty()) {
            return suggestions;
        }
        suggestions.add(new Suggestion(venues + (venues == 1 ? " venue matches" : " venues match"),
                "Press Enter to see the full list", null));
        return suggestions;
    }

    private List<Suggestion> eventSuggestions(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ENGLISH);
        List<Suggestion> suggestions = new ArrayList<>();
        Stadium stadium = selectedStadium;
        List<StadiumEvent> events = stadium == null
                ? StadiumData.getEvents() : StadiumData.getEvents(stadium.getId());
        int matches = 0;
        for (StadiumEvent event : events) {
            if (!q.isEmpty() && !event.searchableText().contains(q)
                    && !venueMatches(event, q)) {
                continue;
            }
            matches++;
            Stadium home = StadiumData.getStadium(event.getStadiumId());
            suggestions.add(new Suggestion(event.getHeadline(),
                    (home == null ? "" : home.getName() + "  •  ")
                            + event.getDate().format(DATE_FORMATTER) + "  •  "
                            + event.getStartTime().format(TIME_FORMATTER),
                    event.getHeadline()));
        }
        if (!q.isEmpty()) {
            for (String name : distinctTeamsAndArtists(q)) {
                suggestions.add(new Suggestion(name, "Team or artist", name));
            }
        }
        if (suggestions.isEmpty()) {
            return suggestions;
        }
        suggestions.add(new Suggestion(matches + (matches == 1 ? " event matches" : " events match"),
                "Press Enter to see the full list", null));
        return suggestions;
    }

    private List<Suggestion> bookingSuggestions(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ENGLISH);
        List<Suggestion> suggestions = new ArrayList<>();
        int matches = 0;
        for (Booking booking : bookingService.getBookings()) {
            if (!q.isEmpty() && !bookingSearchText(booking).contains(q)) {
                continue;
            }
            matches++;
            Stadium stadium = StadiumData.getStadium(booking.getStadiumId());
            suggestions.add(new Suggestion(booking.getReference() + "  •  " + booking.getEvent(),
                    (stadium == null ? "" : stadium.getName() + "  •  ")
                            + booking.getSeatDisplay() + "  •  " + booking.getStatus().name(),
                    booking.getReference()));
        }
        if (suggestions.isEmpty()) {
            return suggestions;
        }
        suggestions.add(new Suggestion(matches + (matches == 1 ? " booking matches" : " bookings match"),
                "Press Enter to see the full list", null));
        return suggestions;
    }

    /** Distinct team and artist names in the dataset that contain the query. */
    private List<String> distinctTeamsAndArtists(String query) {
        List<String> names = new ArrayList<>();
        for (StadiumEvent event : StadiumData.getEvents()) {
            for (String name : List.of(
                    event.getTeamOne() == null ? "" : event.getTeamOne(),
                    event.getTeamTwo() == null ? "" : event.getTeamTwo(),
                    event.getArtist() == null ? "" : event.getArtist())) {
                if (name.isEmpty() || names.contains(name)) {
                    continue;
                }
                if (name.toLowerCase(Locale.ENGLISH).contains(query)) {
                    names.add(name);
                }
            }
        }
        return names;
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
            StadiumBookingApp app = new StadiumBookingApp();
            app.setVisible(true);
            // Dialogs need a visible parent, so first run setup waits for the window.
            app.runFirstRunSetup();
        });
    }
}
