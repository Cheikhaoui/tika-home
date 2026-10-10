package com.tikahome.repository;

import com.tikahome.entity.Booking;
import com.tikahome.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /** Half-open ranges [a1,a2) and [b1,b2) overlap iff a1 < b2 and b1 < a2. */
    @Query("""
            select b from Booking b
            where b.property.id = :propertyId
              and b.status in :statuses
              and b.checkIn < :to
              and b.checkOut > :from
            order by b.checkIn
            """)
    List<Booking> findOverlapping(UUID propertyId, LocalDate from, LocalDate to, Collection<BookingStatus> statuses);
}
