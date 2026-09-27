package com.stadium.booking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Records how a booking was paid for.
 *
 * <p>This models the flow a stadium box office actually uses: cash at the desk,
 * or a mobile money transfer. The mobile money option here is a simulated
 * authorisation, because a real transfer needs a merchant account with a
 * provider; {@link #isRealProviderConfigured()} says so plainly rather than
 * pretending a payment was taken.
 */
public final class PaymentRecord {
    /** The ways a booking can be settled. */
    public enum Method {
        CASH_AT_VENUE("Cash at the venue desk"),
        MOBILE_MONEY("Mobile money (simulated)"),
        NOT_REQUIRED("No payment required");

        private final String label;

        Method(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final Method method;
    private final String reference;
    private final double amount;
    private final LocalDateTime takenAt;
    private final boolean simulated;

    private PaymentRecord(Method method, String reference, double amount,
                          LocalDateTime takenAt, boolean simulated) {
        this.method = method;
        this.reference = reference;
        this.amount = amount;
        this.takenAt = takenAt;
        this.simulated = simulated;
    }

    /**
     * Settles a booking as cash at the venue, which is the default because it
     * needs no third party and is how most Ugandan venues still operate.
     */
    public static PaymentRecord cashAtVenue(double amount) {
        return new PaymentRecord(Method.CASH_AT_VENUE, generateReference(),
                round(amount), LocalDateTime.now(), false);
    }

    /**
     * Simulates a mobile money authorisation.
     *
     * <p>No money moves. The reference is local to this machine so the record can
     * be shown on the ticket, and {@link #isSimulated()} is true.
     */
    public static PaymentRecord simulatedMobileMoney(double amount, String phone) {
        return new PaymentRecord(Method.MOBILE_MONEY, "SIM-" + generateReference(),
                round(amount), LocalDateTime.now(), true);
    }

    public static String generateReference() {
        return String.format(Locale.US, "%06d", (int) (Math.random() * 1_000_000));
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public Method getMethod() {
        return method;
    }

    public String getReference() {
        return reference;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public boolean isSimulated() {
        return simulated;
    }

    /** A real transfer needs provider credentials, which this build does not hold. */
    public static boolean isRealProviderConfigured() {
        return false;
    }

    public String describe() {
        StringBuilder text = new StringBuilder(method.getLabel())
                .append("  ").append(BookingService.formatMoney(amount))
                .append("  ref ").append(reference);
        if (simulated) {
            text.append("  (simulated, no money moved)");
        }
        return text.toString();
    }
}
