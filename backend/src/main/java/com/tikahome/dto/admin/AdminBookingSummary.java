package com.tikahome.dto.admin;

import com.tikahome.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One row in the admin bookings list. */
public record AdminBookingSummary(UUID id, String reference, BookingStatus status, LocalDate checkIn,
                                  LocalDate checkOut, long nights, int guests, BigDecimal totalPrice,
                                  String currency, String guestName, String guestEmail, Instant createdAt) {
}
