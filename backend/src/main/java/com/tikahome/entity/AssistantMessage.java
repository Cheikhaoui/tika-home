package com.tikahome.entity;

import com.tikahome.model.AssistantResult;
import com.tikahome.model.TokenUsage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One message of an assistant conversation, with the tokens and estimated cost of an answer. */
@Entity
@Table(name = "assistant_message")
public class AssistantMessage {

    @Id
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AssistantRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "tools_used", length = 200)
    private String toolsUsed;

    @Column(nullable = false)
    private boolean refused;

    @Column(nullable = false)
    private boolean failed;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "cache_read_tokens", nullable = false)
    private int cacheReadTokens;

    @Column(name = "cache_write_tokens", nullable = false)
    private int cacheWriteTokens;

    @Column(name = "cost_usd", nullable = false, precision = 12, scale = 6)
    private BigDecimal costUsd;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AssistantMessage() {
    }

    private AssistantMessage(UUID conversationId, AssistantRole role, String text, Instant now) {
        this.id = UUID.randomUUID();
        this.conversationId = conversationId;
        this.role = role;
        this.text = text;
        this.costUsd = BigDecimal.ZERO;
        this.createdAt = now;
    }

    public static AssistantMessage guest(UUID conversationId, String text, Instant now) {
        return new AssistantMessage(conversationId, AssistantRole.GUEST, text, now);
    }

    public static AssistantMessage answer(UUID conversationId, AssistantResult result, Instant now) {
        AssistantMessage message = new AssistantMessage(conversationId, AssistantRole.ASSISTANT, result.text(), now);
        message.toolsUsed = result.toolsUsed().isEmpty() ? null : String.join(",", result.toolsUsed());
        message.refused = result.refused();
        TokenUsage usage = result.usage();
        message.inputTokens = Math.toIntExact(usage.inputTokens());
        message.outputTokens = Math.toIntExact(usage.outputTokens());
        message.cacheReadTokens = Math.toIntExact(usage.cacheReadTokens());
        message.cacheWriteTokens = Math.toIntExact(usage.cacheWriteTokens());
        message.costUsd = usage.estimatedCostUsd();
        return message;
    }

    /** No answer: Claude failed or the guest closed the page. What was already streamed is kept. */
    public static AssistantMessage failure(UUID conversationId, String partialText, Instant now) {
        AssistantMessage message = new AssistantMessage(conversationId, AssistantRole.ASSISTANT, partialText, now);
        message.failed = true;
        return message;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public AssistantRole getRole() {
        return role;
    }

    public String getText() {
        return text;
    }

    public String getToolsUsed() {
        return toolsUsed;
    }

    public boolean isRefused() {
        return refused;
    }

    public boolean isFailed() {
        return failed;
    }

    public int getInputTokens() {
        return inputTokens;
    }

    public int getOutputTokens() {
        return outputTokens;
    }

    public int getCacheReadTokens() {
        return cacheReadTokens;
    }

    public int getCacheWriteTokens() {
        return cacheWriteTokens;
    }

    public BigDecimal getCostUsd() {
        return costUsd;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
