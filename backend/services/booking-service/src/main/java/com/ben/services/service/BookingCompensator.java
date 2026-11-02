package com.ben.services.service;

import com.ben.common_lib.enums.BookingStatus;
import com.ben.services.clients.SeatClient;
import com.ben.services.event.publisher.BookingEventProducer;
import com.ben.services.repository.BookingRepository;
import com.ben.services.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Saga compensation in ONE place (DESIGN.md §1): every way a booking dies (a create step failed, payment failed,
 * user cancel, recovery / expiry job, money that can't be honoured) runs the same undo:
 *   1. booking → final status, only if the guard matches       (conditional update, one winner)
 *   2. its tickets → CANCELLED                                  (same short transaction)
 *   3. outbox: booking.cancelled                                (same transaction = the release is GUARANTEED:
 *                                                                seat-service consumes it, retrying until it is up)
 *   4. release the seats right now (sync fast path)             (after commit; failure ignored, step 3 covers it)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCompensator {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final BookingEventProducer bookingEventProducer;
    private final SeatClient seatClient;
    private final TransactionTemplate transactionTemplate;

    /** Closes a live booking (CANCELLED / EXPIRED). @return false if it was already closed by someone else. */
    public boolean close(Long bookingId, BookingStatus finalStatus) {
        Boolean closed = transactionTemplate.execute(status -> {
            if (bookingRepository.closeIfAlive(bookingId, finalStatus) == 0) {
                return false;   // already closed (or paid): whoever closed it also released its seats
            }
            ticketRepository.cancelByBooking(bookingId);
            bookingEventProducer.sendBookingCancelled(bookingId, finalStatus.name());
            return true;
        });
        if (Boolean.TRUE.equals(closed)) {
            releaseNow(bookingId);
            log.info("Booking {} closed as {}", bookingId, finalStatus);
        }
        return Boolean.TRUE.equals(closed);
    }

    /**
     * Money arrived but the booking can't be honoured (seats lost, or the user had cancelled):
     * REFUND_REQUIRED for the admin, tickets cancelled, any seat it still holds released.
     */
    public boolean refundRequired(Long bookingId, Long paymentId) {
        Boolean flagged = transactionTemplate.execute(status -> {
            if (bookingRepository.markRefundRequired(bookingId, paymentId) == 0) {
                return false;   // duplicate event: already REFUND_REQUIRED or CONFIRMED
            }
            ticketRepository.cancelByBooking(bookingId);
            bookingEventProducer.sendBookingCancelled(bookingId, BookingStatus.REFUND_REQUIRED.name());
            return true;
        });
        if (Boolean.TRUE.equals(flagged)) {
            releaseNow(bookingId);
        }
        return Boolean.TRUE.equals(flagged);
    }

    // Fast path so other users get the seats immediately. Release is idempotent (only this booking's HELD
    // seats), so running it here AND in seat-service's booking.cancelled consumer is harmless.
    private void releaseNow(Long bookingId) {
        try {
            seatClient.releaseSeats(bookingId);
        } catch (RuntimeException e) {
            log.warn("Fast release of booking {} failed, the booking.cancelled event will release it: {}",
                    bookingId, e.getMessage());
        }
    }
}
