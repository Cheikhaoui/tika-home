package com.tikahome.availability;

import com.tikahome.property.Property;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "blocked_date")
public class BlockedDate {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id")
    private Property property;

    /** Inclusive. */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Exclusive, same convention as booking check-out. */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(length = 200)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlockSource source;

    @Column(name = "channel_id")
    private UUID channelId;

    @Column(name = "external_uid")
    private String externalUid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BlockedDate() {
        // for JPA
    }

    public static BlockedDate manual(Property property, LocalDate startDate, LocalDate endDate, String reason) {
        BlockedDate b = new BlockedDate();
        b.id = UUID.randomUUID();
        b.property = property;
        b.startDate = startDate;
        b.endDate = endDate;
        b.reason = reason;
        b.source = BlockSource.MANUAL;
        b.createdAt = Instant.now();
        return b;
    }

    public UUID getId() { return id; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getReason() { return reason; }
    public BlockSource getSource() { return source; }
}
