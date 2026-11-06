package com.ben.common_lib.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * booking-service → seat-service (topic booking.cancelled): the booking is closed, release its HELD seats.
 * Written to booking's outbox in the same transaction that closes the booking, so the release is guaranteed
 * even if seat-service is down at that moment (the seat consumer retries until it is back).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCancelledEvent {
    private Long bookingId;
    // CANCELLED, EXPIRED or REFUND_REQUIRED: why the booking was closed (for logs)
    private String finalStatus;
}
