package com.tikahome.booking;

import com.tikahome.pricing.QuoteIssue;

import java.util.Set;

/** The requested stay breaks a booking rule (unavailable dates, min nights, capacity, ...). */
public class BookingRejectedException extends RuntimeException {

    private final Set<QuoteIssue> issues;

    public BookingRejectedException(Set<QuoteIssue> issues) {
        super("Booking cannot be accepted: " + issues);
        this.issues = Set.copyOf(issues);
    }

    public Set<QuoteIssue> getIssues() {
        return issues;
    }

    public boolean isDateConflict() {
        return issues.contains(QuoteIssue.DATES_UNAVAILABLE);
    }
}
