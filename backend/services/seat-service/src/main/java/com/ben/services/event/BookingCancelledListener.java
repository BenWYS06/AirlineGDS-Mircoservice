package com.ben.services.event;

import com.ben.common_lib.event.BookingCancelledEvent;
import com.ben.services.service.SeatInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Guaranteed half of the saga compensation (DESIGN.md §1): booking-service writes booking.cancelled to its
 * outbox in the same transaction that closes the booking. If this release fails (DB down), the error handler
 * retries with backoff and finally parks the message in booking.cancelled.DLT, so it is never silently lost.
 * Idempotent: releaseSeats only frees seats still HELD by this booking, so running it after the sync fast path
 * (or twice on redelivery) changes nothing, and it never touches BOOKED seats.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCancelledListener {

    private final SeatInstanceService seatInstanceService;

    @KafkaListener(topics = "booking.cancelled", groupId = "seat-service-group")
    public void handleBookingCancelled(BookingCancelledEvent event) {
        int released = seatInstanceService.releaseSeats(event.getBookingId());
        log.info("booking.cancelled for booking {} ({}): {} seat(s) released",
                event.getBookingId(), event.getFinalStatus(), released);
    }
}
