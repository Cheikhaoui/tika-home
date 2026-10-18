package com.tikahome.model;

import java.util.List;

/** One answered guest message: the full text, which tools ran, tokens used, and whether Claude refused. */
public record AssistantResult(String text, List<String> toolsUsed, TokenUsage usage, boolean refused) {
}
