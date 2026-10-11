package com.tikahome.service;

import com.tikahome.service.EmailOutbox.RenderedEmail;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Renders the real templates and message files, without starting Spring. */
class EmailRendererTest {

    static EmailRenderer renderer() {
        var messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/emails");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setTemplateEngineMessageSource(messages);
        return new EmailRenderer(engine, messages);
    }

    final EmailRenderer renderer = renderer();

    RenderedEmail guestEmail(String language) {
        Locale locale = EmailRenderer.locale(language);
        List<Map.Entry<String, String>> rows = List.of(
                Map.entry("label.reference", "TK-7F3A2B"),
                Map.entry("label.checkIn", EmailRenderer.formatDate(LocalDate.of(2027, 3, 10), locale)),
                Map.entry("label.total", EmailRenderer.formatMoney(new BigDecimal("640.00"), "EUR", locale)));
        return renderer.render("booking-request-guest", locale, "booking.request.guest.subject",
                new Object[]{"TK-7F3A2B"}, rows, Map.of("guestName", "Sara", "propertyName", "Tika Home Marrakech"));
    }

    @Test
    void guestEmailInFrench() {
        RenderedEmail email = guestEmail("fr");

        assertThat(email.subject()).isEqualTo("Nous avons bien reçu votre demande de réservation TK-7F3A2B");
        assertThat(email.html())
                .contains("lang=\"fr\"", "dir=\"ltr\"", "Bonjour Sara,", "Arrivée", "mercredi 10 mars 2027",
                        "Aucun paiement n’est demandé")
                .containsPattern("640,00[\\s\\u00A0\\u202F]€"); // French uses a non-breaking space before €
    }

    @Test
    void guestEmailInEnglish() {
        RenderedEmail email = guestEmail("en");

        assertThat(email.subject()).isEqualTo("We received your booking request TK-7F3A2B");
        assertThat(email.html()).contains("Hello Sara,", "Wednesday 10 March 2027", "€640.00");
    }

    @Test
    void guestEmailInArabicIsRightToLeftWithLatinDigits() {
        RenderedEmail email = guestEmail("ar");

        assertThat(email.subject()).isEqualTo("لقد استلمنا طلب الحجز الخاص بك TK-7F3A2B");
        assertThat(email.html())
                .contains("lang=\"ar\"", "dir=\"rtl\"", "مرحبًا Sara،", "الوصول", "2027", "640")
                .doesNotContain("٢٠٢٧"); // no Arabic-Indic digits
    }

    @Test
    void unknownLanguageFallsBackToEnglish() {
        assertThat(guestEmail("de").subject()).isEqualTo("We received your booking request TK-7F3A2B");
    }

    @Test
    void ownerEmailsAreEnglishAndShowRecommendation() {
        RenderedEmail email = renderer.render("double-booking-owner", Locale.ENGLISH,
                "double.booking.owner.subject", new Object[]{"Airbnb", "TK-7F3A2B"},
                List.of(Map.entry("label.guest", "Sara")),
                Map.of("channelName", "Airbnb", "recommendationKey", "double.booking.owner.requested"));

        assertThat(email.subject()).isEqualTo("Double booking: Airbnb reservation overlaps booking TK-7F3A2B");
        assertThat(email.html()).contains("Double booking detected", "decline it politely",
                "Nothing was cancelled automatically.");
    }
}
