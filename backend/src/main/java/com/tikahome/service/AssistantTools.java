package com.tikahome.service;

import com.anthropic.core.JsonValue;
import com.anthropic.models.beta.messages.BetaTool;
import com.tikahome.exception.BadRequestException;
import com.tikahome.exception.NotFoundException;
import com.tikahome.model.DateRange;
import com.tikahome.model.Quote;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The assistant's tools: thin read-only wrappers around the same services the website uses, so prices and
 * availability always come from the database, never from the model's imagination. No tool can book,
 * cancel or change anything.
 */
@Component
public class AssistantTools {

    static final String CHECK_STAY = "check_stay";
    static final String FIND_AVAILABLE_DATES = "find_available_dates";
    static final int MAX_SEARCH_DAYS = 92;

    private final QuoteService quoteService;
    private final AvailabilityService availabilityService;

    public AssistantTools(QuoteService quoteService, AvailabilityService availabilityService) {
        this.quoteService = quoteService;
        this.availabilityService = availabilityService;
    }

    /** strict: Claude's arguments always match the schema exactly. */
    public List<BetaTool> definitions() {
        return List.of(
                tool(CHECK_STAY,
                        "Check whether the house can be booked for given dates and number of guests, and get the exact "
                                + "price (per-night prices, cleaning fee, total). Use it for ANY question about price, "
                                + "availability or minimum stay for specific dates.",
                        Map.of("check_in", dateProperty("Arrival date (first night), YYYY-MM-DD"),
                                "check_out", dateProperty("Departure date (not a night), YYYY-MM-DD"),
                                "guests", Map.of("type", "integer", "description", "Number of guests")),
                        List.of("check_in", "check_out", "guests")),
                tool(FIND_AVAILABLE_DATES,
                        "List the periods when the house is free between two dates (at most " + MAX_SEARCH_DAYS
                                + " days apart). Use it when the guest asks when the house is available.",
                        Map.of("from", dateProperty("Start of the period to search, YYYY-MM-DD"),
                                "to", dateProperty("End of the period to search (exclusive), YYYY-MM-DD")),
                        List.of("from", "to")));
    }

    /** Runs a tool. Problems become an error message for Claude (it then explains them to the guest). */
    public ToolResult execute(UUID propertyId, String name, Map<String, Object> input) {
        try {
            return switch (name) {
                case CHECK_STAY -> new ToolResult(checkStay(propertyId, input), false);
                case FIND_AVAILABLE_DATES -> new ToolResult(findAvailableDates(propertyId, input), false);
                default -> new ToolResult(Map.of("error", "Unknown tool " + name), true);
            };
        } catch (IllegalArgumentException | DateTimeParseException | BadRequestException | NotFoundException e) {
            return new ToolResult(Map.of("error", String.valueOf(e.getMessage())), true);
        }
    }

    private Map<String, Object> checkStay(UUID propertyId, Map<String, Object> input) {
        DateRange stay = new DateRange(date(input, "check_in"), date(input, "check_out"));
        int guests = ((Number) input.get("guests")).intValue();
        Quote quote = quoteService.quote(propertyId, stay, guests);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("check_in", stay.start().toString());
        result.put("check_out", stay.end().toString());
        result.put("nights", quote.nights());
        result.put("guests", guests);
        result.put("bookable", quote.bookable());
        result.put("problems", quote.issues().stream().map(Enum::name).toList());
        result.put("minimum_nights_for_these_dates", quote.pricing().minNights());
        result.put("currency", quote.currency());
        result.put("nightly_prices", quote.pricing().nights().stream()
                .map(n -> Map.of("night", n.date().toString(), "price", n.price())).toList());
        result.put("cleaning_fee", quote.pricing().cleaningFee());
        result.put("total", quote.total());
        return result;
    }

    private Map<String, Object> findAvailableDates(UUID propertyId, Map<String, Object> input) {
        DateRange range = new DateRange(date(input, "from"), date(input, "to"));
        if (range.nights() > MAX_SEARCH_DAYS) {
            throw new BadRequestException("Search at most " + MAX_SEARCH_DAYS + " days at a time");
        }
        var unavailable = availabilityService.getAvailability(propertyId, range).unavailable();
        List<Map<String, String>> free = new ArrayList<>();
        LocalDate cursor = range.start();
        for (DateRange taken : unavailable) {
            if (cursor.isBefore(taken.start())) {
                free.add(Map.of("from", cursor.toString(), "to", taken.start().toString()));
            }
            if (taken.end().isAfter(cursor)) {
                cursor = taken.end();
            }
        }
        if (cursor.isBefore(range.end())) {
            free.add(Map.of("from", cursor.toString(), "to", range.end().toString()));
        }
        return Map.of("free_periods", free, "note",
                "'to' is the departure day. Minimum stay rules still apply: confirm with check_stay.");
    }

    private static LocalDate date(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing " + key);
        }
        return LocalDate.parse(value.toString());
    }

    private static Map<String, Object> dateProperty(String description) {
        return Map.of("type", "string", "format", "date", "description", description);
    }

    private static BetaTool tool(String name, String description, Map<String, Map<String, Object>> properties,
                                 List<String> required) {
        BetaTool.InputSchema.Properties.Builder props = BetaTool.InputSchema.Properties.builder();
        properties.forEach((key, schema) -> props.putAdditionalProperty(key, JsonValue.from(schema)));
        return BetaTool.builder()
                .name(name)
                .description(description)
                .inputSchema(BetaTool.InputSchema.builder()
                        .properties(props.build())
                        .required(required)
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .build())
                .strict(true)
                .build();
    }

    /** {@code content} is serialised to JSON for Claude. */
    public record ToolResult(Map<String, Object> content, boolean isError) {
    }
}
