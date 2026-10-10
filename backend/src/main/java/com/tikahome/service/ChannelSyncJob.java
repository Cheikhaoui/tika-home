package com.tikahome.service;

import com.tikahome.entity.Channel;
import com.tikahome.repository.ChannelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Imports every enabled channel periodically. Disabled with tikahome.ical.sync-enabled=false (tests). */
@Component
@ConditionalOnProperty(name = "tikahome.ical.sync-enabled", havingValue = "true", matchIfMissing = true)
public class ChannelSyncJob {

    private static final Logger log = LoggerFactory.getLogger(ChannelSyncJob.class);

    private final ChannelRepository channels;
    private final ChannelSyncService syncService;

    public ChannelSyncJob(ChannelRepository channels, ChannelSyncService syncService) {
        this.channels = channels;
        this.syncService = syncService;
    }

    /** fixedDelay: the next run starts only after the previous one finished, so runs never overlap. */
    @Scheduled(fixedDelayString = "${tikahome.ical.sync-interval}", initialDelayString = "${tikahome.ical.initial-delay}")
    public void syncAll() {
        for (Channel channel : channels.findByEnabledTrue()) {
            try {
                syncService.sync(channel.getId());
            } catch (RuntimeException e) {
                // Expected failures (HTTP errors, bad feeds) are handled inside sync(); this is a safety
                // net so one unexpected error never stops the other channels from syncing.
                log.error("Unexpected error syncing channel {}", channel.getId(), e);
            }
        }
    }
}
