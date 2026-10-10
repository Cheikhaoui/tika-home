package com.tikahome.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

/** Settings under {@code tikahome.ical.*} in application.yml. */
@ConfigurationProperties("tikahome.ical")
public record IcalProperties(
        /* Run the scheduled import job. */
        @DefaultValue("true") boolean syncEnabled,
        @DefaultValue("PT15M") Duration syncInterval,
        @DefaultValue("PT30S") Duration initialDelay,
        @DefaultValue("PT10S") Duration connectTimeout,
        /* Whole-response timeout for downloading a feed. */
        @DefaultValue("PT30S") Duration readTimeout,
        @DefaultValue("2MB") DataSize maxFeedSize,
        /* Only for tests/local dev: real channel URLs are always https. */
        @DefaultValue("false") boolean allowHttp) {
}
