package com.tikahome.availability;

import com.tikahome.common.time.DateRange;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @Operation(summary = "Availability calendar for [from, to) — `to` is exclusive, max 366 days")
    @GetMapping("/api/properties/{propertyId}/availability")
    public AvailabilityResponse availability(
            @PathVariable UUID propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var availability = availabilityService.getAvailability(propertyId, new DateRange(from, to));
        return AvailabilityResponse.from(propertyId, availability);
    }

    public record AvailabilityResponse(UUID propertyId, LocalDate from, LocalDate to,
                                       List<RangeDto> unavailable, List<DayDto> days) {

        static AvailabilityResponse from(UUID propertyId, AvailabilityService.Availability a) {
            List<RangeDto> ranges = a.unavailable().stream()
                    .map(r -> new RangeDto(r.start(), r.end()))
                    .toList();
            List<DayDto> days = a.range().nightsStream()
                    .map(d -> new DayDto(d, a.isAvailable(d)))
                    .toList();
            return new AvailabilityResponse(propertyId, a.range().start(), a.range().end(), ranges, days);
        }
    }

    /** {@code end} is exclusive (the departure day). */
    public record RangeDto(LocalDate start, LocalDate end) {
    }

    /** {@code available} = the night starting on {@code date} can be booked. */
    public record DayDto(LocalDate date, boolean available) {
    }
}
