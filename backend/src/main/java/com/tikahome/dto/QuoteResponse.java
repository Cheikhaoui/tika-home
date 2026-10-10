package com.tikahome.dto;

import com.tikahome.model.NightPrice;
import com.tikahome.model.Quote;
import com.tikahome.model.QuoteIssue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record QuoteResponse(LocalDate checkIn, LocalDate checkOut, long nights, int guests, String currency,
                            List<NightPrice> nightly, BigDecimal subtotal, BigDecimal cleaningFee,
                            BigDecimal total, int minNights, boolean bookable, Set<QuoteIssue> issues) {

    public static QuoteResponse from(Quote q) {
        var p = q.pricing();
        return new QuoteResponse(q.stay().start(), q.stay().end(), q.nights(), q.guests(), q.currency(),
                p.nights(), p.subtotal(), p.cleaningFee(), p.total(), p.minNights(), q.bookable(), q.issues());
    }
}
