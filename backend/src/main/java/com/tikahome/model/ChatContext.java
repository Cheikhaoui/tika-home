package com.tikahome.model;

import java.util.List;
import java.util.UUID;

/** A guest message accepted for answering: its conversation and the earlier turns to replay. */
public record ChatContext(UUID propertyId, UUID conversationId, List<ChatTurn> history, String question,
                          String language) {
}
