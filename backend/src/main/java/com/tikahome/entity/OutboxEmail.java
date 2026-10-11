package com.tikahome.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** One email waiting to be sent (or already sent / given up). See V5__email_outbox.sql. */
@Entity
@Table(name = "email_outbox")
public class OutboxEmail {

    /** Wait before attempt 2, 3, 4, 5. */
    static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(30), Duration.ofHours(2));
    private static final int MAX_ERROR_LENGTH = 1000;

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EmailType type;

    @Column(nullable = false, length = 320)
    private String recipient;

    @Column(nullable = false, length = 5)
    private String language;

    @Column(nullable = false, length = 300)
    private String subject;

    @Column(name = "html_body", nullable = false, columnDefinition = "text")
    private String htmlBody;

    @Column(name = "dedupe_key", unique = true, length = 200)
    private String dedupeKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected OutboxEmail() {
        // for JPA
    }

    public OutboxEmail(EmailType type, String recipient, String language, String subject, String htmlBody,
                       String dedupeKey, Instant now) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.recipient = recipient;
        this.language = language;
        this.subject = subject;
        this.htmlBody = htmlBody;
        this.dedupeKey = dedupeKey;
        this.status = EmailStatus.PENDING;
        this.nextAttemptAt = now;
        this.createdAt = now;
    }

    public void markSent(Instant now) {
        attempts++;
        status = EmailStatus.SENT;
        sentAt = now;
        lastError = null;
    }

    /** Schedules a retry with growing delays, or gives up after {@code maxAttempts}. */
    public void markAttemptFailed(Instant now, String error, int maxAttempts) {
        attempts++;
        lastError = error == null || error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
        if (attempts >= maxAttempts) {
            status = EmailStatus.FAILED;
        } else {
            nextAttemptAt = now.plus(RETRY_DELAYS.get(Math.min(attempts, RETRY_DELAYS.size()) - 1));
        }
    }

    public UUID getId() { return id; }
    public EmailType getType() { return type; }
    public String getRecipient() { return recipient; }
    public String getLanguage() { return language; }
    public String getSubject() { return subject; }
    public String getHtmlBody() { return htmlBody; }
    public String getDedupeKey() { return dedupeKey; }
    public EmailStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getLastError() { return lastError; }
    public Instant getSentAt() { return sentAt; }
}
