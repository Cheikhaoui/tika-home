package com.tikahome.exception;

import com.tikahome.dto.admin.BlockSummary;

import java.util.List;

/** The owner tries to confirm a booking whose dates are blocked (e.g. reserved on Airbnb meanwhile). */
public class BookingConflictException extends RuntimeException {

    private final List<BlockSummary> conflicts;

    public BookingConflictException(List<BlockSummary> conflicts) {
        super("These dates are blocked by another reservation or by the owner");
        this.conflicts = List.copyOf(conflicts);
    }

    public List<BlockSummary> getConflicts() {
        return conflicts;
    }
}
