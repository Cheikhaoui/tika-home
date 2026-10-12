package com.tikahome.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/** Settings under {@code tikahome.admin.*}. Secrets come from environment variables in production. */
@ConfigurationProperties("tikahome.admin")
public record AdminProperties(
        /* First admin account, created at startup if no admin exists (TIKAHOME_ADMIN_EMAIL / _PASSWORD). */
        String bootstrapEmail,
        String bootstrapPassword,
        /* HMAC key for signing JWTs: at least 32 characters (256 bits). TIKAHOME_ADMIN_JWT_SECRET. */
        String jwtSecret,
        @DefaultValue("PT8H") Duration tokenTtl,
        @DefaultValue("tikahome_admin") String cookieName,
        /* true in production (HTTPS only). */
        @DefaultValue("true") boolean cookieSecure,
        /* Origins allowed to send admin write requests (CSRF defence in depth). */
        @DefaultValue("http://localhost:3000") List<String> allowedOrigins,
        @DefaultValue("5") int maxFailedLogins,
        @DefaultValue("PT15M") Duration lockDuration) {
}
