package com.tikahome.service;

import com.tikahome.exception.IcalParseException;
import com.tikahome.model.DateRange;
import com.tikahome.model.ExternalEvent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IcalParserTest {

    static final ZoneId MARRAKECH = ZoneId.of("Africa/Casablanca");

    static DateRange range(String start, String end) {
        return new DateRange(LocalDate.parse(start), LocalDate.parse(end));
    }

    static String fixture(String name) throws IOException {
        try (InputStream in = IcalParserTest.class.getResourceAsStream("/ical/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static String calendar(String... eventLines) {
        return "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//test//EN\r\n"
                + String.join("\r\n", eventLines) + "\r\nEND:VCALENDAR\r\n";
    }

    @Test
    void parsesAirbnbFeed() throws IOException {
        List<ExternalEvent> events = IcalParser.parse(fixture("airbnb.ics"), MARRAKECH);

        assertThat(events).containsExactly(
                new ExternalEvent("1418fb94e984-f2a1c3d5e6b7a8c9d0e1f2a3b4c5d6e7@airbnb.com",
                        range("2027-03-10", "2027-03-15"), "Reserved"),
                new ExternalEvent("7f3a2b1c9d8e-0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d@airbnb.com",
                        range("2027-04-01", "2027-04-05"), "Airbnb (Not available)"));
    }

    @Test
    void parsesBookingComFeed() throws IOException {
        List<ExternalEvent> events = IcalParser.parse(fixture("booking-com.ics"), MARRAKECH);

        assertThat(events).containsExactly(new ExternalEvent("9a8b7c6d5e4f3a2b1c0d@booking.com",
                range("2027-03-20", "2027-03-24"), "CLOSED - Not available"));
    }

    @Test
    void convertsDateTimesToPropertyDates() {
        // 23:30 UTC on 9 June is already 10 June in Marrakech (UTC+1); check-out 11:00 on the 14th.
        // Morocco moves to UTC+0 during Ramadan (e.g. early March 2027), so never hard-code "+1".
        String ics = calendar("BEGIN:VEVENT", "UID:a", "DTSTART:20270609T233000Z",
                "DTEND;TZID=Africa/Casablanca:20270614T110000", "END:VEVENT");

        assertThat(IcalParser.parse(ics, MARRAKECH))
                .extracting(ExternalEvent::range)
                .containsExactly(range("2027-06-10", "2027-06-14"));
    }

    @Test
    void missingOrInvalidEndMeansOneNight() {
        String ics = calendar(
                "BEGIN:VEVENT", "UID:no-end", "DTSTART;VALUE=DATE:20270601", "END:VEVENT",
                "BEGIN:VEVENT", "UID:end-before-start", "DTSTART;VALUE=DATE:20270610",
                "DTEND;VALUE=DATE:20270610", "END:VEVENT");

        assertThat(IcalParser.parse(ics, MARRAKECH))
                .extracting(ExternalEvent::range)
                .containsExactly(range("2027-06-01", "2027-06-02"), range("2027-06-10", "2027-06-11"));
    }

    @Test
    void skipsCancelledEventsAndEventsWithoutStart() {
        String ics = calendar(
                "BEGIN:VEVENT", "UID:cancelled", "STATUS:CANCELLED", "DTSTART;VALUE=DATE:20270701",
                "DTEND;VALUE=DATE:20270703", "END:VEVENT",
                "BEGIN:VEVENT", "UID:no-start", "SUMMARY:broken", "END:VEVENT",
                "BEGIN:VEVENT", "UID:ok", "DTSTART;VALUE=DATE:20270801", "DTEND;VALUE=DATE:20270803", "END:VEVENT");

        assertThat(IcalParser.parse(ics, MARRAKECH)).extracting(ExternalEvent::uid).containsExactly("ok");
    }

    @Test
    void derivesStableUidWhenMissing() {
        String ics = calendar("BEGIN:VEVENT", "DTSTART;VALUE=DATE:20270901", "DTEND;VALUE=DATE:20270904", "END:VEVENT");

        assertThat(IcalParser.parse(ics, MARRAKECH))
                .extracting(ExternalEvent::uid)
                .containsExactly("no-uid-2027-09-01-2027-09-04");
    }

    @Test
    void emptyCalendarHasNoEvents() {
        assertThat(IcalParser.parse(calendar(), MARRAKECH)).isEmpty();
    }

    @Test
    void rejectsNonIcalContent() {
        assertThatThrownBy(() -> IcalParser.parse("<html>Not found</html>", MARRAKECH))
                .isInstanceOf(IcalParseException.class);
    }
}
