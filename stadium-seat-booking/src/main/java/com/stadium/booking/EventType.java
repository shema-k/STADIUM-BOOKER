package com.stadium.booking;

/** The kind of live event taking place at a venue. */
public enum EventType {
    GAME("Game"),
    CONCERT("Concert");

    private final String label;

    EventType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
