package com.tikahome.service;

import com.tikahome.config.IcalProperties;
import com.tikahome.exception.FeedFetchException;
import com.tikahome.support.FakeFeedServer;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IcalFeedClientTest {

    static IcalProperties props(boolean allowHttp, DataSize maxSize) {
        return new IcalProperties(false, Duration.ofMinutes(15), Duration.ofSeconds(30), Duration.ofSeconds(2),
                Duration.ofSeconds(5), maxSize, allowHttp);
    }

    @Test
    void downloadsFeed() throws Exception {
        try (var server = new FakeFeedServer()) {
            server.serve(FakeFeedServer.calendar());
            String body = new IcalFeedClient(props(true, DataSize.ofMegabytes(2))).fetch(server.url());
            assertThat(body).startsWith("BEGIN:VCALENDAR");
        }
    }

    @Test
    void rejectsPlainHttpUnlessAllowed() {
        var client = new IcalFeedClient(props(false, DataSize.ofMegabytes(2)));

        assertThatThrownBy(() -> client.fetch("http://example.com/cal.ics")).hasMessage("Feed URL must use https");
        assertThatThrownBy(() -> client.fetch("file:///etc/passwd")).hasMessage("Feed URL must use https");
    }

    @Test
    void rejectsOversizedFeed() throws Exception {
        try (var server = new FakeFeedServer()) {
            server.serve("X".repeat(2_000));
            var client = new IcalFeedClient(props(true, DataSize.ofBytes(1_000)));

            assertThatThrownBy(() -> client.fetch(server.url()))
                    .isInstanceOf(FeedFetchException.class)
                    .hasMessageStartingWith("Feed larger than");
        }
    }

    @Test
    void errorMessagesNeverContainTheSecretUrl() throws Exception {
        try (var server = new FakeFeedServer()) {
            server.respond(404, "nope");
            var client = new IcalFeedClient(props(true, DataSize.ofMegabytes(2)));

            assertThatThrownBy(() -> client.fetch(server.url()))
                    .hasMessage("HTTP 404")
                    .message().doesNotContain("secret-token");
        }
    }
}
