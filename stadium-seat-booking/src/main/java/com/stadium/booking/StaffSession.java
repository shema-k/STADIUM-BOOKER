package com.stadium.booking;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * A small staff sign-in gate.
 *
 * <p>The application is a desktop tool, so this is deliberately simple: a staff
 * PIN unlocks the management screens. Customer contact details are protected by
 * {@link #canViewCustomerDetails}, which only lets staff see them.
 */
public final class StaffSession {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String salt;
    private final String expectedHash;
    private String signedInStaff;
    private int failedAttempts;

    /** Creates a session for a PIN, storing only a salted hash of it. */
    public StaffSession(String pin) {
        this.salt = randomSalt();
        this.expectedHash = hash(pin, salt);
    }

    private static String randomSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String hash(String value, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((salt + value).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required but unavailable", exception);
        }
    }

    /**
     * Attempts a sign-in.
     *
     * @return null on success, otherwise a message explaining the refusal
     */
    public synchronized String signIn(String pin) {
        if (pin == null || pin.isBlank()) {
            return "Enter your staff PIN";
        }
        if (!expectedHash.equals(hash(pin, salt))) {
            failedAttempts++;
            return "That PIN is not correct";
        }
        failedAttempts = 0;
        signedInStaff = "Staff";
        return null;
    }

    public synchronized void signOut() {
        signedInStaff = null;
    }

    public synchronized boolean isSignedIn() {
        return signedInStaff != null;
    }

    public synchronized int getFailedAttempts() {
        return failedAttempts;
    }

    /** Locks the app after repeated wrong PINs so the data cannot be ground down. */
    public synchronized boolean isLockedOut() {
        return failedAttempts >= 5;
    }

    /**
     * Whether the signed-in user may see full customer contact details. Without a
     * sign-in nothing is shown beyond the seat reference.
     */
    public static boolean canViewCustomerDetails(boolean signedIn) {
        return signedIn;
    }
}
