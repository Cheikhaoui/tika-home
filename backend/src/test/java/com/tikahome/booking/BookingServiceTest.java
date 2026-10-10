package com.tikahome.booking;

import com.tikahome.common.time.DateRange;
import com.tikahome.guest.GuestRepository;
import com.tikahome.pricing.PriceCalculator;
import com.tikahome.pricing.Quote;
import com.tikahome.pricing.QuoteIssue;
import com.tikahome.pricing.QuoteService;
import com.tikahome.property.Property;
import com.tikahome.property.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock PropertyRepository properties;
    @Mock GuestRepository guests;
    @Mock BookingRepository bookings;
    @Mock QuoteService quoteService;
    @InjectMocks BookingService service;

    final Property property = new Property("test", "Test", 6, new BigDecimal("100.00"),
            BigDecimal.ZERO, "EUR", 2);

    final BookingService.BookingRequest request = new BookingService.BookingRequest(property.getId(),
            LocalDate.parse("2030-01-10"), LocalDate.parse("2030-01-13"), 2,
            new BookingService.GuestDetails(" Jane Doe ", "Jane@Example.com", null, "en"), null);

    @Test
    void locksPropertyAndRejectsUnbookableStayWithoutSaving() {
        when(properties.findByIdForUpdate(property.getId())).thenReturn(Optional.of(property));
        when(quoteService.quote(eq(property), any(DateRange.class), anyInt()))
                .thenReturn(quote(Set.of(QuoteIssue.DATES_UNAVAILABLE)));

        assertThatThrownBy(() -> service.request(request))
                .isInstanceOf(BookingRejectedException.class)
                .satisfies(ex -> assertThat(((BookingRejectedException) ex).isDateConflict()).isTrue());
        verify(bookings, never()).saveAndFlush(any());
        verifyNoInteractions(guests);
    }

    @Test
    void savesRequestedBookingWithQuotedTotal() {
        when(properties.findByIdForUpdate(property.getId())).thenReturn(Optional.of(property));
        when(quoteService.quote(eq(property), any(DateRange.class), anyInt())).thenReturn(quote(Set.of()));
        when(guests.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookings.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        Booking booking = service.request(request);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REQUESTED);
        assertThat(booking.getTotalPrice()).isEqualByComparingTo("300.00");
        assertThat(booking.getGuest().getFullName()).isEqualTo("Jane Doe");
        assertThat(booking.getGuest().getEmail()).isEqualTo("jane@example.com");
    }

    private Quote quote(Set<QuoteIssue> issues) {
        var stay = new DateRange(request.checkIn(), request.checkOut());
        var pricing = new PriceCalculator.Pricing(List.of(), new BigDecimal("300.00"), BigDecimal.ZERO,
                new BigDecimal("300.00"), 2);
        return new Quote(stay, 2, "EUR", pricing, issues);
    }
}
