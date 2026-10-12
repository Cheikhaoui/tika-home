package com.tikahome.service;

import com.tikahome.dto.admin.AdminBookingDetail;
import com.tikahome.dto.admin.AdminBookingSummary;
import com.tikahome.dto.admin.BlockSummary;
import com.tikahome.dto.admin.ConfirmResponse;
import com.tikahome.dto.admin.PageResponse;
import com.tikahome.entity.Booking;
import com.tikahome.entity.BookingStatus;
import com.tikahome.entity.Channel;
import com.tikahome.entity.SyncStatus;
import com.tikahome.exception.BookingConflictException;
import com.tikahome.exception.NotFoundException;
import com.tikahome.mapper.AdminMapper;
import com.tikahome.repository.BlockedDateRepository;
import com.tikahome.repository.BookingRepository;
import com.tikahome.repository.ChannelRepository;
import com.tikahome.repository.PropertyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
public class AdminBookingService {

    private static final ZoneId MARRAKECH = ZoneId.of("Africa/Casablanca");

    /** Admin list tabs. */
    public enum View { PENDING, UPCOMING, PAST, CLOSED }

    private final BookingRepository bookings;
    private final BlockedDateRepository blockedDates;
    private final PropertyRepository properties;
    private final ChannelRepository channels;
    private final ChannelSyncService channelSync;
    private final NotificationService notifications;
    private final AdminMapper mapper;
    private final TransactionTemplate tx;
    private final Clock clock;

    public AdminBookingService(BookingRepository bookings, BlockedDateRepository blockedDates,
                               PropertyRepository properties, ChannelRepository channels,
                               ChannelSyncService channelSync, NotificationService notifications,
                               AdminMapper mapper, TransactionTemplate tx, Clock clock) {
        this.bookings = bookings;
        this.blockedDates = blockedDates;
        this.properties = properties;
        this.channels = channels;
        this.channelSync = channelSync;
        this.notifications = notifications;
        this.mapper = mapper;
        this.tx = tx;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminBookingSummary> list(View view, int page, int size) {
        LocalDate today = LocalDate.now(clock.withZone(MARRAKECH));
        var byCheckIn = PageRequest.of(page, size, Sort.by("checkIn"));
        var latestFirst = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "checkIn"));
        Page<Booking> result = switch (view) {
            case PENDING -> bookings.findByStatusIn(EnumSet.of(BookingStatus.REQUESTED), byCheckIn);
            case UPCOMING -> bookings.findByStatusAndCheckOutAfter(BookingStatus.CONFIRMED, today, byCheckIn);
            case PAST -> bookings.findByStatusAndCheckOutLessThanEqual(BookingStatus.CONFIRMED, today, latestFirst);
            case CLOSED -> bookings.findByStatusIn(EnumSet.of(BookingStatus.DECLINED, BookingStatus.CANCELLED),
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        };
        return PageResponse.of(result, mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public AdminBookingDetail get(UUID id) {
        Booking booking = find(id);
        return mapper.toDetail(booking, overlappingBlocks(booking));
    }

    /**
     * Confirms a request, following the cross-platform policy (CLAUDE.md):
     * 1. re-sync the platform calendars first, so a reservation just made on Airbnb is known;
     * 2. then, under the property lock, refuse if any block overlaps the stay (409 with the details).
     * Channel refresh failures don't block the owner; they are returned as warnings.
     */
    public ConfirmResponse confirm(UUID id) {
        UUID propertyId = tx.execute(s -> find(id).getProperty().getId());
        List<String> warnings = refreshChannels(propertyId); // HTTP calls: outside any transaction

        AdminBookingDetail detail = tx.execute(s -> {
            properties.findByIdForUpdate(propertyId).orElseThrow();
            Booking booking = find(id);
            List<BlockSummary> conflicts = overlappingBlocks(booking);
            if (booking.getStatus() == BookingStatus.REQUESTED && !conflicts.isEmpty()) {
                throw new BookingConflictException(conflicts);
            }
            booking.confirm(); // throws InvalidBookingStateException (409) if not REQUESTED
            notifications.bookingConfirmed(booking);
            return mapper.toDetail(booking, List.of());
        });
        return new ConfirmResponse(detail, warnings);
    }

    @Transactional
    public AdminBookingDetail decline(UUID id, String ownerMessage) {
        Booking booking = find(id);
        booking.decline();
        notifications.bookingDeclined(booking, ownerMessage);
        return mapper.toDetail(booking, overlappingBlocks(booking));
    }

    private List<String> refreshChannels(UUID propertyId) {
        List<String> warnings = new ArrayList<>();
        for (Channel channel : channels.findByPropertyIdAndEnabledTrue(propertyId)) {
            var result = channelSync.sync(channel.getId());
            if (result.status() == SyncStatus.FAILED) {
                warnings.add(channel.getName() + ": " + result.error());
            }
        }
        return warnings;
    }

    private Booking find(UUID id) {
        return bookings.findById(id).orElseThrow(() -> new NotFoundException("Booking " + id + " not found"));
    }

    private List<BlockSummary> overlappingBlocks(Booking booking) {
        return mapper.toBlockSummaries(blockedDates.findOverlapping(booking.getProperty().getId(),
                booking.getCheckIn(), booking.getCheckOut()));
    }
}
