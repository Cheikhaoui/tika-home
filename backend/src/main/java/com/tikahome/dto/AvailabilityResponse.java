package com.tikahome.dto;

import com.tikahome.model.Availability;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AvailabilityResponse(UUID propertyId, LocalDate from, LocalDate to,
                                   List<DateRangeDto> unavailable, List<DayDto> days) {

    public static AvailabilityResponse from(UUID propertyId, Availability a) {
        List<DateRangeDto> ranges = a.unavailable().stream()
                .map(r -> new DateRangeDto(r.start(), r.end()))
                .toList();
        List<DayDto> days = a.range().nightsStream()
                .map(d -> new DayDto(d, a.isAvailable(d)))
                .toList();
        return new AvailabilityResponse(propertyId, a.range().start(), a.range().end(), ranges, days);
    }
}
