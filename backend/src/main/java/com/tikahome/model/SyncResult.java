package com.tikahome.model;

import com.tikahome.entity.SyncStatus;

import java.util.List;
import java.util.UUID;

/**
 * Outcome of importing one channel. {@code conflictingBookingIds} are direct bookings that overlap a
 * reservation imported from the channel: a real double booking the owner must resolve.
 */
public record SyncResult(SyncStatus status, int created, int updated, int deleted,
                         List<UUID> conflictingBookingIds, String error) {

    public static SyncResult success(int created, int updated, int deleted, List<UUID> conflicts) {
        return new SyncResult(SyncStatus.SUCCESS, created, updated, deleted, List.copyOf(conflicts), null);
    }

    public static SyncResult failed(String error) {
        return new SyncResult(SyncStatus.FAILED, 0, 0, 0, List.of(), error);
    }
}
