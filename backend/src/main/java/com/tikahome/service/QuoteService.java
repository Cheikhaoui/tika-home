package com.tikahome.service;

import com.tikahome.entity.Property;
import com.tikahome.exception.NotFoundException;
import com.tikahome.model.DateRange;
import com.tikahome.model.Quote;
import com.tikahome.model.QuoteIssue;
import com.tikahome.repository.PriceRuleRepository;
import com.tikahome.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class QuoteService {

    static final int MAX_NIGHTS = 90;

    private final PropertyRepository properties;
    private final PriceRuleRepository priceRules;
    private final AvailabilityService availability;

    public QuoteService(PropertyRepository properties, PriceRuleRepository priceRules,
                        AvailabilityService availability) {
        this.properties = properties;
        this.priceRules = priceRules;
        this.availability = availability;
    }

    @Transactional(readOnly = true)
    public Quote quote(UUID propertyId, DateRange stay, int guests) {
        Property property = properties.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Property " + propertyId + " not found"));
        return quote(property, stay, guests);
    }

    /** Also used by the booking service, inside its locked transaction, with the already-locked property. */
    public Quote quote(Property property, DateRange stay, int guests) {
        var pricing = PriceCalculator.calculate(property,
                priceRules.findOverlapping(property.getId(), stay.start(), stay.end()), stay);

        Set<QuoteIssue> issues = EnumSet.noneOf(QuoteIssue.class);
        if (stay.start().isBefore(availability.today(property))) {
            issues.add(QuoteIssue.CHECK_IN_IN_PAST);
        } else if (!availability.unavailableRanges(property, stay).isEmpty()) {
            issues.add(QuoteIssue.DATES_UNAVAILABLE);
        }
        if (stay.nights() < pricing.minNights()) {
            issues.add(QuoteIssue.BELOW_MIN_NIGHTS);
        }
        if (stay.nights() > MAX_NIGHTS) {
            issues.add(QuoteIssue.ABOVE_MAX_NIGHTS);
        }
        if (guests > property.getMaxGuests()) {
            issues.add(QuoteIssue.TOO_MANY_GUESTS);
        }
        return new Quote(stay, guests, property.getCurrency(), pricing, issues);
    }
}
