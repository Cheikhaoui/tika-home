package com.tikahome.service;

import com.tikahome.service.EmailOutbox.RenderedEmail;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders Thymeleaf email templates (src/main/resources/templates/email) with texts from
 * i18n/emails*.properties in the recipient's language.
 */
@Component
public class EmailRenderer {

    private final ITemplateEngine templates;
    private final MessageSource messages;

    public EmailRenderer(ITemplateEngine templates, MessageSource messages) {
        this.templates = templates;
        this.messages = messages;
    }

    /**
     * @param rows "label key -> already formatted value" lines shown in the details table
     */
    public RenderedEmail render(String template, Locale locale, String subjectKey, Object[] subjectArgs,
                                List<Map.Entry<String, String>> rows, Map<String, Object> variables) {
        String subject = messages.getMessage(subjectKey, subjectArgs, locale);
        Context context = new Context(locale);
        context.setVariables(variables);
        context.setVariable("rows", rows);
        context.setVariable("subject", subject);
        context.setVariable("lang", locale.getLanguage());
        context.setVariable("dir", isRtl(locale) ? "rtl" : "ltr");
        return new RenderedEmail(subject, templates.process("email/" + template, context));
    }

    /**
     * Our language codes -> formatting locale. Arabic uses ar-MA: Moroccan conventions with Latin
     * digits (12 March), whereas plain "ar" would print Arabic-Indic digits.
     */
    public static Locale locale(String language) {
        return switch (language == null ? "" : language) {
            case "fr" -> Locale.forLanguageTag("fr-MA");
            case "ar" -> Locale.forLanguageTag("ar-MA");
            default -> Locale.ENGLISH;
        };
    }

    public static String formatDate(LocalDate date, Locale locale) {
        return date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", locale));
    }

    public static String formatMoney(BigDecimal amount, String currency, Locale locale) {
        NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        format.setCurrency(Currency.getInstance(currency));
        return format.format(amount);
    }

    private static boolean isRtl(Locale locale) {
        return "ar".equals(locale.getLanguage());
    }
}
