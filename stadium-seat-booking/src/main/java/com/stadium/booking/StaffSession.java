package com.stadium.booking;

/**
 * Who is signed in for this run of the application.
 *
 * <p>The credentials themselves live in {@link StaffDirectory}; this only holds
 * the identity of the person currently using the machine. The earlier version
 * compared a PIN against a hash held in memory, which meant the PIN was in the
 * source and every user was anonymous.
 */
public final class StaffSession {
    private StaffDirectory directory;
    private String username;
    private String displayName;
    private StaffDirectory.Role role;

    public StaffSession(StaffDirectory directory) {
        this.directory = directory;
    }

    /**
     * Attempts a sign-in.
     *
     * @return null on success, otherwise a message explaining the refusal
     */
    public String signIn(String username, String pin) {
        if (directory == null) {
            return "The staff list is not available. Please restart the application.";
        }
        StaffDirectory.Result result = directory.signIn(username, pin);
        if (!result.isSuccess()) {
            this.username = null;
            this.displayName = null;
            this.role = null;
            return result.getRefusal();
        }
        StaffDirectory.Account account = result.getAccount();
        this.username = account.getUsername();
        this.displayName = account.getDisplayName();
        this.role = account.getRole();
        return null;
    }

    public void signOut() {
        if (username != null && directory != null) {
            directory.log(username, "SIGN_OUT", "");
        }
        username = null;
        displayName = null;
        role = null;
    }

    public boolean isSignedIn() {
        return username != null;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public StaffDirectory.Role getRole() {
        return role;
    }

    /** True when the account is still on a PIN it must replace before carrying on. */
    public boolean mustChangePin() {
        if (directory == null || username == null) {
            return false;
        }
        return directory.list().stream()
                .filter(account -> account.getUsername().equals(username))
                .anyMatch(StaffDirectory.Account::mustChangePin);
    }

    /**
     * Whether the signed-in user may see full customer contact details. A clerk
     * can see that a booking exists and what was bought, but not how to reach the
     * customer.
     */
    public boolean canViewCustomerDetails() {
        return isSignedIn() && role == StaffDirectory.Role.MANAGER;
    }

    /** Whether this user may see the reports at all. */
    public boolean canViewReports() {
        return isSignedIn() && role != StaffDirectory.Role.SUPERVISOR;
    }

    /**
     * Kept for the call sites that only have a boolean to hand, such as the seat
     * ledger, which the application gates before a sign-in has happened.
     */
    public static boolean canViewCustomerDetails(boolean signedIn) {
        return signedIn;
    }

    /** Records that the signed-in user opened something, for the access trail. */
    public void recordAccess(String action, String detail) {
        if (username != null && directory != null) {
            directory.log(username, action, detail);
        }
    }
}
