package com.tikahome.common.time;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {

    static DateRange r(String start, String end) {
        return new DateRange(LocalDate.parse(start), LocalDate.parse(end));
    }

    @Test
    void endMustBeAfterStart() {
        assertThatThrownBy(() -> r("2027-01-05", "2027-01-05")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> r("2027-01-05", "2027-01-04")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void countsNights() {
        assertThat(r("2027-01-01", "2027-01-04").nights()).isEqualTo(3);
    }

    @Test
    void backToBackRangesDoNotOverlap() {
        assertThat(r("2027-01-01", "2027-01-05").overlaps(r("2027-01-05", "2027-01-08"))).isFalse();
        assertThat(r("2027-01-01", "2027-01-05").overlaps(r("2027-01-04", "2027-01-08"))).isTrue();
        assertThat(r("2027-01-01", "2027-01-10").overlaps(r("2027-01-03", "2027-01-04"))).isTrue();
    }

    @Test
    void mergesOverlappingAndTouchingRanges() {
        var merged = DateRange.merge(List.of(
                r("2027-01-07", "2027-01-08"),
                r("2027-01-01", "2027-01-03"),
                r("2027-01-03", "2027-01-05"),
                r("2027-01-02", "2027-01-04")));

        assertThat(merged).containsExactly(r("2027-01-01", "2027-01-05"), r("2027-01-07", "2027-01-08"));
    }
}
