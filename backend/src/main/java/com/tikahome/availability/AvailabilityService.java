package com.tikahome.availability;

import com.tikahome.booking.BookingRepository;
import com.tikahome.booking.BookingStatus;
import com.tikahome.common.time.DateRange;
import com.tikahome.common.web.BadRequestException;
import com.tikahome.common.web.NotFoundException;
import com.tikahome.property.Property;
import com.tikahome.property.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class AvailabilityService {

    static final int MAX_RANGE_DAYS = 366;

    private final PropertyRepository properties;
    private final BookingRepository bookings;
    private final BlockedDateRepository blockedDates;
    private final Clock clock;

    public AvailabilityService(PropertyRepository properties, BookingRepository bookings,
                               BlockedDateRepository blockedDates, Clock clock) {
        this.properties = properties;
        this.bookings = bookings;
        this.blockedDates = blockedDates;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Availability getAvailability(UUID propertyId, DateRange range) {
        if (range.nights() > MAX_RANGE_DAYS) {
            throw new BadRequestException("Range must not exceed " + MAX_RANGE_DAYS + " days");
        }
        Property property = properties.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Property " + propertyId + " not found"));
        return new Availability(range, unavailableRanges(property, range));
    }

    /**
     * Merged unavailable ranges intersecting {@code range}: active bookings, blocked dates and past nights.
     * Called by the booking service inside its locked transaction, so the answer can't go stale before insert.
     */
    public List<DateRange> unavailableRanges(Property property, DateRange range) {
        Stream<DateRange> booked = bookings
                .findOverlapping(property.getId(), range.start(), range.end(), BookingStatus.ACTIVE).stream()
                .map(b -> new DateRange(b.getCheckIn(), b.getCheckOut()));
        Stream<DateRange> blocked = blockedDates
                .findOverlapping(property.getId(), range.start(), range.end()).stream()
                .map(b -> new DateRange(b.getStartDate(), b.getEndDate()));
        Stream<DateRange> past = pastNights(property, range);
        return DateRange.merge(Stream.of(booked, blocked, past).flatMap(s -> s).toList());
    }

    public LocalDate today(Property property) {
        return LocalDate.now(clock.withZone(property.getZoneId()));
    }

    private Stream<DateRange> pastNights(Property property, DateRange range) {
        LocalDate today = today(property);
        return range.start().isBefore(today)
                ? Stream.of(new DateRange(range.start(), today.isBefore(range.end()) ? today : range.end()))
                : Stream.empty();
    }

    public record Availability(DateRange range, List<DateRange> unavailable) {

        public boolean isAvailable(LocalDate night) {
            return unavailable.stream().noneMatch(r -> r.contains(night));
        }

        public boolean isFullyAvailable() {
            return unavailable.isEmpty();
        }
    }
}
