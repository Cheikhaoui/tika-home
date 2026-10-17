package com.tikahome.config;

import org.junit.jupiter.api.Test;

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

    @Test
    void acceptsAProperProductionSetup() {
        assertThat(ProductionSafetyCheck.problems(
                admin("kM2v9QxR7tLp4WzB8nYc3HsF6jDg1AeU5oIrTqVb", "a-long-real-password!", true),
                notifications("https://tikahome.example"))).isEmpty();
    }

    @Test
    void refusesTheDevelopmentValuesThatArePublicInGit() {
        var problems = ProductionSafetyCheck.problems(
                admin("local-dev-only-secret-change-me-in-production-0123456789", "local-dev-password", false),
                notifications("http://tikahome.example"));

        assertThat(problems).hasSize(4);
    }
}
