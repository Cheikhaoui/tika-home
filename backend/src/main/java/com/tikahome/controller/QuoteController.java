package com.tikahome.controller;

import com.tikahome.dto.QuoteResponse;
import com.tikahome.model.DateRange;
import com.tikahome.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @Operation(summary = "Price a stay. Always 200 for valid input; check `bookable` and `issues`.")
    @GetMapping("/api/properties/{propertyId}/quote")
    public QuoteResponse quote(
            @PathVariable UUID propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam @Min(1) int guests) {
        return QuoteResponse.from(quoteService.quote(propertyId, new DateRange(checkIn, checkOut), guests));
    }
}
