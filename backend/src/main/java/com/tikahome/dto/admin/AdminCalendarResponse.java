package com.tikahome.dto.admin;

import java.time.LocalDate;
import java.util.List;

/** Everything occupying [from, to): active bookings and blocks (manual + imported). */
public record AdminCalendarResponse(LocalDate from, LocalDate to, List<AdminBookingSummary> bookings,
                                    List<BlockSummary> blocks) {
}
