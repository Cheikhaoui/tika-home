package com.tikahome.controller;

import com.tikahome.dto.AssistantChatRequest;
import com.tikahome.dto.AssistantStatusResponse;
import com.tikahome.exception.AssistantUnavailableException;
import com.tikahome.model.AssistantResult;
import com.tikahome.model.ChatContext;
import com.tikahome.service.AssistantChatService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * The AI guest assistant. Answers are streamed with Server-Sent Events so the guest sees the text appear
 * as Claude writes it. Problems found before streaming starts (validation, limits, budget) are normal
 * HTTP errors (problem+json); a failure during streaming is sent as an {@code error} event.
 */
@RestController
public class AssistantController {

    private static final Logger log = LoggerFactory.getLogger(AssistantController.class);
    private static final Duration STREAM_TIMEOUT = Duration.ofMinutes(2);

    private final AssistantChatService chat;
    private final ExecutorService executor;

    public AssistantController(AssistantChatService chat, @Qualifier("assistantExecutor") ExecutorService executor) {
        this.chat = chat;
        this.executor = executor;
    }

    @Operation(summary = "Is the assistant available right now? (API key configured and daily budget not spent)")
    @GetMapping("/api/properties/{propertyId}/assistant")
    public AssistantStatusResponse status(@PathVariable UUID propertyId) {
        return new AssistantStatusResponse(chat.isAvailable());
    }

    @Operation(summary = "Ask the assistant. Server-Sent Events: start {conversationId}, delta {text}..., "
            + "then done {refused} or error {reason}. 429 when a rate limit is hit, 503 when unavailable.")
    @PostMapping("/api/properties/{propertyId}/assistant/chat")
    public ResponseEntity<SseEmitter> chat(@PathVariable UUID propertyId, @Valid @RequestBody AssistantChatRequest request,
                           HttpServletRequest http) {
        // Behind the website proxy, getRemoteAddr() is the visitor's address thanks to X-Forwarded-For
        // (server.forward-headers-strategy in production). It is only used hashed, for rate limits.
        ChatContext context = chat.accept(propertyId, request.conversationId(), http.getRemoteAddr(),
                request.message().strip(), request.language());

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT.toMillis());
        executor.execute(() -> stream(context, emitter));
        // Every layer between here and the browser (Next.js proxy, nginx, CDN) must pass each event on
        // at once: no caching, no compression (it buffers), no proxy buffering.
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }

    private void stream(ChatContext context, SseEmitter emitter) {
        try {
            send(emitter, "start", Map.of("conversationId", context.conversationId()));
            AssistantResult result = chat.answer(context, text -> send(emitter, "delta", Map.of("text", text)));
            send(emitter, "done", Map.of("refused", result.refused()));
            emitter.complete();
        } catch (AssistantUnavailableException e) {
            log.warn("Assistant failed: {}", e.getMessage());
            sendErrorAndComplete(emitter, e.getReason().name());
        } catch (UncheckedIOException e) {
            log.info("Guest left before the answer was complete");
            emitter.complete();
        } catch (RuntimeException e) {
            log.error("Assistant failed", e);
            sendErrorAndComplete(emitter, AssistantUnavailableException.Reason.CLAUDE_ERROR.name());
        }
    }

    private static void sendErrorAndComplete(SseEmitter emitter, String reason) {
        try {
            send(emitter, "error", Map.of("reason", reason));
        } catch (UncheckedIOException ignored) {
            // the guest has already gone
        }
        emitter.complete();
    }

    private static void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
