package com.tikahome.integration;

import com.tikahome.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Login, cookie flags, lockout, protected endpoints and the Origin check, over real HTTP. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AdminAuthIT {

    static final String EMAIL = "admin@tikahome.test"; // created at startup from test config
    static final String PASSWORD = "correct-horse-battery-staple";

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;

    final HttpClient http = HttpClient.newHttpClient(); // no cookie jar: we pass cookies explicitly

    @BeforeEach
    void unlock() {
        jdbc.update("update admin_user set failed_attempts = 0, locked_until = null");
    }

    @Test
    void loginSetsASecureHttpOnlySameSiteCookie() throws Exception {
        HttpResponse<String> response = login(EMAIL, PASSWORD);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"email\":\"admin@tikahome.test\"").doesNotContain("password");
        String cookie = response.headers().firstValue("Set-Cookie").orElseThrow();
        assertThat(cookie).startsWith("tikahome_admin=ey") // a JWT
                .contains("HttpOnly", "SameSite=Strict", "Path=/", "Max-Age=28800");
    }

    @Test
    void emailIsCaseInsensitive() throws Exception {
        assertThat(login("  Admin@TikaHome.test ", PASSWORD).statusCode()).isEqualTo(200);
    }

    @Test
    void cookieGivesAccessToAdminEndpoints() throws Exception {
        String cookie = sessionCookie();

        HttpResponse<String> me = send(get("/api/admin/auth/me").header("Cookie", cookie));

        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains(EMAIL);
    }

    @Test
    void adminEndpointsRequireLogin() throws Exception {
        assertThat(send(get("/api/admin/auth/me")).statusCode()).isEqualTo(401);
        assertThat(send(get("/api/admin/auth/me").header("Cookie", "tikahome_admin=not-a-jwt")).statusCode())
                .isEqualTo(401);
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameAnswer() throws Exception {
        HttpResponse<String> wrongPassword = login(EMAIL, "nope");
        HttpResponse<String> unknownEmail = login("ghost@tikahome.test", "nope");

        assertThat(wrongPassword.statusCode()).isEqualTo(401);
        assertThat(unknownEmail.statusCode()).isEqualTo(401);
        assertThat(wrongPassword.body()).isEqualTo(unknownEmail.body());
        assertThat(wrongPassword.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    void accountIsLockedAfterFiveWrongPasswords() throws Exception {
        for (int i = 0; i < 4; i++) {
            assertThat(login(EMAIL, "wrong-" + i).statusCode()).isEqualTo(401);
        }
        HttpResponse<String> fifth = login(EMAIL, "wrong-5");
        assertThat(fifth.statusCode()).isEqualTo(429);
        assertThat(fifth.headers().firstValue("Retry-After")).isPresent();

        // Even the right password is refused while locked.
        assertThat(login(EMAIL, PASSWORD).statusCode()).isEqualTo(429);
    }

    @Test
    void logoutExpiresTheCookie() throws Exception {
        HttpResponse<String> response = send(post("/api/admin/auth/logout", ""));

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.headers().firstValue("Set-Cookie").orElseThrow()).contains("Max-Age=0");
    }

    @Test
    void writeRequestsFromAnotherWebsiteAreRejected() throws Exception {
        HttpResponse<String> response = send(post("/api/admin/auth/login", credentials(EMAIL, PASSWORD))
                .header("Origin", "https://evil.example"));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    void publicEndpointsStillWorkWithoutLogin() throws Exception {
        assertThat(send(get("/api/properties/6f1c2b8e-4d3a-4f5e-9a7b-1c2d3e4f5a6b")).statusCode()).isEqualTo(200);
    }

    private String sessionCookie() throws Exception {
        String setCookie = login(EMAIL, PASSWORD).headers().firstValue("Set-Cookie").orElseThrow();
        return setCookie.substring(0, setCookie.indexOf(';'));
    }

    private HttpResponse<String> login(String email, String password) throws Exception {
        return send(post("/api/admin/auth/login", credentials(email, password)).header("Origin", "http://localhost:3000"));
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    private HttpRequest.Builder get(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    }

    private HttpRequest.Builder post(String path, String json) {
        return get(path).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json));
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
