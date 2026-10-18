package com.tikahome.exception;

import java.time.Instant;

/** A visitor sent too many messages (abuse and cost protection). */
public class AssistantLimitException extends RuntimeException {

    public enum Limit {
        /** Too many messages in the last hour: retry after {@link #getRetryAfter()}. */
        HOURLY,
        /** The conversation is long enough: start a new one. */
        CONVERSATION
    }

    private final Limit limit;
    private final Instant retryAfter;

    public AssistantLimitException(Limit limit, Instant retryAfter) {
        super(limit == Limit.HOURLY ? "Too many messages, try again later" : "This conversation is too long, start a new one");
        this.limit = limit;
        this.retryAfter = retryAfter;
    }

    public Limit getLimit() {
        return limit;
    }

    /** Null when waiting doesn't help (conversation limit). */
    public Instant getRetryAfter() {
        return retryAfter;
    }
}
