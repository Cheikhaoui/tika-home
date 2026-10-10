package com.tikahome.exception;

/** Downloading an external iCal feed failed. Messages must never contain the feed URL (it holds a secret). */
public class FeedFetchException extends RuntimeException {

    public FeedFetchException(String message) {
        super(message);
    }

    public FeedFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
