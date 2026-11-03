package com.ben.services.event.listener;

import com.ben.common_lib.dto.UserDTO;
import com.ben.common_lib.enums.BookingStatus;
import com.ben.common_lib.event.PaymentCompletedEvent;
import com.ben.common_lib.event.PaymentFailedEvent;
import com.ben.common_lib.payload.response.FareResponse;
import com.ben.common_lib.payload.response.FlightInstanceResponse;
import com.ben.services.clients.FlightClient;
import com.ben.services.clients.PricingClient;
import com.ben.services.clients.SeatClient;
import com.ben.services.clients.UserClient;
import com.ben.services.event.publisher.BookingEventProducer;
import com.ben.services.model.Booking;
import com.ben.services.repository.BookingRepository;
import com.ben.services.repository.TicketRepository;
import com.ben.services.service.BookingCompensator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Finishes the saga after payment (DESIGN.md §7). booking-service is the orchestrator: it confirms the seats
 * itself (sync call) and decides the final status; seat-service no longer reacts to booking.confirmed.
 * Not @Transactional: remote calls run outside any transaction; each status change is its own short,
 * conditional update, so a redelivered event changes nothing (idempotent consumer).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final BookingEventProducer bookingEventProducer;
    private final BookingCompensator bookingCompensator;
    private final TransactionTemplate transactionTemplate;
    private final FlightClient flightClient;
    private final PricingClient pricingClient;
    private final UserClient userClient;
    private final SeatClient seatClient;

    @KafkaListener(topics = "payment.completed", groupId = "booking-service-group")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        Long bookingId = event.getBookingId();
        log.info("Received PaymentCompletedEvent for bookingId={}", bookingId);

        // Plain read (tickets fetched for the seat ids); decides which path this payment takes
        Booking booking = bookingRepository.findByIdWithDetails(bookingId).orElse(null);
        if (booking == null) {
            log.error("Booking not found for id={}", bookingId);
            return;
        }
        switch (booking.getStatus()) {
            case CONFIRMED, REFUND_REQUIRED -> {
                log.info("Ignoring payment.completed for booking {}: already handled", bookingId);
                return;
            }
            case CANCELLED -> {
                // The user cancelled while paying in another tab: don't revive it, refund the money (late money)
                markRefundRequired(bookingId, event.getPaymentId(), "booking was cancelled by the user");
                return;
            }
            case AWAITING_PAYMENT, EXPIRED -> { /* normal path, or late money after the expiry job: continue */ }
            default -> {
                log.warn("Ignoring payment.completed for booking {} in status {}", bookingId, booking.getStatus());
                return;
            }
        }

        // Enrichment for the notification first (remote calls, non-fatal), so the gap between confirming the
        // seats and confirming the booking stays as short as possible
        FlightInstanceResponse flightInstance = fetchFlightInstance(booking.getFlightInstanceId());
        FareResponse fareResponse = fetchFare(booking.getFareId());
        UserDTO userDTO = fetchUser(booking.getUserId());

        // 1. Seats FIRST, then the booking: the booking never says CONFIRMED while its seats aren't BOOKED.
        //    Confirm also takes seats that are still free (late money after expiry). Idempotent on a repeat.
        try {
            seatClient.confirmSeats(bookingId, booking.getSeatInstanceIds());
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() != HttpStatus.CONFLICT) {
                throw e;   // seat-service down: the money is paid, never cancel → throw, Kafka redelivers the event
            }
            // 409: another booking has one of the seats. Nothing was booked (all-or-nothing) → refund
            markRefundRequired(bookingId, event.getPaymentId(), "seats were lost");
            return;
        }

        // 2. One transaction: AWAITING_PAYMENT / EXPIRED → CONFIRMED (conditional) + booking.confirmed into the
        //    outbox, so "confirmed" and "the e-ticket email will be sent" commit together.
        //    Late money: the job had cancelled the tickets of the EXPIRED booking, so they are made valid again.
        Boolean confirmed = transactionTemplate.execute(status -> {
            if (bookingRepository.markConfirmed(bookingId, event.getPaymentId()) == 0) {
                return false;   // a copy of this event got there first, or the user cancelled meanwhile
            }
            ticketRepository.restoreByBooking(bookingId);
            Booking confirmedBooking = bookingRepository.findByIdWithDetails(bookingId).orElseThrow();
            bookingEventProducer.sendBookingConfirmed(confirmedBooking, event, flightInstance, fareResponse, userDTO);
            return true;
        });
        if (!Boolean.TRUE.equals(confirmed)) {
            // 0 rows: either a duplicate (already CONFIRMED / REFUND_REQUIRED → the guard below changes nothing)
            // or the user cancelled while we were confirming (CANCELLED → the money must be refunded, DESIGN §9)
            markRefundRequired(bookingId, event.getPaymentId(), "booking was cancelled while confirming");
            return;
        }
        log.info("Booking {} confirmed after payment {}", bookingId, event.getPaymentId());
    }

    @KafkaListener(topics = "payment.failed", groupId = "booking-service-group")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent for bookingId={}", event.getBookingId());

        // Same compensation as every other failure path. Only a live booking is closed, so a redelivered
        // event (already CANCELLED) changes nothing and seats are not released twice.
        if (bookingCompensator.close(event.getBookingId(), BookingStatus.CANCELLED)) {
            log.warn("Booking {} cancelled due to payment failure: {}",
                    event.getBookingId(), event.getFailureReason());
        } else {
            log.info("Ignoring payment.failed for booking {}: not alive (duplicate or closed)", event.getBookingId());
        }
    }

    // Money arrived but the booking can't be honoured: flag it for a manual refund by the admin
    private void markRefundRequired(Long bookingId, Long paymentId, String reason) {
        if (bookingCompensator.refundRequired(bookingId, paymentId)) {
            log.warn("Booking {} needs a refund: payment {} arrived but {}", bookingId, paymentId, reason);
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private FlightInstanceResponse fetchFlightInstance(Long flightInstanceId) {
        if (flightInstanceId == null) return null;
        try {
            return flightClient.getFlightInstanceResponse(flightInstanceId);
        } catch (Exception e) {
            log.warn("Could not fetch FlightInstance id={} for notification enrichment: {}",
                    flightInstanceId, e.getMessage());
            return null;
        }
    }

    private FareResponse fetchFare(Long fareId) {
        if (fareId == null) return null;
        try {
            return pricingClient.getFareById(fareId);
        } catch (Exception e) {
            log.warn("Could not fetch Fare id={} for notification enrichment: {}",
                    fareId, e.getMessage());
            return null;
        }
    }

    private UserDTO fetchUser(Long userId) {
        if (userId == null) return null;
        try {
            return userClient.getUserById(userId);
        } catch (Exception e) {
            log.warn("Could not fetch User id={} for notification enrichment: {}",
                    userId, e.getMessage());
            return null;
        }
    }
}
