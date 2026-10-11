package com.tikahome.controller;

import com.tikahome.dto.PropertySummaryResponse;
import com.tikahome.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Operation(summary = "Public property facts: capacity, base price, minimum nights")
    @GetMapping("/api/properties/{propertyId}")
    public PropertySummaryResponse get(@PathVariable UUID propertyId) {
        return PropertySummaryResponse.from(propertyService.get(propertyId));
    }
}
