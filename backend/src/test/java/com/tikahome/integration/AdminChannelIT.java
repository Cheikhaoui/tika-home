package com.tikahome.integration;

import com.tikahome.TestcontainersConfiguration;
import com.tikahome.entity.Property;
import com.tikahome.repository.BlockedDateRepository;
import com.tikahome.repository.PropertyRepository;
import com.tikahome.support.AdminClient;
import com.tikahome.support.FakeFeedServer;
import org.junit.jupiter.api.AfterEach;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tikahome.support.FakeFeedServer.calendar;
import static com.tikahome.support.FakeFeedServer.event;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AdminChannelIT {

    @LocalServerPort int port;
    @Autowired PropertyRepository properties;
    @Autowired BlockedDateRepository blockedDates;
    @Autowired JdbcTemplate jdbc;

    AdminClient admin;
    Property property;
    FakeFeedServer airbnb;
    final LocalDate start = LocalDate.now().plusDays(20);

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("update admin_user set failed_attempts = 0, locked_until = null");
        admin = new AdminClient(port);
        airbnb = new FakeFeedServer();
        property = properties.save(new Property("chan-" + UUID.randomUUID(), "Channel IT house", 6,
                new BigDecimal("100.00"), BigDecimal.ZERO, "EUR", 1));
    }

    @AfterEach
    void tearDown() {
        airbnb.close();
    }

    @Test
    void addingAChannelImportsItAtOnceAndNeverExposesTheSecretUrl() throws Exception {
        airbnb.serve(calendar(event("a@airbnb.com", start, start.plusDays(3), "Reserved")));

        var created = admin.post("/api/admin/properties/" + property.getId() + "/channels",
                "{\"type\":\"AIRBNB\",\"name\":\"Airbnb\",\"importUrl\":\"%s\"}".formatted(airbnb.url()));

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("\"created\":1", "\"lastSyncStatus\":\"SUCCESS\"", "/…oken")
                .doesNotContain("secret-token");
        var list = admin.get("/api/admin/properties/" + property.getId() + "/channels");
        assertThat(list.body()).contains("\"exportUrl\":\"http://localhost:3000/ical/", ".ics\"")
                .doesNotContain("secret-token");
    }

    @Test
    void syncNowReportsFailuresAndDisablingStopsTheJob() throws Exception {
        airbnb.serve(calendar());
        String id = id(admin.post("/api/admin/properties/" + property.getId() + "/channels",
                "{\"type\":\"AIRBNB\",\"name\":\"Airbnb\",\"importUrl\":\"%s\"}".formatted(airbnb.url())).body());

        airbnb.respond(500, "oops");
        var sync = admin.post("/api/admin/channels/" + id + "/sync", "");
        assertThat(sync.body()).contains("\"status\":\"FAILED\"", "HTTP 500", "\"consecutiveFailures\":1");

        var updated = admin.put("/api/admin/channels/" + id,
                "{\"type\":\"AIRBNB\",\"name\":\"Airbnb (paused)\",\"importUrl\":\"\",\"enabled\":false}");
        assertThat(updated.body()).contains("\"enabled\":false", "Airbnb (paused)", "/…oken"); // URL kept
    }

    @Test
    void deletingAChannelFreesItsImportedDates() throws Exception {
        airbnb.serve(calendar(event("a@airbnb.com", start, start.plusDays(3), "Reserved")));
        String id = id(admin.post("/api/admin/properties/" + property.getId() + "/channels",
                "{\"type\":\"AIRBNB\",\"name\":\"Airbnb\",\"importUrl\":\"%s\"}".formatted(airbnb.url())).body());
        assertThat(blockedDates.findByChannelId(UUID.fromString(id))).hasSize(1);

        assertThat(admin.delete("/api/admin/channels/" + id).statusCode()).isEqualTo(204);
        assertThat(blockedDates.findByChannelId(UUID.fromString(id))).isEmpty();
    }

    @Test
    void urlIsValidated() throws Exception {
        var response = admin.post("/api/admin/properties/" + property.getId() + "/channels",
                "{\"type\":\"AIRBNB\",\"name\":\"Airbnb\",\"importUrl\":\"ftp://nope\"}");
        assertThat(response.statusCode()).isEqualTo(400);
    }

    private static String id(String json) {
        Matcher m = Pattern.compile("\"channel\":\\{\"id\":\"([0-9a-f-]{36})\"").matcher(json);
        assertThat(m.find()).as(json).isTrue();
        return m.group(1);
    }
}
