package com.tikahome.repository;

import com.tikahome.entity.OutboxEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEmailRepository extends JpaRepository<OutboxEmail, UUID> {

    boolean existsByDedupeKey(String dedupeKey);

    /**
     * Due emails, locked for this transaction. SKIP LOCKED makes rows another instance is already
     * sending invisible to us, so with several app instances an email is still sent only once.
     */
    @Query(value = """
            select * from email_outbox
            where status = 'PENDING' and next_attempt_at <= :now
            order by created_at
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEmail> lockDue(Instant now, int limit);
}
