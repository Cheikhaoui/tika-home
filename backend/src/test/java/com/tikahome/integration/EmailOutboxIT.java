package com.tikahome.integration;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.tikahome.TestcontainersConfiguration;
import com.tikahome.entity.EmailStatus;
import com.tikahome.entity.EmailType;
import com.tikahome.entity.OutboxEmail;
import com.tikahome.repository.OutboxEmailRepository;
import com.tikahome.service.EmailOutbox;
import com.tikahome.service.EmailOutbox.RenderedEmail;
import com.tikahome.service.EmailSender;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EmailOutboxIT {

    /** In-memory SMTP server on localhost:3025 (see test config/application.properties). */
    @RegisterExtension
    static GreenMailExtension smtp = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired EmailOutbox emailOutbox;
    @Autowired EmailSender sender;
    @Autowired OutboxEmailRepository outbox;
    @Autowired TransactionTemplate tx;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clearOutbox() {
        jdbc.update("delete from email_outbox");
    }

    @Test
    void queuedEmailIsSentWithUtf8Content() throws Exception {
        queue("guest@example.com", "تم استلام طلبك", "<p dir=\"rtl\">شكرا</p>", null);

        assertThat(sender.sendDue()).isEqualTo(1);

        MimeMessage[] received = smtp.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getSubject()).isEqualTo("تم استلام طلبك");
        assertThat(received[0].getAllRecipients()[0].toString()).isEqualTo("guest@example.com");
        assertThat(received[0].getReplyTo()[0].toString()).isEqualTo("owner@example.com");
        // getContent() decodes the transfer encoding (quoted-printable) back to the original UTF-8 text.
        assertThat((String) received[0].getContent()).contains("dir=\"rtl\"", "شكرا");
        assertThat(outbox.findAll()).singleElement().satisfies(e -> {
            assertThat(e.getStatus()).isEqualTo(EmailStatus.SENT);
            assertThat(e.getSentAt()).isNotNull();
        });
    }

    @Test
    void smtpDownKeepsEmailPendingAndRetriesLater() {
        queue("owner@example.com", "New request", "<p>hi</p>", null);
        smtp.stop();

        assertThat(sender.sendDue()).isZero();
        OutboxEmail afterFailure = outbox.findAll().getFirst();
        assertThat(afterFailure.getStatus()).isEqualTo(EmailStatus.PENDING);
        assertThat(afterFailure.getAttempts()).isEqualTo(1);
        assertThat(afterFailure.getNextAttemptAt()).isAfter(java.time.Instant.now()); // backoff
        assertThat(sender.sendDue()).isZero(); // not due yet

        smtp.start();
        makeAllDue();
        assertThat(sender.sendDue()).isEqualTo(1);
        assertThat(smtp.getReceivedMessages()).hasSize(1);
    }

    @Test
    void givesUpAfterMaxAttempts() {
        queue("owner@example.com", "Alert", "<p>x</p>", null);
        smtp.stop();

        for (int i = 0; i < 5; i++) {
            makeAllDue();
            sender.sendDue();
        }

        OutboxEmail email = outbox.findAll().getFirst();
        assertThat(email.getStatus()).isEqualTo(EmailStatus.FAILED);
        assertThat(email.getAttempts()).isEqualTo(5);
        assertThat(email.getLastError()).isNotBlank();
    }

    @Test
    void sameDedupeKeyIsQueuedOnlyOnce() {
        String key = "conflict:" + UUID.randomUUID();
        assertThat(queue("owner@example.com", "Double booking", "<p>1</p>", key)).isTrue();
        assertThat(queue("owner@example.com", "Double booking", "<p>2</p>", key)).isFalse();

        assertThat(outbox.count()).isEqualTo(1);
    }

    @Test
    void enqueueOutsideATransactionIsAProgrammingError() {
        assertThatThrownBy(() -> emailOutbox.enqueue(EmailType.BOOKING_REQUEST_OWNER, "a@example.com", null, "en",
                new RenderedEmail("s", "b"), null))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private boolean queue(String to, String subject, String html, String dedupeKey) {
        return tx.execute(s -> emailOutbox.enqueue(EmailType.BOOKING_REQUEST_GUEST, to, "owner@example.com", "ar",
                new RenderedEmail(subject, html), dedupeKey));
    }

    private void makeAllDue() {
        jdbc.update("update email_outbox set next_attempt_at = now() - interval '1 second' where status = 'PENDING'");
    }
}
