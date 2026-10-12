package com.tikahome.controller;

import com.tikahome.dto.admin.AdminBookingDetail;
import com.tikahome.dto.admin.AdminBookingSummary;
import com.tikahome.dto.admin.ConfirmResponse;
import com.tikahome.dto.admin.DeclineRequest;
import com.tikahome.dto.admin.PageResponse;
import com.tikahome.service.AdminBookingService;
import com.tikahome.service.AdminBookingService.View;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    private final AdminBookingService service;

    public AdminBookingController(AdminBookingService service) {
        this.service = service;
    }

    @Operation(summary = "Bookings by tab: PENDING (requests), UPCOMING, PAST, CLOSED (declined/cancelled)")
    @GetMapping
    public PageResponse<AdminBookingSummary> list(@RequestParam(defaultValue = "PENDING") View view,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(view, page, size);
    }

    @GetMapping("/{id}")
    public AdminBookingDetail get(@PathVariable UUID id) {
        return service.get(id);
    }

    @Operation(summary = "Confirm a request: re-syncs channels first; 409 if blocked dates overlap or not REQUESTED")
    @PostMapping("/{id}/confirm")
    public ConfirmResponse confirm(@PathVariable UUID id) {
        return service.confirm(id);
    }

    @Operation(summary = "Decline a request (optional message to the guest); releases the dates")
    @PostMapping("/{id}/decline")
    public AdminBookingDetail decline(@PathVariable UUID id, @Valid @RequestBody(required = false) DeclineRequest body) {
        return service.decline(id, body == null ? null : body.message());
    }
}
