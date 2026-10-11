package com.tikahome.service;

import com.tikahome.entity.EmailType;
import com.tikahome.entity.OutboxEmail;
import com.tikahome.repository.OutboxEmailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Queues emails. Must be called inside the business transaction, so email and data commit together. */
@Service
public class EmailOutbox {

    private final OutboxEmailRepository outbox;
    private final Clock clock;

    public EmailOutbox(OutboxEmailRepository outbox, Clock clock) {
        this.outbox = outbox;
        this.clock = clock;
    }

    /**
     * @param replyTo   optional Reply-To address
     * @param dedupeKey optional; if an email with the same key was ever queued, nothing happens
     * @return true if queued
     */
    @Transactional(propagation = Propagation.MANDATORY) // fails fast if someone calls it outside a transaction
    public boolean enqueue(EmailType type, String recipient, String replyTo, String language, RenderedEmail email,
                           String dedupeKey) {
        if (dedupeKey != null && outbox.existsByDedupeKey(dedupeKey)) {
            return false;
        }
        outbox.save(new OutboxEmail(type, recipient, replyTo, language, email.subject(), email.html(), dedupeKey,
                clock.instant()));
        return true;
    }

    /** Subject + HTML body, produced by the template renderer. */
    public record RenderedEmail(String subject, String html) {
    }
}
