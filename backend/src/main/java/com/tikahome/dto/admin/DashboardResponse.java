package com.tikahome.dto.admin;

import java.util.List;

/** What needs the owner's attention, at a glance. */
public record DashboardResponse(long pendingCount, List<AdminBookingSummary> pending,
                                AdminBookingSummary currentStay, List<AdminBookingSummary> nextArrivals,
                                List<Conflict> conflicts, List<ChannelDto> failingChannels) {

    /** A direct booking overlapping a reservation imported from a platform: a real double booking. */
    public record Conflict(AdminBookingSummary booking, BlockSummary platformReservation) {
    }
}
