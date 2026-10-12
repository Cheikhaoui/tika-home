package com.tikahome.integration;

import com.tikahome.TestcontainersConfiguration;
import com.tikahome.dto.CreateBookingRequest;
import com.tikahome.dto.GuestRequest;
import com.tikahome.entity.BlockedDate;
import com.tikahome.entity.Channel;
import com.tikahome.entity.ChannelType;
import com.tikahome.entity.Property;
import com.tikahome.exception.BookingRejectedException;
import com.tikahome.repository.BlockedDateRepository;
import com.tikahome.repository.ChannelRepository;
import com.tikahome.repository.PropertyRepository;
import com.tikahome.service.BookingService;
import com.tikahome.support.AdminClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AdminCalendarIT {

    @LocalServerPort int port;
    @Autowired BookingService bookingService;
    @Autowired PropertyRepository properties;
    @Autowired BlockedDateRepository blockedDates;
    @Autowired ChannelRepository channels;
    @Autowired JdbcTemplate jdbc;

    AdminClient admin;
    Property property;
    final LocalDate start = LocalDate.now().plusDays(50);

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("update admin_user set failed_attempts = 0, locked_until = null");
        admin = new AdminClient(port);
        property = properties.save(new Property("cal-" + UUID.randomUUID(), "Calendar IT house", 6,
                new BigDecimal("100.00"), BigDecimal.ZERO, "EUR", 1));
    }

    String base() {
        return "/api/admin/properties/" + property.getId();
    }

    @Test
    void ownerBlockMakesTheDatesUnbookable() throws Exception {
        var created = admin.post(base() + "/blocked-dates",
                "{\"startDate\":\"%s\",\"endDate\":\"%s\",\"reason\":\"Family visit\"}".formatted(start, start.plusDays(4)));

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("\"source\":\"MANUAL\"", "Family visit");
        assertThatThrownBy(() -> bookingService.request(booking(start.plusDays(1), start.plusDays(3))))
                .isInstanceOf(BookingRejectedException.class);
    }

    @Test
    void cannotBlockDatesHeldByABooking() throws Exception {
        var held = bookingService.request(booking(start, start.plusDays(3)));

        var response = admin.post(base() + "/blocked-dates",
                "{\"startDate\":\"%s\",\"endDate\":\"%s\"}".formatted(start.plusDays(2), start.plusDays(6)));

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains(held.getReference());
    }

    @Test
    void invalidRangeIs400() throws Exception {
        var response = admin.post(base() + "/blocked-dates",
                "{\"startDate\":\"%s\",\"endDate\":\"%s\"}".formatted(start, start));
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void calendarShowsBookingsAndBlocksOfThePeriod() throws Exception {
        var held = bookingService.request(booking(start, start.plusDays(2)));
        blockedDates.save(BlockedDate.manual(property, start.plusDays(5), start.plusDays(7), "Painting"));
        Channel airbnb = channels.save(new Channel(property, ChannelType.AIRBNB, "Airbnb", "https://a.example/x.ics"));
        blockedDates.save(BlockedDate.imported(airbnb, "x@airbnb.com", start.plusDays(10), start.plusDays(12),
                "Airbnb: Reserved"));

        var response = admin.get(base() + "/calendar?from=%s&to=%s".formatted(start, start.plusDays(30)));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(held.getReference(), "Painting", "\"channelName\":\"Airbnb\"");
    }

    @Test
    void ownerCanRemoveOwnBlocksButNotImportedOnes() throws Exception {
        BlockedDate manual = blockedDates.save(BlockedDate.manual(property, start, start.plusDays(2), null));
        Channel airbnb = channels.save(new Channel(property, ChannelType.AIRBNB, "Airbnb", "https://a.example/x.ics"));
        BlockedDate imported = blockedDates.save(BlockedDate.imported(airbnb, "y@airbnb.com", start.plusDays(5),
                start.plusDays(7), "Airbnb: Reserved"));

        assertThat(admin.delete("/api/admin/blocked-dates/" + manual.getId()).statusCode()).isEqualTo(204);
        assertThat(admin.delete("/api/admin/blocked-dates/" + imported.getId()).statusCode()).isEqualTo(400);
        assertThat(blockedDates.findById(imported.getId())).isPresent();
    }

    private CreateBookingRequest booking(LocalDate checkIn, LocalDate checkOut) {
        return new CreateBookingRequest(property.getId(), checkIn, checkOut, 2,
                new GuestRequest("Calendar Guest", "cal@example.com", null, "en"), null);
    }
}
