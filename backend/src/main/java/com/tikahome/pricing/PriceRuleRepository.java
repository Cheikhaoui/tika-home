package com.tikahome.pricing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PriceRuleRepository extends JpaRepository<PriceRule, UUID> {

    @Query("""
            select r from PriceRule r
            where r.property.id = :propertyId
              and r.startDate < :to
              and r.endDate > :from
            """)
    List<PriceRule> findOverlapping(UUID propertyId, LocalDate from, LocalDate to);
}
