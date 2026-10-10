package com.tikahome.pricing;

import com.tikahome.common.time.DateRange;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record Quote(DateRange stay, int guests, String currency, PriceCalculator.Pricing pricing,
                    Set<QuoteIssue> issues) {

    public boolean bookable() {
        return issues.isEmpty();
    }

    public long nights() {
        return stay.nights();
    }

    public BigDecimal total() {
        return pricing.total();
    }

    public List<PriceCalculator.NightPrice> nightPrices() {
        return pricing.nights();
    }
}
