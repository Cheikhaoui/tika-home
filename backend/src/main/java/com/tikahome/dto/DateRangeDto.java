package com.tikahome.dto;

import java.time.LocalDate;

/** {@code end} is exclusive (the departure day). */
public record DateRangeDto(LocalDate start, LocalDate end) {
}
