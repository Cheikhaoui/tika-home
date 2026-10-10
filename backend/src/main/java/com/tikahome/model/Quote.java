package com.tikahome.model;

import java.math.BigDecimal;
import java.util.Set;

public record Quote(DateRange stay, int guests, String currency, Pricing pricing, Set<QuoteIssue> issues) {

    public boolean bookable() {
        return issues.isEmpty();
    }

    public long nights() {
        return stay.nights();
    }

    public BigDecimal total() {
        return pricing.total();
    }
}
