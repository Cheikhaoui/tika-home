package com.tikahome.controller;

import com.tikahome.dto.admin.DashboardResponse;
import com.tikahome.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class AdminDashboardController {

    private final AdminDashboardService service;

    public AdminDashboardController(AdminDashboardService service) {
        this.service = service;
    }

    @Operation(summary = "Pending requests, current stay, next arrivals, double bookings, failing channels")
    @GetMapping("/api/admin/properties/{propertyId}/dashboard")
    public DashboardResponse dashboard(@PathVariable UUID propertyId) {
        return service.dashboard(propertyId);
    }
}
