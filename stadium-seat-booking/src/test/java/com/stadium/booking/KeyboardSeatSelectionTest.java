package com.stadium.booking;

import static com.stadium.booking.TestRunner.assertEquals;
import static com.stadium.booking.TestRunner.assertFalse;
import static com.stadium.booking.TestRunner.assertTrue;
import static com.stadium.booking.TestRunner.freshDatabase;
import static com.stadium.booking.TestRunner.suite;
import static com.stadium.booking.TestRunner.test;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * The seat map without a mouse.
 *
 * <p>A hand-painted canvas is not focusable by default, so the keyboard bindings
 * look installed but never fire. These tests drive the actions directly, which is
 * the only reliable way to check this without a real display.
 */
final class KeyboardSeatSelectionTest {
    private KeyboardSeatSelectionTest() {
    }

    private static SeatMapPanel panel() throws Exception {
        BookingService service = new BookingService(new BookingStore(freshDatabase("keyboard")));
        service.selectEvent(StadiumData.getEvent("namboole-01"));
        List<Seat> ignored = new ArrayList<>();
        return new SeatMapPanel(service, ignored::add, () -> { }, message -> { });
    }

    static void register() {
        suite("Keyboard seat selection");

        test("the seat canvas accepts keyboard focus", () -> {
            // Without focus on this canvas the arrow keys go nowhere, however
            // they are bound, so the property is asserted rather than assumed.
            SeatMapPanel map = panel();
            Component canvas = map.getSectionCanvas("A");
            assertTrue(canvas != null, "section A must have a canvas");
            assertTrue(canvas.isFocusable(),
                    "the canvas must be focusable or it never receives key presses");
            assertTrue(canvas.getFocusTraversalKeysEnabled() == false,
                    "traversal keys are handled by the grid, not by focus traversal");
        });

        test("the canvas describes itself to assistive technology", () -> {
            SeatMapPanel map = panel();
            Component canvas = map.getSectionCanvas("A");
            String name = canvas.getAccessibleContext().getAccessibleName();
            String description = canvas.getAccessibleContext().getAccessibleDescription();
            assertTrue(name != null && name.contains("A"), "accessible name names the section: " + name);
            assertTrue(description != null && description.contains("Enter"),
                    "description explains how to hold a seat: " + description);
        });

        test("the arrow key actions are installed", () -> {
            SeatMapPanel map = panel();
            for (String action : new String[]{SeatMapPanel.ACTION_UP, SeatMapPanel.ACTION_DOWN,
                    SeatMapPanel.ACTION_LEFT, SeatMapPanel.ACTION_RIGHT, SeatMapPanel.ACTION_HOLD}) {
                assertTrue(map.fireKeyboardAction(action), action + " must be bound");
            }
        });

        test("the caret starts at the first seat and walks the grid", () -> {
            SeatMapPanel map = panel();
            assertEquals("A1-01", map.getKeyboardCaret().display(), "starts at the first seat");
            map.fireKeyboardAction(SeatMapPanel.ACTION_RIGHT);
            assertEquals("A1-02", map.getKeyboardCaret().display(), "right moves along the row");
            map.fireKeyboardAction(SeatMapPanel.ACTION_RIGHT);
            assertEquals("A1-03", map.getKeyboardCaret().display(), "right again");
            map.fireKeyboardAction(SeatMapPanel.ACTION_LEFT);
            assertEquals("A1-02", map.getKeyboardCaret().display(), "left goes back");
            map.fireKeyboardAction(SeatMapPanel.ACTION_DOWN);
            assertEquals("A2-02", map.getKeyboardCaret().display(), "down moves to the next row");
            map.fireKeyboardAction(SeatMapPanel.ACTION_UP);
            assertEquals("A1-02", map.getKeyboardCaret().display(), "up returns");
        });

        test("the caret cannot walk off the grid", () -> {
            SeatMapPanel map = panel();
            for (int step = 0; step < 12; step++) {
                map.fireKeyboardAction(SeatMapPanel.ACTION_LEFT);
                map.fireKeyboardAction(SeatMapPanel.ACTION_UP);
            }
            assertEquals("A1-01", map.getKeyboardCaret().display(), "stops at the first seat");
            for (int step = 0; step < 500; step++) {
                map.fireKeyboardAction(SeatMapPanel.ACTION_RIGHT);
                map.fireKeyboardAction(SeatMapPanel.ACTION_DOWN);
            }
            SeatKey last = map.getKeyboardCaret();
            Stadium namboole = StadiumData.getStadium("namboole");
            SeatSection sectionA = namboole.getSection("A");
            assertEquals(sectionA.getRows(), last.getRow(), "stops at the last row");
            assertEquals(sectionA.getSeatsPerRow(), last.getNumber(), "stops at the last seat");
        });

        test("the caret movement announces the seat, its state and price", () -> {
            SeatMapPanel map = panel();
            map.fireKeyboardAction(SeatMapPanel.ACTION_RIGHT);
            String announcement = map.getAnnouncedSeat();
            assertTrue(announcement.contains("Section A"), "names the section: " + announcement);
            assertTrue(announcement.contains("row 1"), "gives the row: " + announcement);
            assertTrue(announcement.contains("seat 2"), "gives the seat number: " + announcement);
            assertTrue(announcement.contains("vacant"), "gives the state: " + announcement);
            assertTrue(announcement.contains("UGX"), "gives the price: " + announcement);
        });

        test("Enter holds the seat under the caret", () -> {
            SeatMapPanel map = panel();
            map.fireKeyboardAction(SeatMapPanel.ACTION_RIGHT);
            map.fireKeyboardAction(SeatMapPanel.ACTION_HOLD);
            List<SeatKey> chosen = new ArrayList<>();
            for (Seat seat : map.getSelectedSeats()) {
                chosen.add(seat.getKey());
            }
            assertEquals(1, chosen.size(), "one seat chosen: " + chosen);
            assertEquals("A1-02", chosen.get(0).display(), "the seat under the caret");
        });

        test("Enter again releases the seat", () -> {
            SeatMapPanel map = panel();
            map.fireKeyboardAction(SeatMapPanel.ACTION_HOLD);
            assertEquals(1, map.getSelectedSeats().size(), "held");
            map.fireKeyboardAction(SeatMapPanel.ACTION_HOLD);
            assertEquals(0, map.getSelectedSeats().size(), "released again");
        });

        test("seats can be walked along and collected without a mouse", () -> {
            SeatMapPanel map = panel();
            map.fireKeyboardAction(SeatMapPanel.ACTION_RIGHT);
            map.fireKeyboardAction(SeatMapPanel.ACTION_HOLD);
            map.fireKeyboardAction(SeatMapPanel.ACTION_RIGHT);
            map.fireKeyboardAction(SeatMapPanel.ACTION_HOLD);
            map.fireKeyboardAction(SeatMapPanel.ACTION_RIGHT);
            map.fireKeyboardAction(SeatMapPanel.ACTION_HOLD);
            List<String> chosen = new ArrayList<>();
            for (Seat seat : map.getSelectedSeats()) {
                chosen.add(seat.getKey().display());
            }
            assertEquals(3, chosen.size(), "three seats: " + chosen);
            assertTrue(chosen.contains("A1-02") && chosen.contains("A1-03") && chosen.contains("A1-04"),
                    "the seats walked past: " + chosen);
        });

        test("a booked seat is announced as booked", () -> {
            BookingService service = new BookingService(new BookingStore(freshDatabase("keyboard2")));
            StadiumEvent event = StadiumData.getEvent("namboole-01");
            service.selectEvent(event);
            service.book("Someone Else", "x@example.co.ug", "+256700000009",
                    List.of(service.getSeat(new SeatKey("A", 1, 2))));
            SeatMapPanel map = new SeatMapPanel(service, seat -> { }, () -> { },
                    message -> { });
            map.fireKeyboardAction(SeatMapPanel.ACTION_RIGHT);
            assertTrue(map.getAnnouncedSeat().contains("booked"),
                    "a taken seat is announced as booked: " + map.getAnnouncedSeat());
        });

        test("a booked seat cannot be taken from the keyboard", () -> {
            BookingService service = new BookingService(new BookingStore(freshDatabase("keyboard3")));
            StadiumEvent event = StadiumData.getEvent("namboole-01");
            service.selectEvent(event);
            service.book("Someone Else", "x@example.co.ug", "+256700000009",
                    List.of(service.getSeat(new SeatKey("A", 1, 2))));
            SeatMapPanel map = new SeatMapPanel(service, seat -> { }, () -> { },
                    message -> { });
            map.fireKeyboardAction(SeatMapPanel.ACTION_RIGHT);
            map.fireKeyboardAction(SeatMapPanel.ACTION_HOLD);
            assertEquals(0, map.getSelectedSeats().size(), "a sold seat stays unsold");
        });

        test("the held seat is placed under a hold so a second customer is blocked", () -> {
            SeatMapPanel map = panel();
            map.setHoldOwner("first-customer");
            map.fireKeyboardAction(SeatMapPanel.ACTION_HOLD);
            SeatKey held = map.getSelectedSeats().get(0).getKey();
            assertTrue(map.getHoldService().secondsRemaining(held) > 0, "the seat is held");
            assertFalse(map.getHoldService().isHeldByAnother(held,
                            map.getKeyboardCaret() == null ? null
                                    : StadiumData.getEvent("namboole-01"), "first-customer"),
                    "the holder is not blocked from their own seat");
        });
    }
}
