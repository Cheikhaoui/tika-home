package com.tikahome.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on @Scheduled methods. Note for deployment (phase 6): with several app instances each one would
 * run the jobs, so we'll add ShedLock (a DB-based lock) to make sure only one instance syncs at a time.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
