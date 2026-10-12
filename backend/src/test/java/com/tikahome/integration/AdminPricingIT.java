package com.tikahome.integration;

import com.tikahome.TestcontainersConfiguration;
import com.tikahome.entity.Property;
import com.tikahome.model.DateRange;
import com.tikahome.repository.PropertyRepository;
import com.tikahome.service.QuoteService;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AdminPricingIT {

    @LocalServerPort int port;
    @Autowired PropertyRepository properties;
    @Autowired QuoteService quoteService;
    @Autowired JdbcTemplate jdbc;

    AdminClient admin;
    Property property;
    final LocalDate start = LocalDate.now().plusDays(60);

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("update admin_user set failed_attempts = 0, locked_until = null");
        admin = new AdminClient(port);
        property = properties.save(new Property("price-" + UUID.randomUUID(), "Pricing IT house", 6,
                new BigDecimal("100.00"), new BigDecimal("40.00"), "EUR", 2));
    }

    String base() {
        return "/api/admin/properties/" + property.getId();
    }

    @Test
    void newBasePriceIsUsedByQuotes() throws Exception {
        var response = admin.put(base() + "/settings",
                "{\"baseNightlyPrice\":150.00,\"cleaningFee\":50.00,\"minNights\":3,\"maxGuests\":4}");

        assertThat(response.statusCode()).isEqualTo(200);
        var quote = quoteService.quote(property.getId(), new DateRange(start, start.plusDays(3)), 2);
        assertThat(quote.total()).isEqualByComparingTo("500.00"); // 3 x 150 + 50
        assertThat(quote.pricing().minNights()).isEqualTo(3);
    }

    @Test
    void seasonalRulesCanBeCreatedChangedAndDeleted() throws Exception {
        var created = admin.post(base() + "/price-rules", rule("Summer", start, start.plusDays(30), "200.00", 4, 10));
        assertThat(created.statusCode()).isEqualTo(201);
        String id = id(created.body());
        assertThat(nightPrice()).isEqualByComparingTo("200.00");

        assertThat(admin.put("/api/admin/price-rules/" + id, rule("Summer", start, start.plusDays(30), "180.00", 4, 10))
                .statusCode()).isEqualTo(200);
        assertThat(nightPrice()).isEqualByComparingTo("180.00");
        assertThat(admin.get(base() + "/pricing").body()).contains("\"name\":\"Summer\"", "\"nightlyPrice\":180.00");

        assertThat(admin.delete("/api/admin/price-rules/" + id).statusCode()).isEqualTo(204);
        assertThat(nightPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void invalidValuesAreRejected() throws Exception {
        assertThat(admin.put(base() + "/settings",
                "{\"baseNightlyPrice\":-5,\"cleaningFee\":0,\"minNights\":0,\"maxGuests\":4}").statusCode()).isEqualTo(400);
        assertThat(admin.post(base() + "/price-rules", rule("Bad", start, start, "100.00", null, 0)).statusCode())
                .isEqualTo(400); // end must be after start
        assertThat(admin.post(base() + "/price-rules", rule("", start, start.plusDays(2), "100.00", null, 0))
                .statusCode()).isEqualTo(400);
    }

    private BigDecimal nightPrice() {
        return quoteService.quote(property.getId(), new DateRange(start, start.plusDays(5)), 2)
                .pricing().nights().getFirst().price();
    }

    private static String rule(String name, LocalDate from, LocalDate to, String price, Integer minNights, int priority) {
        return "{\"name\":\"%s\",\"startDate\":\"%s\",\"endDate\":\"%s\",\"nightlyPrice\":%s,\"minNights\":%s,\"priority\":%d}"
                .formatted(name, from, to, price, minNights, priority);
    }

    private static String id(String json) {
        Matcher m = Pattern.compile("\"id\":\"([0-9a-f-]{36})\"").matcher(json);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }
}
