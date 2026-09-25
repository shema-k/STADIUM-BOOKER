package com.stadium.booking;

import java.time.LocalDateTime;
import java.util.Objects;

/** A notice, schedule update, emergency or user-submitted special request. */
public final class StadiumAnnouncement {
    private final String id;
    private final String stadiumId;
    private final String eventId;
    private final AnnouncementType type;
    private final String title;
    private final String message;
    private final LocalDateTime createdAt;
    private final LocalDateTime activeUntil;

    public StadiumAnnouncement(String id,
                               String stadiumId,
                               String eventId,
                               AnnouncementType type,
                               String title,
                               String message,
                               LocalDateTime createdAt,
                               LocalDateTime activeUntil) {
        this.id = Objects.requireNonNull(id, "id");
        this.stadiumId = Objects.requireNonNull(stadiumId, "stadiumId");
        this.eventId = eventId == null ? "" : eventId;
        this.type = Objects.requireNonNull(type, "type");
        this.title = Objects.requireNonNull(title, "title");
        this.message = Objects.requireNonNull(message, "message");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.activeUntil = activeUntil;
    }

    public String getId() {
        return id;
    }

    public String getStadiumId() {
        return stadiumId;
    }

    public String getEventId() {
        return eventId;
    }

    public AnnouncementType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getActiveUntil() {
        return activeUntil;
    }

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(createdAt) && (activeUntil == null || now.isBefore(activeUntil));
    }

    public boolean isBlocking() {
        return type.isBlocking() && isActive();
    }

    public boolean appliesToEvent(String candidateEventId) {
        return eventId.isEmpty() || eventId.equals(candidateEventId);
    }
}
