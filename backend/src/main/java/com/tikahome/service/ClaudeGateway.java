package com.tikahome.service;

import com.anthropic.models.beta.messages.MessageCreateParams;
import com.tikahome.model.ClaudeResponse;

import java.util.function.Consumer;

/** One streamed call to Claude. An interface so tests can script Claude's answers without network or cost. */
public interface ClaudeGateway {

    /**
     * @param onText receives the answer's text as it is generated (streaming to the guest)
     */
    ClaudeResponse stream(MessageCreateParams params, Consumer<String> onText);
}
