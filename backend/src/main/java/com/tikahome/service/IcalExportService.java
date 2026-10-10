package com.tikahome.service;

import com.tikahome.entity.BlockSource;
import com.tikahome.entity.BookingStatus;
import com.tikahome.entity.Property;
import com.tikahome.exception.NotFoundException;
import com.tikahome.repository.BlockedDateRepository;
import com.tikahome.repository.BookingRepository;
import com.tikahome.repository.PropertyRepository;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.Uid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Our calendar as iCal, for Airbnb / Booking.com to import.
 * <ul>
 *   <li>Includes active bookings (REQUESTED holds dates too) and the owner's MANUAL blocks.</li>
 *   <li>Excludes ICAL blocks: re-exporting an Airbnb reservation to Airbnb would make it echo between
 *       platforms and never disappear when cancelled.</li>
 *   <li>No personal data: platforms only need to know the dates are taken.</li>
 *   <li>Stable UIDs, so platforms update events instead of duplicating them.</li>
 * </ul>
 */
@Service
public class IcalExportService {

    private static final Pattern TOKEN_FORMAT = Pattern.compile("[0-9a-f]{64}");
    static final String RESERVED = "Reserved";
    static final String NOT_AVAILABLE = "Not available";

    private final PropertyRepository properties;
    private final BookingRepository bookings;
    private final BlockedDateRepository blockedDates;
    private final Clock clock;

    public IcalExportService(PropertyRepository properties, BookingRepository bookings,
                             BlockedDateRepository blockedDates, Clock clock) {
        this.properties = properties;
        this.bookings = bookings;
        this.blockedDates = blockedDates;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public String export(String token) {
        Property property = Optional.of(token)
                .filter(t -> TOKEN_FORMAT.matcher(t).matches()) // skip the DB for obviously wrong tokens
                .flatMap(properties::findByIcalExportToken)
                .orElseThrow(() -> new NotFoundException("Calendar not found"));
        LocalDate today = LocalDate.now(clock.withZone(property.getZoneId()));

        Calendar calendar = new Calendar()
                .withDefaults() // VERSION:2.0 and CALSCALE:GREGORIAN
                .withProdId("-//Tika Home//Direct Booking Calendar 1.0//EN")
                .withProperty(new Method(Method.VALUE_PUBLISH))
                .getFluentTarget();

        bookings.findEndingAfter(property.getId(), today, BookingStatus.ACTIVE).forEach(b ->
                calendar.add(event("booking-" + b.getId(), b.getCheckIn(), b.getCheckOut(), RESERVED)));
        blockedDates.findBySourceEndingAfter(property.getId(), BlockSource.MANUAL, today).forEach(b ->
                calendar.add(event("block-" + b.getId(), b.getStartDate(), b.getEndDate(), NOT_AVAILABLE)));

        StringWriter out = new StringWriter();
        try {
            new CalendarOutputter(false).output(calendar, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toString();
    }

    /** All-day event: DTSTART;VALUE=DATE = first night, DTEND = departure day (exclusive). */
    private static VEvent event(String id, LocalDate start, LocalDate end, String summary) {
        VEvent event = new VEvent(start, end, summary);
        event.add(new Uid(id + "@tikahome"));
        return event;
    }
}
