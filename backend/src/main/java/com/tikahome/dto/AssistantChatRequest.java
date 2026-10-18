package com.tikahome.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** A guest message. {@code conversationId} is null for the first message (the server creates one). */
public record AssistantChatRequest(
        UUID conversationId,
        @NotBlank @Size(max = 1000) String message,
        @NotNull @Pattern(regexp = "fr|en|ar") String language) {
}
