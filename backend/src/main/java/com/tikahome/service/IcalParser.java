package com.tikahome.service;

import com.tikahome.exception.IcalParseException;
import com.tikahome.model.DateRange;
import com.tikahome.model.ExternalEvent;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.Status;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Turns an iCal feed (Airbnb, Booking.com, ...) into busy periods. Pure logic, no Spring, no I/O.
 * <ul>
 *   <li>All-day events (what Airbnb and Booking.com send) map directly to dates; DTEND is exclusive,
 *       exactly like our check-out date.</li>
 *   <li>Date-time events are converted to the property's time zone and only their dates are kept
 *       (arrive 15:00 on the 10th, leave 11:00 on the 14th = nights 10..13).</li>
 *   <li>Cancelled events are skipped; events without a usable start are skipped.</li>
 * </ul>
 */
public final class IcalParser {

    static final int MAX_SUMMARY_LENGTH = 200;

    private IcalParser() {
    }

    public static List<ExternalEvent> parse(String ics, ZoneId zone) {
        Calendar calendar;
        try {
            calendar = new CalendarBuilder().build(new StringReader(ics));
        } catch (IOException | ParserException | RuntimeException e) {
            throw new IcalParseException("Invalid iCal feed: " + e.getMessage(), e);
        }

        List<ExternalEvent> events = new ArrayList<>();
        for (VEvent event : calendar.<VEvent>getComponents(Component.VEVENT)) {
            toExternalEvent(event, zone).ifPresent(events::add);
        }
        return events;
    }

    private static Optional<ExternalEvent> toExternalEvent(VEvent event, ZoneId zone) {
        if (isCancelled(event)) {
            return Optional.empty();
        }
        Optional<LocalDate> start = dateOf(event, Property.DTSTART, zone);
        if (start.isEmpty()) {
            return Optional.empty();
        }
        LocalDate end = dateOf(event, Property.DTEND, zone)
                .filter(e -> e.isAfter(start.get()))
                .orElse(start.get().plusDays(1)); // no/invalid end: treat as a single night
        DateRange range = new DateRange(start.get(), end);

        String uid = text(event, Property.UID)
                .orElse("no-uid-" + range.start() + "-" + range.end()); // stable across syncs
        String summary = text(event, Property.SUMMARY)
                .map(s -> s.length() > MAX_SUMMARY_LENGTH ? s.substring(0, MAX_SUMMARY_LENGTH) : s)
                .orElse(null);
        return Optional.of(new ExternalEvent(uid, range, summary));
    }

    private static boolean isCancelled(VEvent event) {
        return text(event, Property.STATUS).map(Status.VALUE_CANCELLED::equalsIgnoreCase).orElse(false);
    }

    private static Optional<LocalDate> dateOf(VEvent event, String propertyName, ZoneId zone) {
        return event.<DateProperty<Temporal>>getProperty(propertyName)
                .map(DateProperty::getDate)
                .map(t -> toLocalDate(t, zone));
    }

    private static LocalDate toLocalDate(Temporal t, ZoneId zone) {
        return switch (t) {
            case LocalDate d -> d;
            case LocalDateTime dt -> dt.toLocalDate(); // "floating" time: already local
            case ZonedDateTime zdt -> zdt.withZoneSameInstant(zone).toLocalDate();
            case OffsetDateTime odt -> odt.atZoneSameInstant(zone).toLocalDate();
            case Instant i -> i.atZone(zone).toLocalDate();
            default -> LocalDate.from(t);
        };
    }

    private static Optional<String> text(VEvent event, String propertyName) {
        return event.<Property>getProperty(propertyName)
                .map(Property::getValue)
                .map(String::strip)
                .filter(v -> !v.isEmpty());
    }
}
