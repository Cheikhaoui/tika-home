package com.tikahome.service;

import com.tikahome.entity.PriceRule;
import com.tikahome.entity.Property;
import com.tikahome.model.DateRange;
import com.tikahome.model.NightPrice;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PriceCalculatorTest {

    final Property property = new Property("test", "Test", 6, new BigDecimal("100.00"),
            new BigDecimal("40.00"), "EUR", 2);

    static LocalDate d(String s) {
        return LocalDate.parse(s);
    }

    PriceRule rule(String name, String start, String end, String price, Integer minNights, int priority) {
        return new PriceRule(property, name, d(start), d(end), new BigDecimal(price), minNights, priority);
    }

    @Test
    void usesBasePriceWithoutRules() {
        var pricing = PriceCalculator.calculate(property, List.of(), new DateRange(d("2027-02-01"), d("2027-02-04")));

        assertThat(pricing.subtotal()).isEqualByComparingTo("300.00");
        assertThat(pricing.total()).isEqualByComparingTo("340.00");
        assertThat(pricing.minNights()).isEqualTo(2);
    }

    @Test
    void appliesSeasonalPricePerNightAcrossSeasonBoundary() {
        var high = rule("High", "2027-02-03", "2027-03-01", "150.00", null, 0);

        // nights: 01 (base 100), 02 (base 100), 03 (high 150), 04 (high 150)
        var pricing = PriceCalculator.calculate(property, List.of(high), new DateRange(d("2027-02-01"), d("2027-02-05")));

        assertThat(pricing.nights()).extracting(NightPrice::rule)
                .containsExactly(null, null, "High", "High");
        assertThat(pricing.subtotal()).isEqualByComparingTo("500.00");
    }

    @Test
    void highestPriorityRuleWinsWhenRulesOverlap() {
        var season = rule("Season", "2027-12-01", "2028-01-10", "150.00", 3, 10);
        var newYear = rule("New Year", "2027-12-30", "2028-01-02", "250.00", 5, 20);

        var pricing = PriceCalculator.calculate(property, List.of(season, newYear),
                new DateRange(d("2027-12-29"), d("2028-01-03")));

        // 29 season, 30-31-01 new year, 02 season
        assertThat(pricing.nights()).extracting(NightPrice::rule)
                .containsExactly("Season", "New Year", "New Year", "New Year", "Season");
        assertThat(pricing.subtotal()).isEqualByComparingTo("1050.00");
        assertThat(pricing.minNights()).isEqualTo(3); // check-in night is in "Season"
    }

    @Test
    void minNightsComesFromCheckInNightRule() {
        var high = rule("High", "2027-04-01", "2027-05-01", "150.00", 4, 0);

        var startsBefore = PriceCalculator.calculate(property, List.of(high), new DateRange(d("2027-03-30"), d("2027-04-02")));
        var startsInside = PriceCalculator.calculate(property, List.of(high), new DateRange(d("2027-04-10"), d("2027-04-12")));

        assertThat(startsBefore.minNights()).isEqualTo(2);
        assertThat(startsInside.minNights()).isEqualTo(4);
    }

    @Test
    void ruleWithoutMinNightsFallsBackToPropertyDefault() {
        var promo = rule("Promo", "2027-06-01", "2027-07-01", "90.00", null, 0);

        var pricing = PriceCalculator.calculate(property, List.of(promo), new DateRange(d("2027-06-10"), d("2027-06-12")));

        assertThat(pricing.minNights()).isEqualTo(2);
    }
}
