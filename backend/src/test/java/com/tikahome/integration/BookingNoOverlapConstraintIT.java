package com.tikahome.integration;

import com.tikahome.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the database alone rejects overlapping bookings: these inserts bypass all Java logic.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingNoOverlapConstraintIT {

    @Autowired
    JdbcTemplate jdbc;

    UUID propertyId;
    UUID guestId;

    @BeforeEach
    void setUp() {
        propertyId = UUID.randomUUID();
        guestId = UUID.randomUUID();
        jdbc.update("""
                insert into property (id, slug, name, max_guests, base_nightly_price, currency)
                values (?, ?, 'Test house', 6, 100, 'EUR')
                """, propertyId, "test-" + propertyId);
        jdbc.update("insert into guest (id, full_name, email) values (?, 'Test Guest', 'guest@example.com')", guestId);
    }

    @Test
    void rejectsOverlappingActiveBooking() {
        insertBooking("2027-03-10", "2027-03-15", "CONFIRMED");

        assertThatThrownBy(() -> insertBooking("2027-03-14", "2027-03-18", "REQUESTED"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("booking_no_overlap");
    }

    @Test
    void allowsBackToBackStays() {
        insertBooking("2027-04-10", "2027-04-15", "CONFIRMED");

        assertThatCode(() -> insertBooking("2027-04-15", "2027-04-20", "REQUESTED")).doesNotThrowAnyException();
    }

    @Test
    void declinedBookingDoesNotHoldDates() {
        insertBooking("2027-05-10", "2027-05-15", "DECLINED");

        assertThatCode(() -> insertBooking("2027-05-10", "2027-05-15", "REQUESTED")).doesNotThrowAnyException();
    }

    @Test
    void sameDatesOnAnotherPropertyAreFine() {
        insertBooking("2027-06-10", "2027-06-15", "CONFIRMED");
        propertyId = UUID.randomUUID();
        jdbc.update("""
                insert into property (id, slug, name, max_guests, base_nightly_price, currency)
                values (?, ?, 'Other house', 4, 80, 'EUR')
                """, propertyId, "test-" + propertyId);

        assertThatCode(() -> insertBooking("2027-06-10", "2027-06-15", "CONFIRMED")).doesNotThrowAnyException();
    }

    private void insertBooking(String checkIn, String checkOut, String status) {
        jdbc.update("""
                insert into booking (id, property_id, guest_id, check_in, check_out, guests, status, total_price, currency)
                values (?, ?, ?, ?, ?, 2, ?, 500, 'EUR')
                """, UUID.randomUUID(), propertyId, guestId, LocalDate.parse(checkIn), LocalDate.parse(checkOut), status);
    }
}
