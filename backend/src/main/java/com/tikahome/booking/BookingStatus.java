package com.tikahome.booking;

import java.util.EnumSet;
import java.util.Set;

public enum BookingStatus {
    REQUESTED,
    CONFIRMED,
    DECLINED,
    CANCELLED;

    /** Statuses that hold the dates. Must match the WHERE clause of the exclusion constraint (V2). */
    public static final Set<BookingStatus> ACTIVE = EnumSet.of(REQUESTED, CONFIRMED);
}
