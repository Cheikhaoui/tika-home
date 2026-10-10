package com.tikahome.booking;

import com.tikahome.common.time.DateRange;
import com.tikahome.common.web.NotFoundException;
import com.tikahome.guest.Guest;
import com.tikahome.guest.GuestRepository;
import com.tikahome.pricing.QuoteService;
import com.tikahome.property.Property;
import com.tikahome.property.PropertyRepository;
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

    public BookingService(PropertyRepository properties, GuestRepository guests,
                          BookingRepository bookings, QuoteService quoteService) {
        this.properties = properties;
        this.guests = guests;
        this.bookings = bookings;
        this.quoteService = quoteService;
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
    public Booking request(BookingRequest request) {
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
        return bookings.saveAndFlush(Booking.request(property, guest, stay.start(), stay.end(),
                request.guests(), quote.total(), request.message()));
    }

    public record BookingRequest(UUID propertyId, LocalDate checkIn, LocalDate checkOut,
                                 int guests, GuestDetails guest, String message) {
    }

    public record GuestDetails(String fullName, String email, String phone, String language) {
    }
}
