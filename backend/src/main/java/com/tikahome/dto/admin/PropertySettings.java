package com.tikahome.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Pricing basics the owner can edit. */
public record PropertySettings(
        @NotNull @DecimalMin(value = "1.00") @Digits(integer = 8, fraction = 2) BigDecimal baseNightlyPrice,
        @NotNull @DecimalMin(value = "0.00") @Digits(integer = 8, fraction = 2) BigDecimal cleaningFee,
        @Min(1) @Max(30) int minNights,
        @Min(1) @Max(30) int maxGuests) {
}
