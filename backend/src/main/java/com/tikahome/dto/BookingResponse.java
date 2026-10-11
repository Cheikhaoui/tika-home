package com.tikahome.dto;

import com.tikahome.entity.Booking;
import com.tikahome.entity.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BookingResponse(UUID id, String reference, BookingStatus status, LocalDate checkIn,
                              LocalDate checkOut, int guests, BigDecimal totalPrice, String currency) {

    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.getId(), b.getReference(), b.getStatus(), b.getCheckIn(), b.getCheckOut(),
                b.getGuests(), b.getTotalPrice(), b.getCurrency());
    }
}
