package com.tikahome.dto.admin;

import com.tikahome.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Everything the owner needs to decide on a booking, incl. blocks overlapping its dates. */
public record AdminBookingDetail(UUID id, String reference, BookingStatus status, LocalDate checkIn,
                                 LocalDate checkOut, long nights, int guests, BigDecimal totalPrice,
                                 String currency, String message, Instant createdAt, Instant updatedAt,
                                 GuestInfo guest, List<BlockSummary> conflicts) {

    public record GuestInfo(String fullName, String email, String phone, String language) {
    }
}
