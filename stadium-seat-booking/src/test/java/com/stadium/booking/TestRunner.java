package com.stadium.booking;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A very small test harness so the project can be tested with nothing but a JDK.
 * No external libraries, no build tool: compile the sources and run this class.
 */
public final class TestRunner {
    private static final List<String> FAILURES = new ArrayList<>();
    private static int passed;
    private static String suite = "";

    private TestRunner() {
    }

    public interface Body {
        void run() throws Exception;
    }

    public static void suite(String name) {
        suite = name;
        System.out.println();
        System.out.println("== " + name + " ==");
    }

    public static void test(String name, Body body) {
        try {
            body.run();
            passed++;
            System.out.println("  PASS  " + name);
        } catch (AssertionError | Exception failure) {
            FAILURES.add(suite + " / " + name + "  ->  " + failure);
            System.out.println("  FAIL  " + name + "  ->  " + failure);
        }
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected <" + expected + "> but was <" + actual + ">)");
        }
    }

    public static void assertClose(double expected, double actual, double tolerance, String message) {
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(message + " (expected " + expected + " but was " + actual + ")");
        }
    }

    /** A throwable carrying a message, for asserting that something is rejected. */
    public static void assertThrows(String message, Body body) {
        try {
            body.run();
        } catch (Exception expected) {
            return;
        }
        throw new AssertionError(message + " (nothing was thrown)");
    }

    public static Path freshDatabase(String prefix) throws Exception {
        Path directory = Files.createTempDirectory(prefix);
        return directory.resolve("bookings.dat");
    }

    public static int summary() {
        System.out.println();
        System.out.println("----------------------------------------");
        System.out.println(passed + " passed, " + FAILURES.size() + " failed");
        if (!FAILURES.isEmpty()) {
            System.out.println();
            for (String failure : FAILURES) {
                System.out.println("  FAILED: " + failure);
            }
        }
        return FAILURES.isEmpty() ? 0 : 1;
    }

    public static void main(String[] args) {
        StadiumDataTest.register();
        BookingPricingTest.register();
        BookingPersistenceTest.register();
        SeatAllocationTest.register();
        SeatLedgerTest.register();
        SearchAndFilterTest.register();
        BookingToolsTest.register();
        LocaleTest.register();
        OccupancyReportTest.register();
        KeyboardSeatSelectionTest.register();
        StaffAccountTest.register();
        DatabaseProtectionTest.register();
        StadiumDetailsTest.register();
        StadiumDetailsTest.registerPictureSuite();
        ThemeTest.register();
        SavedSelectionTest.register();
        System.exit(summary());
    }
}
