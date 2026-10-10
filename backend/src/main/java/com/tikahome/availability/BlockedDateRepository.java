package com.tikahome.availability;

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
}
