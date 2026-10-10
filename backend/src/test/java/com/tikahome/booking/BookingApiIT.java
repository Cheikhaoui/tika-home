package com.tikahome.booking;

import com.tikahome.TestcontainersConfiguration;
import com.tikahome.availability.BlockedDate;
import com.tikahome.availability.BlockedDateRepository;
import com.tikahome.property.Property;
import com.tikahome.property.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end over real HTTP against a real Postgres (Testcontainers).
 * Each test gets its own property, so tests don't interfere with each other or the seed data.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class BookingApiIT {

    @LocalServerPort
    int port;

    @Autowired
    PropertyRepository properties;

    @Autowired
    BookingRepository bookings;

    @Autowired
    BlockedDateRepository blockedDates;

    @Autowired
    TransactionTemplate tx;

    final HttpClient http = HttpClient.newHttpClient();
    Property property;

    @BeforeEach
    void createProperty() {
        property = properties.save(new Property("it-" + UUID.randomUUID(), "IT house", 6,
                new BigDecimal("100.00"), new BigDecimal("40.00"), "EUR", 2));
    }

    @Test
    void createsRequestedBookingWithServerSidePrice() throws Exception {
        var response = book("2030-03-10", "2030-03-13", "first@example.com");

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body())
                .contains("\"status\":\"REQUESTED\"")
                .contains("\"totalPrice\":340.00"); // 3 x 100 + 40 cleaning
    }

    @Test
    void rejectsOverlappingBookingWith409() throws Exception {
        assertThat(book("2030-03-10", "2030-03-15", "first@example.com").statusCode()).isEqualTo(201);

        var overlapping = book("2030-03-14", "2030-03-17", "second@example.com");

        assertThat(overlapping.statusCode()).isEqualTo(409);
        assertThat(overlapping.body()).contains("DATES_UNAVAILABLE");
    }

    @Test
    void allowsBackToBackBookings() throws Exception {
        assertThat(book("2030-04-10", "2030-04-15", "first@example.com").statusCode()).isEqualTo(201);

        assertThat(book("2030-04-15", "2030-04-18", "second@example.com").statusCode()).isEqualTo(201);
    }

    @Test
    void declinedBookingReleasesDates() throws Exception {
        book("2030-05-10", "2030-05-15", "first@example.com");
        tx.executeWithoutResult(s -> bookings
                .findOverlapping(property.getId(), LocalDate.parse("2030-05-10"), LocalDate.parse("2030-05-15"),
                        BookingStatus.ACTIVE)
                .forEach(Booking::decline));

        assertThat(book("2030-05-10", "2030-05-15", "second@example.com").statusCode()).isEqualTo(201);
    }

    @Test
    void rejectsBookingOverBlockedDates() throws Exception {
        blockedDates.save(BlockedDate.manual(property, LocalDate.parse("2030-06-12"), LocalDate.parse("2030-06-13"),
                "Family visit"));

        var response = book("2030-06-10", "2030-06-14", "first@example.com");

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(availability("2030-06-10", "2030-06-14").body())
                .contains("{\"start\":\"2030-06-12\",\"end\":\"2030-06-13\"}");
    }

    @Test
    void rejectsBrokenRulesWith422() throws Exception {
        var oneNight = book("2030-07-10", "2030-07-11", "first@example.com");

        assertThat(oneNight.statusCode()).isEqualTo(422);
        assertThat(oneNight.body()).contains("BELOW_MIN_NIGHTS");
    }

    /**
     * The key test: many guests hit "Book" for the same dates at the same moment.
     * Exactly one must win; all others get 409. Nothing else (no 500s, no second booking).
     */
    @Test
    void concurrentRequestsForSameDatesProduceExactlyOneBooking() throws Exception {
        int threads = 10;
        var ready = new CountDownLatch(threads);
        var go = new CountDownLatch(1);
        List<Callable<Integer>> attempts = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            String email = "guest" + i + "@example.com";
            attempts.add(() -> {
                ready.countDown();
                go.await(); // all threads fire together
                return book("2030-08-10", "2030-08-14", email).statusCode();
            });
        }

        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<Integer>> futures = attempts.stream().map(pool::submit).toList();
            ready.await();
            go.countDown();
            for (Future<Integer> f : futures) {
                statuses.add(f.get());
            }
        }

        assertThat(statuses).containsOnly(201, 409);
        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(bookings.findOverlapping(property.getId(), LocalDate.parse("2030-08-10"),
                LocalDate.parse("2030-08-14"), BookingStatus.ACTIVE)).hasSize(1);
    }

    private HttpResponse<String> book(String checkIn, String checkOut, String email) throws Exception {
        String json = """
                {"propertyId":"%s","checkIn":"%s","checkOut":"%s","guests":2,
                 "guest":{"fullName":"Test Guest","email":"%s","language":"en"}}
                """.formatted(property.getId(), checkIn, checkOut, email);
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/bookings"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> availability(String from, String to) throws Exception {
        var uri = URI.create("http://localhost:%d/api/properties/%s/availability?from=%s&to=%s"
                .formatted(port, property.getId(), from, to));
        return http.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString());
    }
}
