package com.tikahome.dto.admin;

import com.tikahome.entity.ChannelType;
import com.tikahome.entity.SyncStatus;

import java.time.Instant;
import java.util.UUID;

/** A channel as shown to the owner. The import URL contains a secret: only a masked version is returned. */
public record ChannelDto(UUID id, ChannelType type, String name, String importUrlMasked, boolean enabled,
                         Instant lastSyncedAt, Instant lastSuccessAt, SyncStatus lastSyncStatus, String lastError,
                         int lastConflictCount, int consecutiveFailures) {
}
