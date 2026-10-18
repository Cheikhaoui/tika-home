package com.tikahome.exception;

/** The assistant can't answer right now. The reason tells the website which message to show. */
public class AssistantUnavailableException extends RuntimeException {

    public enum Reason {
        /** No ANTHROPIC_API_KEY: the website hides the chat. */
        NOT_CONFIGURED,
        /** Today's spending limit is reached: back tomorrow. */
        DAILY_BUDGET,
        /** Claude could not be reached or failed. */
        CLAUDE_ERROR
    }

    private final Reason reason;

    public AssistantUnavailableException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public AssistantUnavailableException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
