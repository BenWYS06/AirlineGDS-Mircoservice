package com.ben.common_lib.enums;

// The booking status is the saga state (DESIGN.md §1).
public enum BookingStatus {
    PENDING,            // seats being held / payment link being created
    AWAITING_PAYMENT,   // VNPay link ready, waiting for the IPN
    CONFIRMED,          // paid + seats BOOKED
    CANCELLED,          // a step failed, payment failed or the user cancelled
    EXPIRED,            // user never paid (recovery/expiry job)
    REFUND_REQUIRED,    // money arrived but the booking was closed or the seats were lost
    COMPLETED           // flight flown (outside the saga)
}
