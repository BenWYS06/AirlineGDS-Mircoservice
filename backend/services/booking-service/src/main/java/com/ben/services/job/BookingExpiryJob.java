package com.ben.services.job;

import com.ben.common_lib.enums.BookingStatus;
import com.ben.services.model.Booking;
import com.ben.services.repository.BookingRepository;
import com.ben.services.service.BookingCompensator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Saga recovery from DB state (DESIGN.md §1, §9). Runs on startup and then every minute:
 * - PENDING older than 1 min      → booking-service died mid-create: CANCELLED + release (always compensate,
 *                                   never resume: the user's request is already gone)
 * - AWAITING_PAYMENT past link expiry + grace → the user never paid: EXPIRED + release
 * Safe with several instances: BookingCompensator.close is a conditional update, only one run wins per booking.
 * Payment rows are never touched, so a late IPN can still be recorded (late money, PaymentEventListener).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpiryJob {

    // A create takes far less than this (timeouts × retries ≈ 30s), so a live create is never cancelled
    static final Duration STALE_PENDING = Duration.ofMinutes(1);

    // Time after the VNPay link expires for a slow IPN to still arrive and confirm normally
    public static final Duration PAYMENT_GRACE = Duration.ofMinutes(2);

    private final BookingRepository bookingRepository;
    private final BookingCompensator bookingCompensator;

    @Scheduled(initialDelay = 10_000, fixedDelay = 60_000)
    public void closeStuckBookings() {
        // Stale PENDING: the orchestrator crashed between INSERT and AWAITING_PAYMENT
        for (Booking b : bookingRepository.findTop100ByStatusAndBookingDateBefore(
                BookingStatus.PENDING, LocalDateTime.now().minus(STALE_PENDING))) {
            close(b, BookingStatus.CANCELLED);
        }

        // Unpaid: the link (+ grace) has run out
        for (Booking b : bookingRepository.findTop100ByStatusAndPaymentExpiresAtBefore(
                BookingStatus.AWAITING_PAYMENT, Instant.now().minus(PAYMENT_GRACE))) {
            close(b, BookingStatus.EXPIRED);
        }
    }

    private void close(Booking booking, BookingStatus finalStatus) {
        try {
            bookingCompensator.close(booking.getId(), finalStatus);
        } catch (RuntimeException e) {
            // one bad booking must not stop the others; it is picked up again on the next run
            log.error("Could not close booking {} as {}: {}", booking.getId(), finalStatus, e.getMessage());
        }
    }
}
