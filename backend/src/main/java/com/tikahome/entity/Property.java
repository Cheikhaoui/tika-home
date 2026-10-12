package com.tikahome.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.UUID;

@Entity
@Table(name = "property")
public class Property {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "max_guests", nullable = false)
    private int maxGuests;

    @Column(name = "base_nightly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseNightlyPrice;

    @Column(name = "cleaning_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal cleaningFee;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "min_nights", nullable = false)
    private int minNights;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    /** Secret part of the public iCal export URL: /ical/{token}.ics */
    @Column(name = "ical_export_token", nullable = false, unique = true, length = 64)
    private String icalExportToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Property() {
        // for JPA
    }

    public Property(String slug, String name, int maxGuests, BigDecimal baseNightlyPrice,
                    BigDecimal cleaningFee, String currency, int minNights) {
        this.id = UUID.randomUUID();
        this.slug = slug;
        this.name = name;
        this.maxGuests = maxGuests;
        this.baseNightlyPrice = baseNightlyPrice;
        this.cleaningFee = cleaningFee;
        this.currency = currency;
        this.minNights = minNights;
        this.timeZone = "Africa/Casablanca";
        this.icalExportToken = newExportToken();
        this.createdAt = Instant.now();
    }

    private static String newExportToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** Owner changes the pricing basics. Existing bookings keep the total they were created with. */
    public void updateSettings(BigDecimal baseNightlyPrice, BigDecimal cleaningFee, int minNights, int maxGuests) {
        this.baseNightlyPrice = baseNightlyPrice;
        this.cleaningFee = cleaningFee;
        this.minNights = minNights;
        this.maxGuests = maxGuests;
    }

    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public int getMaxGuests() { return maxGuests; }
    public BigDecimal getBaseNightlyPrice() { return baseNightlyPrice; }
    public BigDecimal getCleaningFee() { return cleaningFee; }
    public String getCurrency() { return currency; }
    public int getMinNights() { return minNights; }
    public ZoneId getZoneId() { return ZoneId.of(timeZone); }
    public String getIcalExportToken() { return icalExportToken; }
}
