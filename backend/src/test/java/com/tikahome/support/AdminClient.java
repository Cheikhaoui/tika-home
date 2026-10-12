package com.tikahome.support;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Logged-in HTTP client for admin integration tests (the test admin comes from test config). */
public class AdminClient {

    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;
    private final String cookie;

    public AdminClient(int port) throws Exception {
        this.baseUrl = "http://localhost:" + port;
        HttpResponse<String> login = http.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/admin/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"admin@tikahome.test\",\"password\":\"correct-horse-battery-staple\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        String setCookie = login.headers().firstValue("Set-Cookie").orElseThrow(() ->
                new IllegalStateException("Login failed: " + login.statusCode() + " " + login.body()));
        this.cookie = setCookie.substring(0, setCookie.indexOf(';'));
    }

    public HttpResponse<String> get(String path) throws Exception {
        return send(request(path).GET());
    }

    public HttpResponse<String> post(String path, String json) throws Exception {
        return send(request(path).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)));
    }

    public HttpResponse<String> put(String path, String json) throws Exception {
        return send(request(path).header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json)));
    }

    public HttpResponse<String> delete(String path) throws Exception {
        return send(request(path).DELETE());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path)).header("Cookie", cookie);
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
