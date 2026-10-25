package com.ben.common_lib.payload.response;

import com.ben.common_lib.enums.BookingStatus;
import com.ben.common_lib.enums.PaymentGateway;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiateResponse {

    private Long paymentId;
    private PaymentGateway gateway;
    private String transactionId;

    private Double amount;
    private String currency;
    private String description;

    // Frontend should redirect user to this URL for payment
    private String checkoutUrl;

    // When the VNPay link stops accepting payments (set by payment-service)
    private Instant expiresAt;

    // Set by booking-service on POST /bookings: which booking this answer is about
    private Long bookingId;
    private BookingStatus bookingStatus;

    // true = a duplicate request: the answer comes from the booking already created (idempotent create)
    private Boolean reused;

    private String message;
    private Boolean success;
}
