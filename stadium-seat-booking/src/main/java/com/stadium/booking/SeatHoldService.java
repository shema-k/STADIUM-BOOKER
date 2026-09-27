package com.stadium.booking;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Temporary holds on seats while a customer completes their details.
 *
 * <p>Without a hold, two people can open the same seat and only find out at the
 * moment they confirm. A hold reserves the seat for a few minutes so the second
 * person is turned away straight away.
 */
public final class SeatHoldService {
    /** How long a seat stays held before it is released automatically. */
    public static final Duration HOLD_DURATION = Duration.ofMinutes(5);

    private static final class Hold {
        final SeatKey key;
        final StadiumEvent event;
        final String owner;
        final Instant expiresAt;

        Hold(SeatKey key, StadiumEvent event, String owner, Instant expiresAt) {
            this.key = key;
            this.event = event;
            this.owner = owner;
            this.expiresAt = expiresAt;
        }

        boolean covers(StadiumEvent candidate) {
            return event != null && event.getId().equals(candidate.getId());
        }
    }

    private final Map<SeatKey, Hold> holds = new LinkedHashMap<>();
    private final BookingService bookingService;

    public SeatHoldService(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** Drops every hold that has run out, so expired seats become selectable again. */
    public synchronized void purgeExpired() {
        Instant now = Instant.now();
        holds.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(now));
    }

    public synchronized void releaseAll() {
        holds.clear();
    }

    public synchronized void releaseAllFor(String owner) {
        holds.entrySet().removeIf(entry -> entry.getValue().owner.equals(owner));
    }

    public synchronized void release(SeatKey key) {
        holds.remove(key);
    }

    /** True when the seat is unavailable to this owner because somebody else holds it. */
    public synchronized boolean isHeldByAnother(SeatKey key, StadiumEvent event, String owner) {
        purgeExpired();
        Hold hold = holds.get(key);
        if (hold == null || !hold.covers(event)) {
            return false;
        }
        return !hold.owner.equals(owner);
    }

    /** True when this owner currently holds the seat. */
    public synchronized boolean isHeldBy(SeatKey key, String owner) {
        purgeExpired();
        Hold hold = holds.get(key);
        return hold != null && hold.owner.equals(owner);
    }

    /**
     * Places or renews a hold.
     *
     * @return null when the hold succeeded, or a reason the seat cannot be held
     */
    public synchronized String hold(SeatKey key, StadiumEvent event, String owner) {
        if (bookingService != null && !bookingService.isSeatSelectable(key)) {
            return "Seat " + key.display() + " is not available";
        }
        purgeExpired();
        Hold existing = holds.get(key);
        if (existing != null && !existing.owner.equals(owner) && existing.covers(event)) {
            return "Seat " + key.display() + " is being held by another customer";
        }
        holds.put(key, new Hold(key, event, owner, Instant.now().plus(HOLD_DURATION)));
        return null;
    }

    public synchronized Instant expiryFor(SeatKey key) {
        Hold hold = holds.get(key);
        return hold == null ? null : hold.expiresAt;
    }

    /** Seconds left on a hold, or 0 when there is none. */
    public synchronized long secondsRemaining(SeatKey key) {
        Hold hold = holds.get(key);
        if (hold == null) {
            return 0;
        }
        long seconds = Duration.between(Instant.now(), hold.expiresAt).getSeconds();
        return Math.max(0, seconds);
    }

    public synchronized List<SeatKey> heldBy(String owner, StadiumEvent event) {
        purgeExpired();
        List<SeatKey> keys = new ArrayList<>();
        for (Hold hold : holds.values()) {
            if (hold.owner.equals(owner) && hold.covers(event)) {
                keys.add(hold.key);
            }
        }
        return keys;
    }

    public synchronized Set<SeatKey> allHeldKeys(StadiumEvent event) {
        purgeExpired();
        Set<SeatKey> keys = new LinkedHashSet<>();
        for (Hold hold : holds.values()) {
            if (hold.covers(event)) {
                keys.add(hold.key);
            }
        }
        return keys;
    }
}
