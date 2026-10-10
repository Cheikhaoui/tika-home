package com.tikahome.repository;

import com.tikahome.entity.Property;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {

    /**
     * SELECT ... FOR UPDATE on the property row. Every write that changes availability
     * (booking request, blocking dates, iCal import) takes this lock first, so those writes
     * are serialized per property and the "check then insert" logic can't race.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Property p where p.id = :id")
    Optional<Property> findByIdForUpdate(UUID id);
}
