package com.tikahome.service;

import com.tikahome.entity.PriceRule;
import com.tikahome.entity.Property;
import com.tikahome.model.DateRange;
import com.tikahome.model.NightPrice;
import com.tikahome.model.Pricing;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Pure pricing logic (no Spring, no database) so it's trivial to unit test.
 * <ul>
 *   <li>Each night costs the price of the highest-priority rule covering it, or the property base price.</li>
 *   <li>Minimum nights come from the rule covering the check-in night (same as Airbnb/Booking.com).</li>
 * </ul>
 */
public final class PriceCalculator {

    private PriceCalculator() {
    }

    public static Pricing calculate(Property property, List<PriceRule> rules, DateRange stay) {
        List<NightPrice> nights = stay.nightsStream()
                .map(night -> {
                    Optional<PriceRule> rule = ruleFor(night, rules);
                    return new NightPrice(night,
                            rule.map(PriceRule::getNightlyPrice).orElse(property.getBaseNightlyPrice()),
                            rule.map(PriceRule::getName).orElse(null));
                })
                .toList();
        BigDecimal subtotal = nights.stream().map(NightPrice::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cleaningFee = property.getCleaningFee();
        int minNights = ruleFor(stay.start(), rules)
                .map(PriceRule::getMinNights) // empty when the rule has no min_nights of its own
                .orElse(property.getMinNights());
        return new Pricing(nights, subtotal, cleaningFee, subtotal.add(cleaningFee), minNights);
    }

    private static Optional<PriceRule> ruleFor(LocalDate night, List<PriceRule> rules) {
        return rules.stream()
                .filter(r -> r.covers(night))
                .max(Comparator.comparingInt(PriceRule::getPriority));
    }
}
