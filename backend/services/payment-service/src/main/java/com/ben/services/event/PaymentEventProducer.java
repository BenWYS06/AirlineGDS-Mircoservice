package com.ben.services.event;

import com.ben.common_lib.event.PaymentCompletedEvent;
import com.ben.common_lib.event.PaymentFailedEvent;
import com.ben.services.model.Payment;
import com.ben.services.outbox.OutboxWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Builds the payment result events and puts them in the outbox (same transaction as the IPN that set the
 * payment SUCCESS / FAILED). OutboxRelay sends them to Kafka with key = bookingId.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final OutboxWriter outboxWriter;

    public void sendPaymentCompleted(Payment payment) {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .providerPaymentId(payment.getProviderPaymentId())
                .paidAt(payment.getPaidAt())
                .build();

        outboxWriter.add("payment.completed", payment.getBookingId(), event);
        log.info("Outbox: PaymentCompletedEvent for payment ID: {}, booking ID: {}",
                payment.getId(), payment.getBookingId());
    }

    public void sendPaymentFailed(Payment payment) {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .failureReason(payment.getFailureReason())
                .failedAt(LocalDateTime.now())
                .build();

        outboxWriter.add("payment.failed", payment.getBookingId(), event);
        log.warn("Outbox: PaymentFailedEvent for payment ID: {} - Reason: {}",
                payment.getId(), payment.getFailureReason());
    }
}
