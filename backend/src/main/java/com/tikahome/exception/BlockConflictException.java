package com.tikahome.exception;

import com.tikahome.dto.admin.AdminBookingSummary;

import java.util.List;

/** The owner tries to block dates that a booking already holds. */
public class BlockConflictException extends RuntimeException {

    private final List<AdminBookingSummary> bookings;

    public BlockConflictException(List<AdminBookingSummary> bookings) {
        super("A booking holds some of these dates: decline it first or choose other dates");
        this.bookings = List.copyOf(bookings);
    }

    public List<AdminBookingSummary> getBookings() {
        return bookings;
    }
}
