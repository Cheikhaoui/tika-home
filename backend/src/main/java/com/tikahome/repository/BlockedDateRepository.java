package com.tikahome.repository;

import com.tikahome.entity.BlockSource;
import com.tikahome.entity.BlockedDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BlockedDateRepository extends JpaRepository<BlockedDate, UUID> {

    @Query("""
            select b from BlockedDate b
            where b.property.id = :propertyId
              and b.startDate < :to
              and b.endDate > :from
            order by b.startDate
            """)
    List<BlockedDate> findOverlapping(UUID propertyId, LocalDate from, LocalDate to);

    /** All blocks imported from one channel (the sync diffs the feed against these). */
    List<BlockedDate> findByChannelId(UUID channelId);

    /** Blocks of one source that are not over yet (used by the iCal export). */
    @Query("""
            select b from BlockedDate b
            where b.property.id = :propertyId
              and b.source = :source
              and b.endDate > :from
            order by b.startDate
            """)
    List<BlockedDate> findBySourceEndingAfter(UUID propertyId, BlockSource source, LocalDate from);
}
