package com.tikahome.service;

import com.tikahome.dto.admin.AdminCalendarResponse;
import com.tikahome.dto.admin.BlockSummary;
import com.tikahome.dto.admin.CreateBlockRequest;
import com.tikahome.entity.BlockSource;
import com.tikahome.entity.BlockedDate;
import com.tikahome.entity.BookingStatus;
import com.tikahome.entity.Property;
import com.tikahome.exception.BadRequestException;
import com.tikahome.exception.BlockConflictException;
import com.tikahome.exception.NotFoundException;
import com.tikahome.mapper.AdminMapper;
import com.tikahome.model.DateRange;
import com.tikahome.repository.BlockedDateRepository;
import com.tikahome.repository.BookingRepository;
import com.tikahome.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class AdminCalendarService {

    static final int MAX_RANGE_DAYS = 366;

    private final PropertyRepository properties;
    private final BookingRepository bookings;
    private final BlockedDateRepository blockedDates;
    private final AdminMapper mapper;

    public AdminCalendarService(PropertyRepository properties, BookingRepository bookings,
                                BlockedDateRepository blockedDates, AdminMapper mapper) {
        this.properties = properties;
        this.bookings = bookings;
        this.blockedDates = blockedDates;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public AdminCalendarResponse calendar(UUID propertyId, LocalDate from, LocalDate to) {
        DateRange range = limited(new DateRange(from, to));
        requireProperty(propertyId);
        return new AdminCalendarResponse(range.start(), range.end(),
                bookings.findOverlapping(propertyId, range.start(), range.end(), BookingStatus.ACTIVE).stream()
                        .map(mapper::toSummary).toList(),
                mapper.toBlockSummaries(blockedDates.findOverlapping(propertyId, range.start(), range.end())));
    }

    /**
     * Blocks dates for the owner. Same rule as every availability write: lock the property row first,
     * so a guest can't book these nights between our check and our insert.
     */
    @Transactional
    public BlockSummary block(UUID propertyId, CreateBlockRequest request) {
        DateRange range = limited(new DateRange(request.startDate(), request.endDate()));
        Property property = properties.findByIdForUpdate(propertyId)
                .orElseThrow(() -> new NotFoundException("Property " + propertyId + " not found"));
        var holding = bookings.findOverlapping(propertyId, range.start(), range.end(), BookingStatus.ACTIVE);
        if (!holding.isEmpty()) {
            throw new BlockConflictException(holding.stream().map(mapper::toSummary).toList());
        }
        String reason = request.reason() == null || request.reason().isBlank() ? null : request.reason().strip();
        return mapper.toBlockSummary(blockedDates.save(BlockedDate.manual(property, range.start(), range.end(), reason)));
    }

    /** Only the owner's own blocks; imported ones are removed by cancelling them on the platform. */
    @Transactional
    public void unblock(UUID blockId) {
        BlockedDate block = blockedDates.findById(blockId)
                .orElseThrow(() -> new NotFoundException("Blocked period " + blockId + " not found"));
        if (block.getSource() != BlockSource.MANUAL) {
            throw new BadRequestException("Imported blocks are managed on the platform they come from");
        }
        blockedDates.delete(block);
    }

    private void requireProperty(UUID propertyId) {
        if (!properties.existsById(propertyId)) {
            throw new NotFoundException("Property " + propertyId + " not found");
        }
    }

    private static DateRange limited(DateRange range) {
        if (range.nights() > MAX_RANGE_DAYS) {
            throw new BadRequestException("Range must not exceed " + MAX_RANGE_DAYS + " days");
        }
        return range;
    }
}
