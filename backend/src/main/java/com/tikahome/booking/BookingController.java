package com.tikahome.booking;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(summary = "Request a booking (status REQUESTED). 409 if the dates are taken, 422 if a rule is broken.")
    @PostMapping("/api/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest body) {
        var g = body.guest();
        Booking booking = bookingService.request(new BookingService.BookingRequest(
                body.propertyId(), body.checkIn(), body.checkOut(), body.guests(),
                new BookingService.GuestDetails(g.fullName(), g.email(), g.phone(), g.language()),
                body.message()));
        return BookingResponse.from(booking);
    }

    public record CreateBookingRequest(
            @NotNull UUID propertyId,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut,
            @Min(1) int guests,
            @NotNull @Valid GuestDto guest,
            @Size(max = 2000) String message) {
    }

    public record GuestDto(
            @NotBlank @Size(max = 200) String fullName,
            @NotBlank @Email @Size(max = 320) String email,
            @Size(max = 40) String phone,
            @NotNull @Pattern(regexp = "fr|en|ar") String language) {
    }

    public record BookingResponse(UUID id, BookingStatus status, LocalDate checkIn, LocalDate checkOut,
                                  int guests, BigDecimal totalPrice, String currency) {

        static BookingResponse from(Booking b) {
            return new BookingResponse(b.getId(), b.getStatus(), b.getCheckIn(), b.getCheckOut(),
                    b.getGuests(), b.getTotalPrice(), b.getCurrency());
        }
    }
}
