package com.tikahome.service;

import com.tikahome.config.NotificationProperties;
import com.tikahome.entity.Booking;
import com.tikahome.entity.EmailType;
import com.tikahome.entity.Guest;
import com.tikahome.service.EmailOutbox.RenderedEmail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
        outbox.enqueue(EmailType.BOOKING_REQUEST_GUEST, guest.getEmail(), guestLocale.getLanguage(), toGuest, null);

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
        outbox.enqueue(EmailType.BOOKING_REQUEST_OWNER, properties.ownerEmail(), ownerLocale.getLanguage(), toOwner,
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
