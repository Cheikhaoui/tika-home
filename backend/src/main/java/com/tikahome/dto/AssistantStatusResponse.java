package com.tikahome.dto;

/** Whether the website should show the chat (false: no API key, or today's budget is spent). */
public record AssistantStatusResponse(boolean available) {
}
