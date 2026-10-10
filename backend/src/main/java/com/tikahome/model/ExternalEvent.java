package com.tikahome.model;

/** One busy period read from an external iCal feed. {@code uid} identifies it across syncs. */
public record ExternalEvent(String uid, DateRange range, String summary) {
}
