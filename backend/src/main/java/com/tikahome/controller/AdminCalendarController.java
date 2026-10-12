package com.tikahome.controller;

import com.tikahome.dto.admin.AdminCalendarResponse;
import com.tikahome.dto.admin.BlockSummary;
import com.tikahome.dto.admin.CreateBlockRequest;
import com.tikahome.service.AdminCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminCalendarController {

    private final AdminCalendarService service;

    public AdminCalendarController(AdminCalendarService service) {
        this.service = service;
    }

    @Operation(summary = "Bookings and blocks overlapping [from, to) for the admin calendar")
    @GetMapping("/properties/{propertyId}/calendar")
    public AdminCalendarResponse calendar(@PathVariable UUID propertyId,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.calendar(propertyId, from, to);
    }

    @Operation(summary = "Block nights [startDate, endDate); 409 if a booking holds them")
    @PostMapping("/properties/{propertyId}/blocked-dates")
    @ResponseStatus(HttpStatus.CREATED)
    public BlockSummary block(@PathVariable UUID propertyId, @Valid @RequestBody CreateBlockRequest request) {
        return service.block(propertyId, request);
    }

    @Operation(summary = "Remove a manual block (imported blocks are managed on their platform)")
    @DeleteMapping("/blocked-dates/{blockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@PathVariable UUID blockId) {
        service.unblock(blockId);
    }
}
