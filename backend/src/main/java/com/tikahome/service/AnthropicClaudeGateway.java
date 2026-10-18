package com.tikahome.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.helpers.BetaMessageAccumulator;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaRawMessageStreamEvent;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.tikahome.model.ClaudeResponse;
import com.tikahome.model.TokenUsage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** The real gateway: official Anthropic Java SDK, streaming (beta endpoint, for server-side refusal fallbacks). */
@Component
public class AnthropicClaudeGateway implements ClaudeGateway {

    private final AnthropicClient client;

    public AnthropicClaudeGateway(AnthropicClient client) {
        this.client = client;
    }

    @Override
    public ClaudeResponse stream(MessageCreateParams params, Consumer<String> onText) {
        BetaMessageAccumulator accumulator = BetaMessageAccumulator.create();
        try (StreamResponse<BetaRawMessageStreamEvent> response = client.beta().messages().createStreaming(params)) {
            response.stream()
                    .peek(accumulator::accumulate) // rebuilds the full message (incl. tool_use blocks)
                    .flatMap(event -> event.contentBlockDelta().stream())
                    .flatMap(delta -> delta.delta().text().stream()) // only answer text goes to the guest
                    .forEach(text -> onText.accept(text.text()));
        }
        return toResponse(accumulator.message());
    }

    @SuppressWarnings("unchecked")
    static ClaudeResponse toResponse(BetaMessage message) {
        StringBuilder text = new StringBuilder();
        List<ClaudeResponse.ToolCall> toolCalls = new ArrayList<>();
        message.content().forEach(block -> {
            block.text().ifPresent(t -> text.append(t.text()));
            block.toolUse().ifPresent(t -> toolCalls.add(new ClaudeResponse.ToolCall(t.id(), t.name(),
                    t._input().convert(Map.class))));
        });
        var usage = message.usage();
        TokenUsage tokens = new TokenUsage(usage.inputTokens(), usage.outputTokens(),
                usage.cacheReadInputTokens().orElse(0L), usage.cacheCreationInputTokens().orElse(0L));
        ClaudeResponse.Stop stop = message.stopReason().map(AnthropicClaudeGateway::stop).orElse(ClaudeResponse.Stop.OTHER);
        return new ClaudeResponse(text.toString(), toolCalls, stop, tokens, message.toParam());
    }

    private static ClaudeResponse.Stop stop(BetaStopReason reason) {
        if (reason.equals(BetaStopReason.END_TURN)) return ClaudeResponse.Stop.END_TURN;
        if (reason.equals(BetaStopReason.TOOL_USE)) return ClaudeResponse.Stop.TOOL_USE;
        if (reason.equals(BetaStopReason.MAX_TOKENS)) return ClaudeResponse.Stop.MAX_TOKENS;
        if (reason.equals(BetaStopReason.REFUSAL)) return ClaudeResponse.Stop.REFUSAL;
        return ClaudeResponse.Stop.OTHER;
    }
}
