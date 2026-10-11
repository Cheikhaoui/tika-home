package com.tikahome.entity;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEmailTest {

    final Instant t0 = Instant.parse("2027-01-01T10:00:00Z");

    @Test
    void retryDelaysGrowThenGiveUp() {
        var email = new OutboxEmail(EmailType.BOOKING_REQUEST_OWNER, "o@example.com", null, "en", "s", "b", null, t0);

        email.markAttemptFailed(t0, "down", 5);
        assertThat(email.getNextAttemptAt()).isEqualTo(t0.plus(Duration.ofMinutes(1)));
        email.markAttemptFailed(t0, "down", 5);
        assertThat(email.getNextAttemptAt()).isEqualTo(t0.plus(Duration.ofMinutes(5)));
        email.markAttemptFailed(t0, "down", 5);
        assertThat(email.getNextAttemptAt()).isEqualTo(t0.plus(Duration.ofMinutes(30)));
        email.markAttemptFailed(t0, "down", 5);
        assertThat(email.getNextAttemptAt()).isEqualTo(t0.plus(Duration.ofHours(2)));
        assertThat(email.getStatus()).isEqualTo(EmailStatus.PENDING);

        email.markAttemptFailed(t0, "down", 5);
        assertThat(email.getStatus()).isEqualTo(EmailStatus.FAILED);
        assertThat(email.getAttempts()).isEqualTo(5);
    }
}
