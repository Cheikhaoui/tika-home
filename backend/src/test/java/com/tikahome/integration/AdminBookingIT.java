package com.tikahome.integration;

import com.tikahome.TestcontainersConfiguration;
import com.tikahome.dto.CreateBookingRequest;
import com.tikahome.dto.GuestRequest;
import com.tikahome.entity.Booking;
import com.tikahome.entity.BookingStatus;
import com.tikahome.entity.Channel;
import com.tikahome.entity.ChannelType;
import com.tikahome.entity.Property;
import com.tikahome.repository.BookingRepository;
import com.tikahome.repository.ChannelRepository;
import com.tikahome.repository.PropertyRepository;
import com.tikahome.service.BookingService;
import com.tikahome.support.AdminClient;
import com.tikahome.support.FakeFeedServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.UUID;

import static com.tikahome.support.FakeFeedServer.calendar;
import static com.tikahome.support.FakeFeedServer.event;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AdminBookingIT {

    @LocalServerPort int port;
    @Autowired BookingService bookingService;
    @Autowired PropertyRepository properties;
    @Autowired BookingRepository bookings;
    @Autowired ChannelRepository channels;
    @Autowired JdbcTemplate jdbc;

    AdminClient admin;
    Property property;
    final LocalDate checkIn = LocalDate.now().plusDays(40);

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("update admin_user set failed_attempts = 0, locked_until = null");
        jdbc.update("delete from email_outbox");
        admin = new AdminClient(port);
        property = properties.save(new Property("admin-" + UUID.randomUUID(), "Admin IT house", 6,
                new BigDecimal("100.00"), BigDecimal.ZERO, "EUR", 1));
    }

    @Test
    void requiresLogin() throws Exception {
        var anonymous = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/admin/bookings")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(anonymous.statusCode()).isEqualTo(401);
    }

    @Test
    void pendingRequestsAreListedWithTheirGuest() throws Exception {
        Booking booking = request("sara@example.com", "fr");

        var list = admin.get("/api/admin/bookings?view=PENDING&size=100");

        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.body()).contains(booking.getReference(), "Sara Benali", "sara@example.com", "\"nights\":3");
    }

    @Test
    void detailShowsGuestContactAndMessage() throws Exception {
        Booking booking = request("sara@example.com", "fr");

        var detail = admin.get("/api/admin/bookings/" + booking.getId());

        assertThat(detail.body()).contains("\"phone\":\"+212600000000\"", "\"language\":\"fr\"", "Arrivée tardive",
                "\"conflicts\":[]");
    }

    @Test
    void confirmEmailsTheGuestInTheirLanguage() throws Exception {
        Booking booking = request("sara@example.com", "fr");

        var response = admin.post("/api/admin/bookings/" + booking.getId() + "/confirm", "");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"CONFIRMED\"", "\"warnings\":[]");
        assertThat(bookings.findById(booking.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(jdbc.queryForObject(
                "select subject from email_outbox where type = 'BOOKING_CONFIRMED_GUEST' and recipient = ?",
                String.class, "sara@example.com")).isEqualTo("Votre séjour est confirmé " + booking.getReference());
        assertThat(admin.get("/api/admin/bookings?view=UPCOMING&size=100").body()).contains(booking.getReference());
    }

    @Test
    void cannotConfirmTwice() throws Exception {
        Booking booking = request("sara@example.com", "en");
        admin.post("/api/admin/bookings/" + booking.getId() + "/confirm", "");

        var again = admin.post("/api/admin/bookings/" + booking.getId() + "/confirm", "");

        assertThat(again.statusCode()).isEqualTo(409);
        assertThat(again.body()).contains("is CONFIRMED");
    }

    @Test
    void declineReleasesTheDatesAndSendsTheOwnersMessage() throws Exception {
        Booking booking = request("sara@example.com", "en");

        var response = admin.post("/api/admin/bookings/" + booking.getId() + "/decline",
                "{\"message\":\"Sorry, we have family staying that week. The week after is free!\"}");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"DECLINED\"");
        String html = jdbc.queryForObject("select html_body from email_outbox where type = 'BOOKING_DECLINED_GUEST'",
                String.class);
        assertThat(html).contains("Sorry, we have family staying that week.");
        // The same dates can be requested again.
        assertThat(request("other@example.com", "en").getStatus()).isEqualTo(BookingStatus.REQUESTED);
    }

    @Test
    void confirmResyncsChannelsAndRefusesWhenAirbnbTookTheDatesMeanwhile() throws Exception {
        Booking booking = request("sara@example.com", "en");
        try (var airbnb = new FakeFeedServer()) {
            channels.save(new Channel(property, ChannelType.AIRBNB, "Airbnb", airbnb.url()));
            // Reserved on Airbnb after our request arrived, before the owner answered.
            airbnb.serve(calendar(event("late@airbnb.com", checkIn.plusDays(1), checkIn.plusDays(5), "Reserved")));

            var response = admin.post("/api/admin/bookings/" + booking.getId() + "/confirm", "");

            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(response.body()).contains("\"conflicts\"", "\"channelName\":\"Airbnb\"", "\"source\":\"ICAL\"");
            assertThat(bookings.findById(booking.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.REQUESTED);
        }
    }

    @Test
    void channelOutageDoesNotBlockConfirmationButIsReported() throws Exception {
        Booking booking = request("sara@example.com", "en");
        try (var airbnb = new FakeFeedServer()) {
            channels.save(new Channel(property, ChannelType.AIRBNB, "Airbnb", airbnb.url()));
            airbnb.respond(503, "down");

            var response = admin.post("/api/admin/bookings/" + booking.getId() + "/confirm", "");

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"status\":\"CONFIRMED\"", "Airbnb: HTTP 503");
        }
    }

    private Booking request(String email, String language) {
        return bookingService.request(new CreateBookingRequest(property.getId(), checkIn, checkIn.plusDays(3), 2,
                new GuestRequest("Sara Benali", email, "+212600000000", language), "Arrivée tardive"));
    }
}
