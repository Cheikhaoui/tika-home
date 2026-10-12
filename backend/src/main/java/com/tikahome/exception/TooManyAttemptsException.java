package com.tikahome.exception;

import java.time.Instant;

/** Account temporarily locked after too many wrong passwords. */
public class TooManyAttemptsException extends RuntimeException {

    private final Instant retryAfter;

    public TooManyAttemptsException(Instant retryAfter) {
        super("Too many failed attempts, try again later");
        this.retryAfter = retryAfter;
    }

    public Instant getRetryAfter() {
        return retryAfter;
    }
}
