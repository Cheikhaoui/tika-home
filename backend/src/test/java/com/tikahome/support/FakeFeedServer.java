package com.tikahome.support;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Plays the role of Airbnb / Booking.com in tests: serves whatever feed (or error) the test sets. */
public class FakeFeedServer implements AutoCloseable {

    private final HttpServer server;
    private volatile int status = 200;
    private volatile String body = calendar();

    public FakeFeedServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/calendar; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    public String url() {
        return "http://localhost:" + server.getAddress().getPort() + "/calendar.ics?s=secret-token";
    }

    public void respond(int status, String body) {
        this.status = status;
        this.body = body;
    }

    public void serve(String ics) {
        respond(200, ics);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    /** An all-day VEVENT, the format Airbnb and Booking.com use. */
    public static String event(String uid, LocalDate start, LocalDate end, String summary) {
        var f = DateTimeFormatter.BASIC_ISO_DATE;
        return String.join("\r\n", "BEGIN:VEVENT", "UID:" + uid, "DTSTART;VALUE=DATE:" + start.format(f),
                "DTEND;VALUE=DATE:" + end.format(f), "SUMMARY:" + summary, "END:VEVENT");
    }

    public static String calendar(String... events) {
        return "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//fake//EN\r\n"
                + (events.length == 0 ? "" : String.join("\r\n", events) + "\r\n") + "END:VCALENDAR\r\n";
    }
}
