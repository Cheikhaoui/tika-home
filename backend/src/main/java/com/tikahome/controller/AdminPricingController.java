package com.tikahome.controller;

import com.tikahome.dto.admin.PriceRuleDto;
import com.tikahome.dto.admin.PricingResponse;
import com.tikahome.dto.admin.PropertySettings;
import com.tikahome.service.AdminPricingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminPricingController {

    private final AdminPricingService service;

    public AdminPricingController(AdminPricingService service) {
        this.service = service;
    }

    @GetMapping("/properties/{propertyId}/pricing")
    public PricingResponse get(@PathVariable UUID propertyId) {
        return service.get(propertyId);
    }

    @Operation(summary = "Base price, cleaning fee, min nights, max guests (existing bookings keep their price)")
    @PutMapping("/properties/{propertyId}/settings")
    public PricingResponse updateSettings(@PathVariable UUID propertyId, @Valid @RequestBody PropertySettings settings) {
        return service.updateSettings(propertyId, settings);
    }

    @PostMapping("/properties/{propertyId}/price-rules")
    @ResponseStatus(HttpStatus.CREATED)
    public PriceRuleDto createRule(@PathVariable UUID propertyId, @Valid @RequestBody PriceRuleDto rule) {
        return service.createRule(propertyId, rule);
    }

    @PutMapping("/price-rules/{ruleId}")
    public PriceRuleDto updateRule(@PathVariable UUID ruleId, @Valid @RequestBody PriceRuleDto rule) {
        return service.updateRule(ruleId, rule);
    }

    @DeleteMapping("/price-rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable UUID ruleId) {
        service.deleteRule(ruleId);
    }
}
