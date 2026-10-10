package com.tikahome.integration;

import com.tikahome.TestcontainersConfiguration;
import com.tikahome.entity.BlockedDate;
import com.tikahome.entity.Booking;
import com.tikahome.entity.Channel;
import com.tikahome.entity.ChannelType;
import com.tikahome.entity.Guest;
import com.tikahome.entity.Property;
import com.tikahome.model.DateRange;
import com.tikahome.model.ExternalEvent;
import com.tikahome.repository.BlockedDateRepository;
import com.tikahome.repository.BookingRepository;
import com.tikahome.repository.ChannelRepository;
import com.tikahome.repository.GuestRepository;
import com.tikahome.repository.PropertyRepository;
import com.tikahome.service.IcalParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class IcalExportIT {

    static final ZoneId MARRAKECH = ZoneId.of("Africa/Casablanca");

    @LocalServerPort int port;
    @Autowired PropertyRepository properties;
    @Autowired GuestRepository guests;
    @Autowired BookingRepository bookings;
    @Autowired BlockedDateRepository blockedDates;
    @Autowired ChannelRepository channels;

    final HttpClient http = HttpClient.newHttpClient();
    Property property;
    LocalDate today;

    @BeforeEach
    void setUp() {
        property = properties.save(new Property("export-" + UUID.randomUUID(), "Export house", 6,
                new BigDecimal("100.00"), BigDecimal.ZERO, "EUR", 1));
        today = LocalDate.now(MARRAKECH);
    }

    @Test
    void exportsActiveBookingsAndManualBlocksButNotImportedOnesOrPersonalData() throws Exception {
        Guest guest = guests.save(new Guest("Sara Secret", "sara@example.com", "+212600000000", "fr"));
        Booking requested = bookings.save(booking(guest, 10, 14));
        Booking declined = booking(guest, 20, 22);
        declined.decline();
        bookings.save(declined);
        bookings.save(booking(guest, -10, -5)); // already over
        BlockedDate manual = blockedDates.save(BlockedDate.manual(property, today.plusDays(30), today.plusDays(33), "Family"));
        Channel airbnb = channels.save(new Channel(property, ChannelType.AIRBNB, "Airbnb", "https://a.example/x.ics"));
        blockedDates.save(BlockedDate.imported(airbnb, "x@airbnb.com", today.plusDays(40), today.plusDays(45), "Airbnb: Reserved"));

        HttpResponse<String> response = get("/ical/" + property.getIcalExportToken() + ".ics");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(v -> assertThat(v).startsWith("text/calendar"));
        String ics = response.body();

        // Round trip through our own parser: exactly what Airbnb / Booking.com will read.
        List<ExternalEvent> events = IcalParser.parse(ics, MARRAKECH);
        assertThat(events).containsExactlyInAnyOrder(
                new ExternalEvent("booking-" + requested.getId() + "@tikahome",
                        new DateRange(today.plusDays(10), today.plusDays(14)), "Reserved"),
                new ExternalEvent("block-" + manual.getId() + "@tikahome",
                        new DateRange(today.plusDays(30), today.plusDays(33)), "Not available"));
        assertThat(ics).contains("VERSION:2.0", "DTSTART;VALUE=DATE:", "DTSTAMP:")
                .doesNotContain("Sara", "sara@example.com", "+212", "Family", "airbnb");
    }

    @Test
    void unknownOrMalformedTokenIs404() throws Exception {
        assertThat(get("/ical/" + "0".repeat(64) + ".ics").statusCode()).isEqualTo(404);
        assertThat(get("/ical/not-a-token.ics").statusCode()).isEqualTo(404);
    }

    private Booking booking(Guest guest, int fromDay, int toDay) {
        return Booking.request(property, guest, today.plusDays(fromDay), today.plusDays(toDay), 2,
                new BigDecimal("400.00"), null);
    }

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
