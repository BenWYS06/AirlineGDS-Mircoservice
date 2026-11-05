package com.ben.services.clients;

import com.ben.common_lib.payload.request.PaymentInitiateRequest;
import com.ben.common_lib.payload.response.PaymentDTO;
import com.ben.common_lib.payload.response.PaymentInitiateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class PaymentClientFallback implements PaymentClient {

    // Never fake a payment link: 503 → create compensates (booking CANCELLED, seats released), user retries
    @Override
    public PaymentInitiateResponse initiatePayment(PaymentInitiateRequest request, Long userId) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Payment service is temporarily unavailable. Please retry.");
    }

    @Override
    public PaymentDTO getPaymentByBookingId(Long bookingId) {
        return null;
    }

    @Override
    public Map<Long, PaymentDTO> getPaymentsByBookingIds(List<Long> bookingIds) {
        return Collections.emptyMap();
    }
}
