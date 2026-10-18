package com.tikahome.repository;

import com.tikahome.entity.AssistantMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AssistantMessageRepository extends JpaRepository<AssistantMessage, UUID> {

    List<AssistantMessage> findTop20ByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    /** Estimated Claude spend since a moment (the daily budget). */
    @Query("select coalesce(sum(m.costUsd), 0) from AssistantMessage m where m.createdAt >= :since")
    BigDecimal costSince(Instant since);

    /** Guest messages sent by one visitor since a moment, oldest first (the hourly limit). */
    @Query("""
            select m.createdAt from AssistantMessage m, AssistantConversation c
            where c.id = m.conversationId and c.visitorHash = :visitorHash
              and m.role = com.tikahome.entity.AssistantRole.GUEST and m.createdAt >= :since
            order by m.createdAt""")
    List<Instant> guestMessageTimesSince(String visitorHash, Instant since);
}
