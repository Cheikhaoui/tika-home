package com.tikahome.service;

import com.tikahome.config.NotificationProperties;
import com.tikahome.entity.OutboxEmail;
import com.tikahome.repository.OutboxEmailRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** Sends due outbox emails over SMTP and records the outcome (sent / retry later / failed). */
@Service
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);
    static final int BATCH_SIZE = 20;

    private final OutboxEmailRepository outbox;
    private final JavaMailSender mailSender;
    private final NotificationProperties properties;
    private final Clock clock;

    public EmailSender(OutboxEmailRepository outbox, JavaMailSender mailSender,
                       NotificationProperties properties, Clock clock) {
        this.outbox = outbox;
        this.mailSender = mailSender;
        this.properties = properties;
        this.clock = clock;
    }

    /** @return number of emails sent successfully in this batch */
    @Transactional
    public int sendDue() {
        Instant now = clock.instant();
        int sent = 0;
        for (OutboxEmail email : outbox.lockDue(now, BATCH_SIZE)) {
            try {
                mailSender.send(toMime(email));
                email.markSent(now);
                sent++;
            } catch (MessagingException | RuntimeException e) {
                // Catch everything per email: one exception escaping would roll back the whole batch,
                // un-marking emails that were already handed to SMTP, which would then be sent twice.
                email.markAttemptFailed(now, e.getMessage(), properties.maxAttempts());
                log.warn("Email {} ({}) attempt {} failed: {}", email.getId(), email.getType(), email.getAttempts(),
                        e.getMessage());
            }
        }
        return sent;
    }

    private MimeMessage toMime(OutboxEmail email) throws MessagingException {
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
        helper.setFrom(properties.fromAddress());
        helper.setTo(email.getRecipient());
        if (email.getReplyTo() != null) {
            helper.setReplyTo(email.getReplyTo());
        }
        helper.setSubject(email.getSubject());
        helper.setText(email.getHtmlBody(), true);
        return mime;
    }
}
