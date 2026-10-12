package com.tikahome.service;

import com.tikahome.dto.admin.DashboardResponse;
import com.tikahome.dto.admin.DashboardResponse.Conflict;
import com.tikahome.entity.BlockSource;
import com.tikahome.entity.Booking;
import com.tikahome.entity.BookingStatus;
import com.tikahome.entity.SyncStatus;
import com.tikahome.exception.NotFoundException;
import com.tikahome.mapper.AdminMapper;
import com.tikahome.repository.BlockedDateRepository;
import com.tikahome.repository.BookingRepository;
import com.tikahome.repository.ChannelRepository;
import com.tikahome.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AdminDashboardService {

    private final PropertyRepository properties;
    private final BookingRepository bookings;
    private final BlockedDateRepository blockedDates;
    private final ChannelRepository channels;
    private final AdminMapper mapper;
    private final Clock clock;

    public AdminDashboardService(PropertyRepository properties, BookingRepository bookings,
                                 BlockedDateRepository blockedDates, ChannelRepository channels,
                                 AdminMapper mapper, Clock clock) {
        this.properties = properties;
        this.bookings = bookings;
        this.blockedDates = blockedDates;
        this.channels = channels;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(UUID propertyId) {
        var property = properties.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Property " + propertyId + " not found"));
        LocalDate today = LocalDate.now(clock.withZone(property.getZoneId()));

        var pending = bookings.findTop5ByPropertyIdAndStatusOrderByCreatedAtAsc(propertyId, BookingStatus.REQUESTED);
        var current = bookings.findFirstByPropertyIdAndStatusAndCheckInLessThanEqualAndCheckOutAfter(
                propertyId, BookingStatus.CONFIRMED, today, today);
        var arrivals = bookings.findTop5ByPropertyIdAndStatusAndCheckInGreaterThanEqualOrderByCheckInAsc(
                propertyId, BookingStatus.CONFIRMED, today);

        return new DashboardResponse(
                bookings.countByPropertyIdAndStatus(propertyId, BookingStatus.REQUESTED),
                pending.stream().map(mapper::toSummary).toList(),
                current.map(mapper::toSummary).orElse(null),
                arrivals.stream().map(mapper::toSummary).toList(),
                conflicts(propertyId, today),
                channels.findByPropertyIdOrderByName(propertyId).stream()
                        .filter(c -> c.getLastSyncStatus() == SyncStatus.FAILED)
                        .map(mapper::toChannelDto).toList());
    }

    private List<Conflict> conflicts(UUID propertyId, LocalDate today) {
        List<Conflict> result = new ArrayList<>();
        for (Booking booking : bookings.findEndingAfter(propertyId, today, BookingStatus.ACTIVE)) {
            blockedDates.findOverlapping(propertyId, booking.getCheckIn(), booking.getCheckOut()).stream()
                    .filter(b -> b.getSource() == BlockSource.ICAL)
                    .forEach(b -> result.add(new Conflict(mapper.toSummary(booking), mapper.toBlockSummary(b))));
        }
        return result;
    }
}
