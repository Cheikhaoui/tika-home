package com.tikahome.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A seasonal price for nights [startDate, endDate). Used for reading and writing ({@code id} is ignored
 * on create/update). When rules overlap, the highest {@code priority} wins for each night.
 */
public record PriceRuleDto(
        UUID id,
        @NotBlank @Size(max = 100) String name,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @DecimalMin(value = "1.00") @Digits(integer = 8, fraction = 2) BigDecimal nightlyPrice,
        @Min(1) @Max(30) Integer minNights,
        @Min(0) @Max(1000) int priority) {
}
