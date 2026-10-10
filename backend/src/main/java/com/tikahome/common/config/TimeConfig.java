package com.tikahome.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    /** Injected instead of calling LocalDate.now() directly, so tests can freeze "today". */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
