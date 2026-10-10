package com.tikahome.dto;

import java.time.LocalDate;

/** {@code available} = the night starting on {@code date} can be booked. */
public record DayDto(LocalDate date, boolean available) {
}
