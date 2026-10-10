package com.tikahome.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** An external platform (Airbnb, Booking.com, ...) whose iCal calendar is imported as blocked dates. */
@Entity
@Table(name = "channel")
public class Channel {

    private static final int MAX_ERROR_LENGTH = 1000;

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id")
    private Property property;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChannelType type;

    @Column(nullable = false, length = 100)
    private String name;

    /** Contains the platform's secret token: never log it or return it from the API. */
    @Column(name = "import_url", nullable = false, length = 2000)
    private String importUrl;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_sync_status", length = 20)
    private SyncStatus lastSyncStatus;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    @Column(name = "last_conflict_count", nullable = false)
    private int lastConflictCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Channel() {
        // for JPA
    }

    public Channel(Property property, ChannelType type, String name, String importUrl) {
        this.id = UUID.randomUUID();
        this.property = property;
        this.type = type;
        this.name = name;
        this.importUrl = importUrl;
        this.enabled = true;
        this.createdAt = Instant.now();
    }

    public void recordSuccess(Instant at, int conflictCount) {
        lastSyncedAt = at;
        lastSyncStatus = SyncStatus.SUCCESS;
        lastError = null;
        lastConflictCount = conflictCount;
    }

    /** Keeps the previous blocks and conflict count: a failed download tells us nothing new. */
    public void recordFailure(Instant at, String error) {
        lastSyncedAt = at;
        lastSyncStatus = SyncStatus.FAILED;
        lastError = error == null || error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
    }

    public void disable() {
        enabled = false;
    }

    public UUID getId() { return id; }
    public Property getProperty() { return property; }
    public ChannelType getType() { return type; }
    public String getName() { return name; }
    public String getImportUrl() { return importUrl; }
    public boolean isEnabled() { return enabled; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public SyncStatus getLastSyncStatus() { return lastSyncStatus; }
    public String getLastError() { return lastError; }
    public int getLastConflictCount() { return lastConflictCount; }
}
