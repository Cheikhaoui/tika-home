package com.tikahome.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Tokens used by one or more Claude calls. Costs use Claude Opus 5.5 list prices (USD per million tokens). */
public record TokenUsage(long inputTokens, long outputTokens, long cacheReadTokens, long cacheWriteTokens) {

    public static final TokenUsage ZERO = new TokenUsage(0, 0, 0, 0);

    private static final BigDecimal INPUT = new BigDecimal("4.00");
    private static final BigDecimal OUTPUT = new BigDecimal("20.00");
    private static final BigDecimal CACHE_READ = new BigDecimal("0.20");
    private static final BigDecimal CACHE_WRITE = new BigDecimal("5.00"); // 1.25 x input (5-minute cache)
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

    public TokenUsage plus(TokenUsage other) {
        return new TokenUsage(inputTokens + other.inputTokens, outputTokens + other.outputTokens,
                cacheReadTokens + other.cacheReadTokens, cacheWriteTokens + other.cacheWriteTokens);
    }

    /** Estimated cost in USD (a refusal fallback may run on another model with other prices). */
    public BigDecimal estimatedCostUsd() {
        return BigDecimal.valueOf(inputTokens).multiply(INPUT)
                .add(BigDecimal.valueOf(outputTokens).multiply(OUTPUT))
                .add(BigDecimal.valueOf(cacheReadTokens).multiply(CACHE_READ))
                .add(BigDecimal.valueOf(cacheWriteTokens).multiply(CACHE_WRITE))
                .divide(MILLION, 6, RoundingMode.HALF_UP);
    }
}
