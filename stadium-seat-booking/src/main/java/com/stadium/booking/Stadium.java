package com.stadium.booking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** A venue that can be selected before browsing its schedule and seat map. */
public final class Stadium {
    private final String id;
    private final String name;
    private final String city;
    private final String country;
    private final String venueType;
    private final String description;
    private final String address;
    private final int capacity;
    private final String accentColor;
    private final StadiumShape shape;
    private final List<SeatSection> sections;

    /** Compatibility constructor for the original compact demo. */
    public Stadium(String id,
                   String name,
                   String city,
                   String country,
                   String venueType,
                   String description,
                   String address,
                   int capacity,
                   String accentColor) {
        this(id, name, city, country, venueType, description, address, capacity, accentColor,
                StadiumShape.BOX, defaultSections(capacity));
    }

    public Stadium(String id,
                   String name,
                   String city,
                   String country,
                   String venueType,
                   String description,
                   String address,
                   int capacity,
                   String accentColor,
                   StadiumShape shape,
                   List<SeatSection> sections) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.city = Objects.requireNonNull(city, "city");
        this.country = Objects.requireNonNull(country, "country");
        this.venueType = Objects.requireNonNull(venueType, "venueType");
        this.description = Objects.requireNonNull(description, "description");
        this.address = Objects.requireNonNull(address, "address");
        this.capacity = capacity;
        this.accentColor = Objects.requireNonNull(accentColor, "accentColor");
        this.shape = Objects.requireNonNull(shape, "shape");
        this.sections = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(sections, "sections")));
        if (this.sections.isEmpty()) {
            throw new IllegalArgumentException("A stadium must have at least one section");
        }
        int calculatedCapacity = this.sections.stream().mapToInt(SeatSection::getSeatCount).sum();
        if (calculatedCapacity != capacity) {
            throw new IllegalArgumentException("Stadium capacity does not match its seating sections");
        }
    }

    private static List<SeatSection> defaultSections(int capacity) {
        if (capacity < 4) {
            throw new IllegalArgumentException("A stadium must have at least four seats");
        }
        int[] targets = new int[4];
        int remaining = capacity;
        for (int index = 0; index < targets.length; index++) {
            int sectionsLeft = targets.length - index;
            targets[index] = Math.max(1, remaining / sectionsLeft);
            remaining -= targets[index];
        }
        // Put any integer remainder into the final section.
        targets[targets.length - 1] += remaining;

        List<SeatSection> sections = new ArrayList<>();
        String[] labels = {"North end", "East side", "South end", "West side"};
        for (int index = 0; index < targets.length; index++) {
            int target = targets[index];
            int rows = Math.max(1, (int) Math.sqrt(target));
            while (rows > 1 && target % rows != 0) {
                rows--;
            }
            int seatsPerRow = target / rows;
            sections.add(new SeatSection(String.valueOf((char) ('A' + index)), labels[index],
                    rows, seatsPerRow, 40.0));
        }
        return sections;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getVenueType() {
        return venueType;
    }

    public String getDescription() {
        return description;
    }

    public String getAddress() {
        return address;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getSeatCount() {
        return sections.stream().mapToInt(SeatSection::getSeatCount).sum();
    }

    public String getAccentColor() {
        return accentColor;
    }

    public StadiumShape getShape() {
        return shape;
    }

    public String getShapeLabel() {
        return shape.getLabel();
    }

    public List<SeatSection> getSections() {
        return sections;
    }

    public SeatSection getSection(String sectionId) {
        if (sectionId == null) {
            return null;
        }
        for (SeatSection section : sections) {
            if (section.getId().equalsIgnoreCase(sectionId)) {
                return section;
            }
        }
        return null;
    }

    public String getLocation() {
        return city + ", " + country;
    }

    public String searchableText() {
        String sectionText = sections.stream()
                .map(SeatSection::searchableText)
                .collect(java.util.stream.Collectors.joining(" "));
        return String.join(" ", id, name, city, country, venueType, description, address,
                shape.getLabel(), sectionText).toLowerCase(Locale.ENGLISH);
    }

    public String initials() {
        // Ignore punctuation so names such as "Hamz Stadium (Nakivubo)" still
        // produce a readable two-letter badge.
        List<String> words = java.util.Arrays.stream(name.trim().split("\\s+"))
                .map(word -> word.replaceAll("[^\\p{L}\\p{N}]", ""))
                .filter(word -> !word.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        if (words.isEmpty()) {
            return "UG";
        }
        if (words.size() == 1) {
            String only = words.get(0);
            return only.substring(0, Math.min(2, only.length())).toUpperCase(Locale.ENGLISH);
        }
        return (words.get(0).substring(0, 1) + words.get(words.size() - 1).substring(0, 1))
                .toUpperCase(Locale.ENGLISH);
    }
}
