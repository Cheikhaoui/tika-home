package com.tikahome.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Half-open date range [start, end): {@code start} is the first night, {@code end} is not included.
 * Same semantics as Postgres {@code daterange(start, end, '[)')}.
 */
public record DateRange(LocalDate start, LocalDate end) {

    public DateRange {
        if (start == null || end == null) {
            throw new IllegalArgumentException("start and end are required");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end (" + end + ") must be after start (" + start + ")");
        }
    }

    public long nights() {
        return ChronoUnit.DAYS.between(start, end);
    }

    public boolean overlaps(DateRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public boolean contains(LocalDate night) {
        return !night.isBefore(start) && night.isBefore(end);
    }

    /** Every night in the range, in order. */
    public Stream<LocalDate> nightsStream() {
        return start.datesUntil(end);
    }

    /** Sorts and merges overlapping or touching ranges: [1,3) + [3,5) + [7,8) -> [1,5) + [7,8). */
    public static List<DateRange> merge(List<DateRange> ranges) {
        List<DateRange> sorted = ranges.stream().sorted(Comparator.comparing(DateRange::start)).toList();
        List<DateRange> merged = new ArrayList<>();
        for (DateRange r : sorted) {
            if (!merged.isEmpty() && !r.start().isAfter(merged.getLast().end())) {
                DateRange last = merged.removeLast();
                merged.add(new DateRange(last.start(), r.end().isAfter(last.end()) ? r.end() : last.end()));
            } else {
                merged.add(r);
            }
        }
        return merged;
    }
}
