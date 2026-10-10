package com.tikahome.controller;

import com.tikahome.dto.AvailabilityResponse;
import com.tikahome.model.DateRange;
import com.tikahome.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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
}
