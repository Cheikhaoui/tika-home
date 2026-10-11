package com.tikahome.service;

import com.tikahome.dto.CreateBookingRequest;
import com.tikahome.entity.Booking;
import com.tikahome.entity.Guest;
import com.tikahome.entity.Property;
import com.tikahome.exception.BookingRejectedException;
import com.tikahome.exception.NotFoundException;
import com.tikahome.model.DateRange;
import com.tikahome.repository.BookingRepository;
import com.tikahome.repository.GuestRepository;
import com.tikahome.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class BookingService {

    private final PropertyRepository properties;
    private final GuestRepository guests;
    private final BookingRepository bookings;
    private final QuoteService quoteService;
    private final NotificationService notifications;

    public BookingService(PropertyRepository properties, GuestRepository guests,
                          BookingRepository bookings, QuoteService quoteService,
                          NotificationService notifications) {
        this.properties = properties;
        this.guests = guests;
        this.bookings = bookings;
        this.quoteService = quoteService;
        this.notifications = notifications;
    }

    /**
     * Creates a REQUESTED booking. Double bookings are prevented in two layers:
     * <ol>
     *   <li>The property row is locked (SELECT ... FOR UPDATE) for the whole transaction, so concurrent
     *       requests for this house run one after another and each sees the previous one's booking.</li>
     *   <li>If anything still slips through, the {@code booking_no_overlap} exclusion constraint rejects
     *       the insert; {@code saveAndFlush} makes that happen here, inside this method.</li>
     * </ol>
     */
    @Transactional
    public Booking request(CreateBookingRequest request) {
        Property property = properties.findByIdForUpdate(request.propertyId())
                .orElseThrow(() -> new NotFoundException("Property " + request.propertyId() + " not found"));

        var stay = new DateRange(request.checkIn(), request.checkOut());
        var quote = quoteService.quote(property, stay, request.guests()); // re-checked under the lock
        if (!quote.bookable()) {
            throw new BookingRejectedException(quote.issues());
        }

        var g = request.guest();
        Guest guest = guests.save(new Guest(g.fullName().strip(), g.email().strip().toLowerCase(),
                g.phone(), g.language()));
        Booking booking = bookings.saveAndFlush(Booking.request(property, guest, stay.start(), stay.end(),
                request.guests(), quote.total(), request.message()));
        notifications.bookingRequested(booking); // queued in this transaction: no booking, no email
        return booking;
    }
}
