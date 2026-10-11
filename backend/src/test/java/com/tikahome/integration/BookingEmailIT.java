package com.tikahome.integration;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.tikahome.TestcontainersConfiguration;
import com.tikahome.dto.CreateBookingRequest;
import com.tikahome.dto.GuestRequest;
import com.tikahome.entity.Property;
import com.tikahome.exception.BookingRejectedException;
import com.tikahome.repository.OutboxEmailRepository;
import com.tikahome.repository.PropertyRepository;
import com.tikahome.service.BookingService;
import com.tikahome.service.EmailSender;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingEmailIT {

    @RegisterExtension
    static GreenMailExtension smtp = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired BookingService bookingService;
    @Autowired EmailSender sender;
    @Autowired PropertyRepository properties;
    @Autowired OutboxEmailRepository outbox;
    @Autowired JdbcTemplate jdbc;

    Property property;
    final LocalDate checkIn = LocalDate.now().plusDays(60);

    @BeforeEach
    void setUp() {
        jdbc.update("delete from email_outbox");
        property = properties.save(new Property("mail-" + UUID.randomUUID(), "Tika Home Marrakech", 6,
                new BigDecimal("100.00"), new BigDecimal("40.00"), "EUR", 1));
    }

    @Test
    void bookingRequestEmailsGuestInTheirLanguageAndOwnerInEnglish() throws Exception {
        var booking = bookingService.request(request("sara@example.com", "fr", "Arrivée tardive"));

        assertThat(sender.sendDue()).isEqualTo(2);

        MimeMessage toGuest = receivedBy("sara@example.com");
        assertThat(toGuest.getSubject()).isEqualTo("Nous avons bien reçu votre demande de réservation " + booking.getReference());
        assertThat((String) toGuest.getContent()).contains("Bonjour Sara Benali,", "Tika Home Marrakech", "340,00");
        assertThat(toGuest.getReplyTo()[0].toString()).isEqualTo("cheikhaoui.hicham@gmail.com"); // reply -> owner

        MimeMessage toOwner = receivedBy("cheikhaoui.hicham@gmail.com");
        assertThat(toOwner.getSubject()).startsWith("New booking request " + booking.getReference());
        assertThat((String) toOwner.getContent())
                .contains("Sara Benali", "sara@example.com", "+212600000000", "FR", "Arrivée tardive", "€340.00");
        assertThat(toOwner.getReplyTo()[0].toString()).isEqualTo("sara@example.com"); // reply -> guest
    }

    @Test
    void rejectedBookingSendsNothing() {
        bookingService.request(request("first@example.com", "en", null));
        jdbc.update("delete from email_outbox");

        assertThatThrownBy(() -> bookingService.request(request("second@example.com", "en", null)))
                .isInstanceOf(BookingRejectedException.class);
        assertThat(outbox.count()).isZero();
    }

    @Test
    void mailServerDownDoesNotAffectTheBooking() {
        smtp.stop();

        var booking = bookingService.request(request("guest@example.com", "ar", null));

        assertThat(booking.getId()).isNotNull();
        assertThat(sender.sendDue()).isZero(); // both emails wait in the outbox for a retry
        assertThat(outbox.count()).isEqualTo(2);
    }

    private MimeMessage receivedBy(String address) {
        return Arrays.stream(smtp.getReceivedMessages())
                .filter(m -> {
                    try {
                        return m.getAllRecipients()[0].toString().equals(address);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst().orElseThrow();
    }

    private CreateBookingRequest request(String email, String language, String message) {
        return new CreateBookingRequest(property.getId(), checkIn, checkIn.plusDays(3), 2,
                new GuestRequest("Sara Benali", email, "+212600000000", language), message);
    }
}
