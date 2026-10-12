package com.tikahome.service;

import com.tikahome.config.NotificationProperties;
import com.tikahome.entity.Booking;
import com.tikahome.entity.BookingStatus;
import com.tikahome.entity.Channel;
import com.tikahome.entity.EmailType;
import com.tikahome.entity.Guest;
import com.tikahome.model.ExternalEvent;
import com.tikahome.service.EmailOutbox.RenderedEmail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Decides who gets which email and with what data, then queues it in the outbox.
 * Always called inside the business transaction (MANDATORY), so emails commit with the data.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class NotificationService {

    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale.ENGLISH);

    private final EmailOutbox outbox;
    private final EmailRenderer renderer;
    private final NotificationProperties properties;

    public NotificationService(EmailOutbox outbox, EmailRenderer renderer, NotificationProperties properties) {
        this.outbox = outbox;
        this.renderer = renderer;
        this.properties = properties;
    }

    /** New booking request: confirmation to the guest (in their language) + alert to the owner. */
    public void bookingRequested(Booking booking) {
        Guest guest = booking.getGuest();

        Locale guestLocale = EmailRenderer.locale(guest.getPreferredLanguage());
        RenderedEmail toGuest = renderer.render("booking-request-guest", guestLocale,
                "booking.request.guest.subject", new Object[]{booking.getReference()},
                stayRows(booking, guestLocale),
                Map.of("guestName", guest.getFullName(), "propertyName", booking.getProperty().getName()));
        // Guest hits "Reply" -> reaches the owner.
        outbox.enqueue(EmailType.BOOKING_REQUEST_GUEST, guest.getEmail(), properties.ownerEmail(),
                guestLocale.getLanguage(), toGuest, null);

        Locale ownerLocale = ownerLocale();
        List<Map.Entry<String, String>> ownerRows = new ArrayList<>();
        ownerRows.add(Map.entry("label.guest", guest.getFullName()));
        ownerRows.add(Map.entry("label.email", guest.getEmail()));
        if (guest.getPhone() != null && !guest.getPhone().isBlank()) {
            ownerRows.add(Map.entry("label.phone", guest.getPhone()));
        }
        ownerRows.add(Map.entry("label.language", guest.getPreferredLanguage().toUpperCase(Locale.ROOT)));
        ownerRows.addAll(stayRows(booking, ownerLocale));
        Map<String, Object> ownerVars = new HashMap<>();
        ownerVars.put("message", booking.getMessage()); // may be null: HashMap, not Map.of
        RenderedEmail toOwner = renderer.render("booking-request-owner", ownerLocale,
                "booking.request.owner.subject",
                new Object[]{booking.getReference(), booking.getCheckIn().format(SHORT_DATE),
                        booking.getCheckOut().format(SHORT_DATE)},
                ownerRows, ownerVars);
        // Owner hits "Reply" -> answers the guest directly.
        outbox.enqueue(EmailType.BOOKING_REQUEST_OWNER, properties.ownerEmail(), guest.getEmail(),
                ownerLocale.getLanguage(), toOwner, null);
    }

    /** The owner confirmed the request: tell the guest, in their language. */
    public void bookingConfirmed(Booking booking) {
        Guest guest = booking.getGuest();
        Locale locale = EmailRenderer.locale(guest.getPreferredLanguage());
        RenderedEmail email = renderer.render("booking-confirmed-guest", locale, "booking.confirmed.guest.subject",
                new Object[]{booking.getReference()}, stayRows(booking, locale),
                Map.of("guestName", guest.getFullName()));
        outbox.enqueue(EmailType.BOOKING_CONFIRMED_GUEST, guest.getEmail(), properties.ownerEmail(),
                locale.getLanguage(), email, null);
    }

    /** The owner declined the request; {@code ownerMessage} (optional) is included as written. */
    public void bookingDeclined(Booking booking, String ownerMessage) {
        Guest guest = booking.getGuest();
        Locale locale = EmailRenderer.locale(guest.getPreferredLanguage());
        Map<String, Object> vars = new HashMap<>();
        vars.put("guestName", guest.getFullName());
        vars.put("ownerMessage", ownerMessage == null || ownerMessage.isBlank() ? null : ownerMessage.strip());
        vars.put("siteUrl", properties.siteUrl() + "/" + locale.getLanguage() + "#booking");
        RenderedEmail email = renderer.render("booking-declined-guest", locale, "booking.declined.guest.subject",
                new Object[]{booking.getReference()}, stayRows(booking, locale), vars);
        outbox.enqueue(EmailType.BOOKING_DECLINED_GUEST, guest.getEmail(), properties.ownerEmail(),
                locale.getLanguage(), email, null);
    }

    /**
     * A platform reservation overlaps a direct booking. Sent once per (booking, reservation) pair:
     * the sync sees the same conflict every 15 minutes, the dedupe key stops repeats.
     */
    public void doubleBooking(Booking booking, Channel channel, ExternalEvent reservation) {
        Locale locale = ownerLocale();
        Guest guest = booking.getGuest();
        List<Map.Entry<String, String>> rows = List.of(
                Map.entry("label.channel", channel.getName()),
                Map.entry("label.reference", booking.getReference()),
                Map.entry("label.status", booking.getStatus().name()),
                Map.entry("label.guest", guest.getFullName()),
                Map.entry("label.email", guest.getEmail()),
                Map.entry("label.directStay", EmailRenderer.formatDate(booking.getCheckIn(), locale) + " → "
                        + EmailRenderer.formatDate(booking.getCheckOut(), locale)),
                Map.entry("label.platformStay", EmailRenderer.formatDate(reservation.range().start(), locale) + " → "
                        + EmailRenderer.formatDate(reservation.range().end(), locale)));
        String recommendation = booking.getStatus() == BookingStatus.CONFIRMED
                ? "double.booking.owner.confirmed" : "double.booking.owner.requested";
        RenderedEmail email = renderer.render("double-booking-owner", locale, "double.booking.owner.subject",
                new Object[]{channel.getName(), booking.getReference()}, rows,
                Map.of("channelName", channel.getName(), "recommendationKey", recommendation));
        String dedupeKey = "double-booking:" + booking.getId() + ":" + channel.getId() + ":" + reservation.uid();
        outbox.enqueue(EmailType.DOUBLE_BOOKING_OWNER, properties.ownerEmail(), null, locale.getLanguage(), email,
                dedupeKey.length() <= 200 ? dedupeKey : dedupeKey.substring(0, 200));
    }

    /** A channel failed several times in a row: new platform reservations are not blocked here. */
    public void syncFailing(Channel channel) {
        Locale locale = ownerLocale();
        ZoneId zone = channel.getProperty().getZoneId();
        String lastSuccess = channel.getLastSuccessAt() == null ? "never"
                : DATE_TIME.format(channel.getLastSuccessAt().atZone(zone));
        List<Map.Entry<String, String>> rows = List.of(
                Map.entry("label.channel", channel.getName()),
                Map.entry("label.lastSuccess", lastSuccess),
                Map.entry("label.lastError", String.valueOf(channel.getLastError())));
        RenderedEmail email = renderer.render("sync-failing-owner", locale, "sync.failing.owner.subject",
                new Object[]{channel.getName()}, rows,
                Map.of("channelName", channel.getName(), "failures", channel.getConsecutiveFailures()));
        outbox.enqueue(EmailType.SYNC_FAILING_OWNER, properties.ownerEmail(), null, locale.getLanguage(), email, null);
    }

    public void syncRecovered(Channel channel) {
        Locale locale = ownerLocale();
        RenderedEmail email = renderer.render("sync-recovered-owner", locale, "sync.recovered.owner.subject",
                new Object[]{channel.getName()}, List.of(), Map.of("channelName", channel.getName()));
        outbox.enqueue(EmailType.SYNC_RECOVERED_OWNER, properties.ownerEmail(), null, locale.getLanguage(), email,
                null);
    }

    private Locale ownerLocale() {
        return EmailRenderer.locale(properties.ownerLanguage());
    }

    private static List<Map.Entry<String, String>> stayRows(Booking booking, Locale locale) {
        long nights = booking.getCheckOut().toEpochDay() - booking.getCheckIn().toEpochDay();
        return List.of(
                Map.entry("label.reference", booking.getReference()),
                Map.entry("label.checkIn", EmailRenderer.formatDate(booking.getCheckIn(), locale)),
                Map.entry("label.checkOut", EmailRenderer.formatDate(booking.getCheckOut(), locale)),
                Map.entry("label.nights", String.valueOf(nights)),
                Map.entry("label.guests", String.valueOf(booking.getGuests())),
                Map.entry("label.total", EmailRenderer.formatMoney(booking.getTotalPrice(), booking.getCurrency(), locale)));
    }
}
