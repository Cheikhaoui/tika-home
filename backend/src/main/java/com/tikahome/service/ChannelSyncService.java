package com.tikahome.service;

import com.tikahome.entity.BlockedDate;
import com.tikahome.entity.BookingStatus;
import com.tikahome.entity.Channel;
import com.tikahome.entity.Property;
import com.tikahome.exception.FeedFetchException;
import com.tikahome.exception.IcalParseException;
import com.tikahome.exception.NotFoundException;
import com.tikahome.model.ExternalEvent;
import com.tikahome.model.SyncResult;
import com.tikahome.repository.BlockedDateRepository;
import com.tikahome.repository.BookingRepository;
import com.tikahome.repository.ChannelRepository;
import com.tikahome.repository.PropertyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Imports a channel's iCal feed as ICAL blocked dates.
 * <ol>
 *   <li>Download + parse <b>outside</b> any transaction: HTTP can take seconds and we must not hold
 *       the property lock (which every booking request needs) while waiting on Airbnb.</li>
 *   <li>Apply the diff in a short transaction that first locks the property row, like every other
 *       write that changes availability: insert new events, update moved ones, delete vanished ones.</li>
 *   <li>A failed download or invalid feed changes nothing: an outage on the platform's side must never
 *       make reserved dates bookable here.</li>
 * </ol>
 */
@Service
public class ChannelSyncService {

    private static final Logger log = LoggerFactory.getLogger(ChannelSyncService.class);
    private static final int MAX_REASON_LENGTH = 200;

    private final ChannelRepository channels;
    private final PropertyRepository properties;
    private final BlockedDateRepository blockedDates;
    private final BookingRepository bookings;
    private final IcalFeedClient feedClient;
    private final TransactionTemplate tx;
    private final Clock clock;

    public ChannelSyncService(ChannelRepository channels, PropertyRepository properties,
                              BlockedDateRepository blockedDates, BookingRepository bookings,
                              IcalFeedClient feedClient, TransactionTemplate tx, Clock clock) {
        this.channels = channels;
        this.properties = properties;
        this.blockedDates = blockedDates;
        this.bookings = bookings;
        this.feedClient = feedClient;
        this.tx = tx;
        this.clock = clock;
    }

    public SyncResult sync(UUID channelId) {
        FeedTarget target = tx.execute(status -> {
            Channel channel = channels.findById(channelId)
                    .orElseThrow(() -> new NotFoundException("Channel " + channelId + " not found"));
            return new FeedTarget(channel.getName(), channel.getImportUrl(), channel.getProperty().getZoneId());
        });

        List<ExternalEvent> events;
        try {
            events = IcalParser.parse(feedClient.fetch(target.url()), target.zone());
        } catch (FeedFetchException | IcalParseException e) {
            log.warn("Channel '{}' ({}) sync failed, keeping existing blocks: {}", target.name(), channelId, e.getMessage());
            tx.executeWithoutResult(status -> channels.findById(channelId)
                    .ifPresent(c -> c.recordFailure(clock.instant(), e.getMessage())));
            return SyncResult.failed(e.getMessage());
        }

        SyncResult result = tx.execute(status -> apply(channelId, events));
        log.info("Channel '{}' ({}) synced: {} created, {} updated, {} deleted, {} conflict(s)", target.name(),
                channelId, result.created(), result.updated(), result.deleted(), result.conflictingBookingIds().size());
        return result;
    }

    private SyncResult apply(UUID channelId, List<ExternalEvent> feed) {
        Channel channel = channels.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Channel " + channelId + " not found"));
        Property property = properties.findByIdForUpdate(channel.getProperty().getId())
                .orElseThrow(() -> new NotFoundException("Property not found"));
        LocalDate today = LocalDate.now(clock.withZone(property.getZoneId()));

        // Past stays don't affect availability; a duplicated UID keeps its first occurrence.
        Map<String, ExternalEvent> wanted = feed.stream()
                .filter(e -> e.range().end().isAfter(today))
                .collect(Collectors.toMap(ExternalEvent::uid, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<String, BlockedDate> existing = blockedDates.findByChannelId(channelId).stream()
                .collect(Collectors.toMap(BlockedDate::getExternalUid, Function.identity()));

        int created = 0;
        int updated = 0;
        int deleted = 0;
        for (ExternalEvent event : wanted.values()) {
            String reason = reason(channel, event);
            BlockedDate block = existing.get(event.uid());
            if (block == null) {
                blockedDates.save(BlockedDate.imported(channel, event.uid(), event.range().start(),
                        event.range().end(), reason));
                created++;
            } else if (block.update(event.range().start(), event.range().end(), reason)) {
                updated++; // saved by JPA dirty checking at commit
            }
        }
        for (BlockedDate block : existing.values()) {
            if (!wanted.containsKey(block.getExternalUid())) {
                blockedDates.delete(block); // cancelled or removed on the platform
                deleted++;
            }
        }

        Set<UUID> conflicts = new LinkedHashSet<>();
        for (ExternalEvent event : wanted.values()) {
            bookings.findOverlapping(property.getId(), event.range().start(), event.range().end(), BookingStatus.ACTIVE)
                    .forEach(b -> conflicts.add(b.getId()));
        }
        if (!conflicts.isEmpty()) {
            // A reservation was taken on the platform while a direct booking holds the same dates.
            // We can't refuse it (it's confirmed there), so alert: the owner resolves it (see CLAUDE.md).
            log.warn("DOUBLE BOOKING: channel '{}' ({}) has reservations overlapping direct booking(s) {}",
                    channel.getName(), channelId, conflicts);
        }

        channel.recordSuccess(clock.instant(), conflicts.size());
        return SyncResult.success(created, updated, deleted, List.copyOf(conflicts));
    }

    private static String reason(Channel channel, ExternalEvent event) {
        String reason = event.summary() == null ? channel.getName() : channel.getName() + ": " + event.summary();
        return reason.length() > MAX_REASON_LENGTH ? reason.substring(0, MAX_REASON_LENGTH) : reason;
    }

    private record FeedTarget(String name, String url, ZoneId zone) {
    }
}
