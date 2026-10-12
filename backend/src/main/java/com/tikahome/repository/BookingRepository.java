package com.tikahome.repository;

import com.tikahome.entity.Booking;
import com.tikahome.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // Admin lists. @EntityGraph loads the guest in the same query (avoids one extra query per row: "N+1").
    // Dashboard (one property).
    long countByPropertyIdAndStatus(UUID propertyId, BookingStatus status);

    @EntityGraph(attributePaths = "guest")
    List<Booking> findTop5ByPropertyIdAndStatusOrderByCreatedAtAsc(UUID propertyId, BookingStatus status);

    @EntityGraph(attributePaths = "guest")
    List<Booking> findTop5ByPropertyIdAndStatusAndCheckInGreaterThanEqualOrderByCheckInAsc(
            UUID propertyId, BookingStatus status, LocalDate from);

    @EntityGraph(attributePaths = "guest")
    Optional<Booking> findFirstByPropertyIdAndStatusAndCheckInLessThanEqualAndCheckOutAfter(
            UUID propertyId, BookingStatus status, LocalDate checkInOnOrBefore, LocalDate checkOutAfter);

    @EntityGraph(attributePaths = "guest")
    Page<Booking> findByStatusIn(Collection<BookingStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = "guest")
    Page<Booking> findByStatusAndCheckOutAfter(BookingStatus status, LocalDate date, Pageable pageable);

    @EntityGraph(attributePaths = "guest")
    Page<Booking> findByStatusAndCheckOutLessThanEqual(BookingStatus status, LocalDate date, Pageable pageable);

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

    @Query("""
            select b from Booking b
            where b.property.id = :propertyId
              and b.status in :statuses
              and b.checkOut > :from
            order by b.checkIn
            """)
    List<Booking> findEndingAfter(UUID propertyId, LocalDate from, Collection<BookingStatus> statuses);
}
