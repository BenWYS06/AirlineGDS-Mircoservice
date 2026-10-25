package com.ben.common_lib.enums;

public enum SeatAvailabilityStatus {
    AVAILABLE,
    // Reserved for a booking that is waiting for payment; free again once held_until passes
    HELD,
    BOOKED,
    OCCUPIED,
    BLOCKED
}
