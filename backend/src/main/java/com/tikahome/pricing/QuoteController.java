package com.tikahome.pricing;

import com.tikahome.common.time.DateRange;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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

    public record QuoteResponse(LocalDate checkIn, LocalDate checkOut, long nights, int guests, String currency,
                                List<PriceCalculator.NightPrice> nightly, BigDecimal subtotal,
                                BigDecimal cleaningFee, BigDecimal total, int minNights,
                                boolean bookable, Set<QuoteIssue> issues) {

        public static QuoteResponse from(Quote q) {
            var p = q.pricing();
            return new QuoteResponse(q.stay().start(), q.stay().end(), q.nights(), q.guests(), q.currency(),
                    p.nights(), p.subtotal(), p.cleaningFee(), p.total(), p.minNights(), q.bookable(), q.issues());
        }
    }
}
