package com.tikahome.model;

import java.time.LocalDate;
import java.util.List;

/** Unavailable (merged) ranges within {@code range}. */
public record Availability(DateRange range, List<DateRange> unavailable) {

    public boolean isAvailable(LocalDate night) {
        return unavailable.stream().noneMatch(r -> r.contains(night));
    }

    public boolean isFullyAvailable() {
        return unavailable.isEmpty();
    }
}
