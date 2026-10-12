package com.tikahome.exception;

import com.tikahome.entity.BookingStatus;

/** E.g. confirming a booking that was already declined. Mapped to 409. */
public class InvalidBookingStateException extends RuntimeException {

    public InvalidBookingStateException(String reference, BookingStatus actual, BookingStatus expected) {
        super("Booking " + reference + " is " + actual + ", it must be " + expected + " for this action");
    }
}
