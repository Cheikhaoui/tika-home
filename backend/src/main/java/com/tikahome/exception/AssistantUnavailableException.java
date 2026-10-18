package com.tikahome.exception;

/** The assistant can't answer right now (no API key, Claude unreachable, budget reached...). */
public class AssistantUnavailableException extends RuntimeException {

    public AssistantUnavailableException(String message) {
        super(message);
    }

    public AssistantUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
