package com.tikahome.model;

/** Reasons a stay can't be booked as requested. Machine-readable so the website can translate them. */
public enum QuoteIssue {
    DATES_UNAVAILABLE,
    CHECK_IN_IN_PAST,
    BELOW_MIN_NIGHTS,
    ABOVE_MAX_NIGHTS,
    TOO_MANY_GUESTS
}
