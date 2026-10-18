package com.tikahome.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

/** Settings under {@code tikahome.assistant.*} for the AI guest assistant (phase 7). */
@ConfigurationProperties("tikahome.assistant")
public record AssistantProperties(
        /* Multilingual sentence-embedding model (ONNX) + its tokenizer: https:// (cached) or file: URIs. */
        @DefaultValue("https://huggingface.co/Xenova/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model_quantized.onnx")
        String embeddingModelUri,
        @DefaultValue("https://huggingface.co/Xenova/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/tokenizer.json")
        String embeddingTokenizerUri,
        /* Where downloaded model files are kept between restarts. */
        @DefaultValue("${user.home}/.cache/tikahome/models") String modelCacheDirectory,
        /* Knowledge base: chunks retrieved per question, and the minimum cosine similarity to keep one. */
        @DefaultValue("4") int retrievalTopK,
        @DefaultValue("0.35") double retrievalMinSimilarity,
        /* Claude. */
        @DefaultValue("claude-opus-5-5") String model,
        @DefaultValue("low") String effort,
        @DefaultValue("2048") long maxTokens,
        /* Abuse and cost protection. */
        @DefaultValue("20") int maxMessagesPerHourPerVisitor,
        @DefaultValue("30") int maxMessagesPerConversation,
        @DefaultValue("5.00") BigDecimal dailyBudgetUsd) {
}
