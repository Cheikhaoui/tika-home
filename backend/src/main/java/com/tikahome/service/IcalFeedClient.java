package com.tikahome.service;

import com.tikahome.config.IcalProperties;
import com.tikahome.exception.FeedFetchException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;

/** Downloads iCal feeds with timeouts and a size limit (a broken or hostile URL must not hang or flood us). */
@Component
public class IcalFeedClient {

    private final IcalProperties properties;
    private final HttpClient http;

    public IcalFeedClient(IcalProperties properties) {
        this.properties = properties;
        this.http = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL) // never downgrades https -> http
                .build();
    }

    public String fetch(String url) {
        URI uri = validate(url);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(properties.readTimeout())
                .header("User-Agent", "TikaHome-CalendarSync/1.0")
                .header("Accept", "text/calendar, */*;q=0.1")
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() / 100 != 2) {
                    throw new FeedFetchException("HTTP " + response.statusCode());
                }
                long max = properties.maxFeedSize().toBytes();
                byte[] bytes = body.readNBytes((int) max + 1);
                if (bytes.length > max) {
                    throw new FeedFetchException("Feed larger than " + properties.maxFeedSize());
                }
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (HttpTimeoutException e) {
            throw new FeedFetchException("Timed out", e);
        } catch (IOException e) {
            // Only the exception type: IOException messages can contain the host or URL.
            throw new FeedFetchException("Download failed (" + e.getClass().getSimpleName() + ")", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FeedFetchException("Interrupted", e);
        }
    }

    private URI validate(String url) {
        URI uri;
        try {
            uri = URI.create(url.strip());
        } catch (IllegalArgumentException e) {
            throw new FeedFetchException("Invalid feed URL");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        boolean allowed = scheme.equals("https") || (scheme.equals("http") && properties.allowHttp());
        if (!allowed || uri.getHost() == null) {
            throw new FeedFetchException("Feed URL must use https");
        }
        return uri;
    }
}
