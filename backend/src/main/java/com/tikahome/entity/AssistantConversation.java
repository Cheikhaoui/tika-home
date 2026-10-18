package com.tikahome.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One chat between a website visitor and the AI assistant. See V11__assistant_conversations.sql. */
@Entity
@Table(name = "assistant_conversation")
public class AssistantConversation {

    @Id
    private UUID id;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    /** HMAC of the visitor's IP address: groups their messages without storing the IP. */
    @Column(name = "visitor_hash", nullable = false, length = 64)
    private String visitorHash;

    @Column(nullable = false, length = 5)
    private String language;

    @Column(name = "guest_messages", nullable = false)
    private int guestMessages;

    @Column(name = "cost_usd", nullable = false, precision = 12, scale = 6)
    private BigDecimal costUsd;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    protected AssistantConversation() {
    }

    public AssistantConversation(UUID propertyId, String visitorHash, String language, Instant now) {
        this.id = UUID.randomUUID();
        this.propertyId = propertyId;
        this.visitorHash = visitorHash;
        this.language = language;
        this.costUsd = BigDecimal.ZERO;
        this.createdAt = now;
        this.lastMessageAt = now;
    }

    public void guestMessage(String language, Instant now) {
        this.language = language;
        this.guestMessages++;
        this.lastMessageAt = now;
    }

    public void addCost(BigDecimal cost) {
        this.costUsd = costUsd.add(cost);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPropertyId() {
        return propertyId;
    }

    public String getVisitorHash() {
        return visitorHash;
    }

    public String getLanguage() {
        return language;
    }

    public int getGuestMessages() {
        return guestMessages;
    }

    public BigDecimal getCostUsd() {
        return costUsd;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }
}
