package com.tikahome.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "booking")
public class Booking {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id")
    private Guest guest;

    /** First night (inclusive). */
    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    /** Departure day (exclusive): the night of check_out is not part of the stay. */
    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private int guests;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 2000)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Booking() {
        // for JPA
    }

    public static Booking request(Property property, Guest guest, LocalDate checkIn, LocalDate checkOut,
                                  int guests, BigDecimal totalPrice, String message) {
        Booking b = new Booking();
        b.id = UUID.randomUUID();
        b.property = property;
        b.guest = guest;
        b.checkIn = checkIn;
        b.checkOut = checkOut;
        b.guests = guests;
        b.status = BookingStatus.REQUESTED;
        b.totalPrice = totalPrice;
        b.currency = property.getCurrency();
        b.message = message;
        b.createdAt = Instant.now();
        b.updatedAt = b.createdAt;
        return b;
    }

    public void decline() {
        transitionFrom(BookingStatus.REQUESTED, BookingStatus.DECLINED);
    }

    private void transitionFrom(BookingStatus expected, BookingStatus next) {
        if (status != expected) {
            throw new IllegalStateException("Booking " + id + " is " + status + ", expected " + expected);
        }
        status = next;
    }

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    /**
     * Short reference for emails and phone calls, e.g. TK-7F3A2B (from the UUID, so no extra column).
     * Display only: not guaranteed unique, never use it to look a booking up.
     */
    public String getReference() {
        return "TK-" + id.toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    public UUID getId() { return id; }
    public Property getProperty() { return property; }
    public Guest getGuest() { return guest; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getGuests() { return guests; }
    public BookingStatus getStatus() { return status; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public String getCurrency() { return currency; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
