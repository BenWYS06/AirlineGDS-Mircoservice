package com.ben.services.service;

import com.ben.common_lib.exception.PaymentException;
import com.ben.common_lib.payload.request.PaymentInitiateRequest;
import com.ben.common_lib.payload.response.PaymentDTO;
import com.ben.common_lib.payload.response.PaymentInitiateResponse;
import com.ben.common_lib.payload.response.VnPayReturnResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface PaymentService {

    PaymentInitiateResponse initiatePayment(PaymentInitiateRequest request) throws PaymentException;

    /** VNPay server-to-server callback: records every result (SUCCESS / FAILED), the source of truth. */
    Map<String, String> handleVnPayIpn(Map<String, String> vnpParams);

    /** Browser redirect from VNPay: records a cancel (code 24, usually no IPN); otherwise display only. */
    VnPayReturnResponse handleVnPayReturn(Map<String, String> vnpParams);

    Page<PaymentDTO> getAllPayments(Pageable pageable);

    Map<Long, PaymentDTO> getPaymentsByBookingIds(List<Long> bookingIds);
}
