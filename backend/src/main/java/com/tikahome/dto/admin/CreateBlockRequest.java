package com.tikahome.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Owner blocks the nights [startDate, endDate) (endDate exclusive, like a check-out). */
public record CreateBlockRequest(@NotNull LocalDate startDate, @NotNull LocalDate endDate,
                                 @Size(max = 200) String reason) {
}
