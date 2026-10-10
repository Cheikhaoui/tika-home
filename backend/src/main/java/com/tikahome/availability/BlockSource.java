package com.tikahome.availability;

public enum BlockSource {
    /** Blocked by the owner (admin dashboard, phase 5). */
    MANUAL,
    /** Imported from an external channel calendar such as Airbnb or Booking.com (phase 2). */
    ICAL
}
