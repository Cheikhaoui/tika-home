package com.tikahome.integration;

import com.tikahome.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** The hosting platform's health checks work without login; nothing else from Actuator is exposed. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class HealthIT {

    @LocalServerPort int port;
    final HttpClient http = HttpClient.newHttpClient();

    HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void livenessAndReadinessAreUp() throws Exception {
        assertThat(get("/actuator/health/liveness").body()).contains("\"status\":\"UP\"");
        assertThat(get("/actuator/health/readiness").body()).contains("\"status\":\"UP\"");
    }

    @Test
    void healthShowsNoDetailsAndOtherEndpointsAreHidden() throws Exception {
        assertThat(get("/actuator/health").body()).contains("\"status\":\"UP\"").doesNotContain("components", "details");
        assertThat(get("/actuator/env").statusCode()).isIn(403, 404);
        assertThat(get("/actuator/beans").statusCode()).isIn(403, 404);
    }
}
