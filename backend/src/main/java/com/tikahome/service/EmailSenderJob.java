package com.tikahome.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Drains the email outbox. Disabled with tikahome.notifications.sender-enabled=false (tests). */
@Component
@ConditionalOnProperty(name = "tikahome.notifications.sender-enabled", havingValue = "true", matchIfMissing = true)
public class EmailSenderJob {

    private final EmailSender sender;

    public EmailSenderJob(EmailSender sender) {
        this.sender = sender;
    }

    @Scheduled(fixedDelayString = "${tikahome.notifications.send-interval:PT30S}", initialDelayString = "PT10S")
    public void run() {
        // Keep draining while full batches come back, so a burst doesn't wait 30 s per 20 emails.
        while (sender.sendDue() == EmailSender.BATCH_SIZE) {
            // next batch
        }
    }
}
