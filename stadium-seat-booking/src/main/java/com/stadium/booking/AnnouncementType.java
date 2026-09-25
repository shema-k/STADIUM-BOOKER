package com.stadium.booking;

/** Categories shown in the stadium special-notices section. */
public enum AnnouncementType {
    NOTICE("Notice", false),
    SCHEDULE_CHANGE("Schedule update", false),
    CANCELLATION("Event cancellation", true),
    EMERGENCY("Stadium emergency", true),
    SPECIAL_REQUEST("Special request", false);

    private final String label;
    private final boolean blocking;

    AnnouncementType(String label, boolean blocking) {
        this.label = label;
        this.blocking = blocking;
    }

    public String getLabel() {
        return label;
    }

    public boolean isBlocking() {
        return blocking;
    }
}
