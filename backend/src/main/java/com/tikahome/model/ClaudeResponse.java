package com.tikahome.model;

import com.anthropic.models.beta.messages.BetaMessageParam;

import java.util.List;
import java.util.Map;

/**
 * What the assistant needs from one Claude call, independent of SDK response classes (easy to fake in tests).
 * {@code assistantTurn} is the full response content (thinking + tool_use blocks), to send back UNCHANGED
 * with the tool results: the tool loop must be append-only.
 */
public record ClaudeResponse(String text, List<ToolCall> toolCalls, Stop stop, TokenUsage usage,
                             BetaMessageParam assistantTurn) {

    public enum Stop { END_TURN, TOOL_USE, MAX_TOKENS, REFUSAL, OTHER }

    public record ToolCall(String id, String name, Map<String, Object> input) {
    }
}
