package com.stadium.booking;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Formats the time remaining before an event's booking deadline. */
public final class BookingCountdown {
    private BookingCountdown() {
    }

    public static String format(LocalDateTime deadline) {
        if (deadline == null) {
            return "Deadline unavailable";
        }
        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(deadline)) {
            return "Booking closed";
        }

        LocalDate today = now.toLocalDate();
        LocalDate endDate = deadline.toLocalDate();
        long days = ChronoUnit.DAYS.between(today, endDate);
        long months = ChronoUnit.MONTHS.between(today, endDate);

        if (months > 1) {
            long remainingDays = ChronoUnit.DAYS.between(today.plusMonths(months), endDate);
            if (remainingDays > 0) {
                return months + " months " + remainingDays
                        + (remainingDays == 1 ? " day left" : " days left");
            }
            return months + " months left";
        }
        if (months == 1) {
            long remainingDays = ChronoUnit.DAYS.between(today.plusMonths(1), endDate);
            if (remainingDays > 0) {
                return "1 month " + remainingDays + (remainingDays == 1 ? " day left" : " days left");
            }
            return "1 month left";
        }
        if (days > 0) {
            return days + (days == 1 ? " day left" : " days left");
        }

        Duration remaining = Duration.between(now, deadline);
        long hours = remaining.toHours();
        if (hours > 0) {
            return hours + (hours == 1 ? " hour left" : " hours left");
        }
        long minutes = Math.max(1, remaining.toMinutes());
        return minutes + (minutes == 1 ? " minute left" : " minutes left");
    }
}
