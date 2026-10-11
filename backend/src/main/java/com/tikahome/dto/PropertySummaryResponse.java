package com.tikahome.dto;

import com.tikahome.entity.Property;

import java.math.BigDecimal;
import java.util.UUID;

/** Public facts the website needs: capacity, "from" price, minimum stay. */
public record PropertySummaryResponse(UUID id, String name, int maxGuests, BigDecimal baseNightlyPrice,
                                      BigDecimal cleaningFee, String currency, int minNights) {

    public static PropertySummaryResponse from(Property p) {
        return new PropertySummaryResponse(p.getId(), p.getName(), p.getMaxGuests(), p.getBaseNightlyPrice(),
                p.getCleaningFee(), p.getCurrency(), p.getMinNights());
    }
}
