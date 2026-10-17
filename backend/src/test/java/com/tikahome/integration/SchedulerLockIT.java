package com.tikahome.integration;

import com.tikahome.TestcontainersConfiguration;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Two "instances" asking for the same job lock: only the first gets it, until it is released. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SchedulerLockIT {

    @Autowired LockProvider lockProvider;

    @Test
    void onlyOneInstanceRunsAJobAtATime() {
        var config = new LockConfiguration(Instant.now(), "it-job-" + System.nanoTime(), Duration.ofMinutes(1), Duration.ZERO);

        var first = lockProvider.lock(config);
        var second = lockProvider.lock(config);

        assertThat(first).isPresent();
        assertThat(second).isEmpty();

        first.get().unlock();
        assertThat(lockProvider.lock(config)).isPresent();
    }
}
