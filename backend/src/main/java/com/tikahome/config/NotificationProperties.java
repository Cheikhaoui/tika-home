package com.tikahome.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** Settings under {@code tikahome.notifications.*} in application.yml. */
@ConfigurationProperties("tikahome.notifications")
public record NotificationProperties(
        /* Who receives booking requests and alerts. */
        String ownerEmail,
        @DefaultValue("en") String ownerLanguage,
        String fromAddress,
        /* Public website URL, used for links in emails. */
        String siteUrl,
        /* Run the outbox sender job (disabled in tests that drive it manually). */
        @DefaultValue("true") boolean senderEnabled,
        @DefaultValue("PT30S") Duration sendInterval,
        @DefaultValue("5") int maxAttempts,
        /* Consecutive failed syncs before the owner is alerted. */
        @DefaultValue("3") int syncFailureAlertThreshold) {
}
