package com.tikahome.integration;

import com.tikahome.TestcontainersConfiguration;
import com.tikahome.dto.CreateBookingRequest;
import com.tikahome.dto.GuestRequest;
import com.tikahome.entity.Booking;
import com.tikahome.entity.BlockedDate;
import com.tikahome.entity.Channel;
import com.tikahome.entity.ChannelType;
import com.tikahome.entity.Property;
import com.tikahome.repository.BlockedDateRepository;
import com.tikahome.repository.BookingRepository;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AdminDashboardIT {

    @LocalServerPort int port;
    @Autowired BookingService bookingService;
    @Autowired PropertyRepository properties;
    @Autowired BookingRepository bookings;
    @Autowired BlockedDateRepository blockedDates;
    @Autowired ChannelRepository channels;
    @Autowired TransactionTemplate tx;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void unlock() {
        jdbc.update("update admin_user set failed_attempts = 0, locked_until = null");
    }

    @Test
    void summarisesWhatNeedsAttention() throws Exception {
        Property property = properties.save(new Property("dash-" + UUID.randomUUID(), "Dashboard IT house", 6,
                new BigDecimal("100.00"), BigDecimal.ZERO, "EUR", 1));
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Casablanca"));

        Booking pending = request(property, today.plusDays(30), today.plusDays(33), "pending@example.com");
        Booking arriving = request(property, today.plusDays(10), today.plusDays(12), "arriving@example.com");
        tx.executeWithoutResult(s -> bookings.findById(arriving.getId()).orElseThrow().confirm());

        Channel airbnb = channels.save(new Channel(property, ChannelType.AIRBNB, "Airbnb", "https://a.example/x.ics"));
        // Imported reservation overlapping the pending request: a double booking.
        blockedDates.save(BlockedDate.imported(airbnb, "late@airbnb.com", today.plusDays(31), today.plusDays(35),
                "Airbnb: Reserved"));
        tx.executeWithoutResult(s -> channels.findById(airbnb.getId()).orElseThrow()
                .recordFailure(Instant.now(), "HTTP 503"));

        var response = new AdminClient(port).get("/api/admin/properties/" + property.getId() + "/dashboard");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"pendingCount\":1", pending.getReference(), arriving.getReference(), "\"currentStay\":null")
                .contains("\"conflicts\":[{\"booking\":{", "\"channelName\":\"Airbnb\"")
                .contains("\"failingChannels\":[{", "HTTP 503");
    }

    private Booking request(Property property, LocalDate checkIn, LocalDate checkOut, String email) {
        return bookingService.request(new CreateBookingRequest(property.getId(), checkIn, checkOut, 2,
                new GuestRequest("Dashboard Guest", email, null, "en"), null));
    }
}
