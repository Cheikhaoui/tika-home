package com.tikahome.config;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionSafetyCheckTest {

    static AdminProperties admin(String jwtSecret, String bootstrapPassword, boolean cookieSecure) {
        return new AdminProperties("owner@example.com", bootstrapPassword, jwtSecret, Duration.ofHours(8),
                "tikahome_admin", cookieSecure, List.of("https://tikahome.example"), 5, Duration.ofMinutes(15));
    }

    static NotificationProperties notifications(String siteUrl) {
        return new NotificationProperties("owner@example.com", "en", "Tika Home <no-reply@tikahome.example>", siteUrl,
                true, Duration.ofSeconds(30), 5, 3);
    }

    static AssistantProperties assistant(String visitorHashSecret) {
        return new AssistantProperties("m", "t", "/tmp", 4, 0.35, "claude-opus-5-5", "low", 2048, 20, 30,
                new BigDecimal("5.00"), visitorHashSecret);
    }

    @Test
    void acceptsAProperProductionSetup() {
        assertThat(ProductionSafetyCheck.problems(
                admin("test-fixture-not-a-real-secret-0123456789", "test-fixture-password-123", true),
                notifications("https://tikahome.example"),
                assistant("test-fixture-not-a-real-visitor-key-0123456789"))).isEmpty();
    }

    @Test
    void refusesTheDevelopmentValuesThatArePublicInGit() {
        var problems = ProductionSafetyCheck.problems(
                admin("local-dev-only-secret-change-me-in-production-0123456789", "local-dev-password", false),
                notifications("http://tikahome.example"),
                assistant("local-dev-only-visitor-hash-secret-0123456789"));

        assertThat(problems).hasSize(5);
    }
}
