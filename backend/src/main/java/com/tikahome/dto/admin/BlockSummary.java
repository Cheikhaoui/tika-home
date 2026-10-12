package com.tikahome.dto.admin;

import com.tikahome.entity.BlockSource;

import java.time.LocalDate;
import java.util.UUID;

/** A blocked period as shown to the owner. {@code channelName} is set for ICAL blocks (e.g. "Airbnb"). */
public record BlockSummary(UUID id, BlockSource source, String channelName, LocalDate startDate, LocalDate endDate,
                           String reason) {
}
