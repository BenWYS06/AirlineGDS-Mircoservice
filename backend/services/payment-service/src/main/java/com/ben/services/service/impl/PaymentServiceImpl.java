package com.ben.services.service.impl;

import com.ben.common_lib.enums.PaymentStatus;
import com.ben.common_lib.exception.PaymentException;
import com.ben.common_lib.payload.request.PaymentInitiateRequest;
import com.ben.common_lib.payload.response.PaymentDTO;
import com.ben.common_lib.payload.response.PaymentInitiateResponse;
import com.ben.common_lib.payload.response.VnPayReturnResponse;
import com.ben.services.event.PaymentEventProducer;
import com.ben.services.mapper.PaymentMapper;
import com.ben.services.model.Payment;
import com.ben.services.repository.PaymentRepository;
import com.ben.services.service.PaymentService;
import com.ben.services.service.gateway.VnPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    // VNPay requires vnp_IpAddr; the real client IP is not forwarded through booking-service yet
    private static final String DEFAULT_CLIENT_IP = "127.0.0.1";

    // vnp_ResponseCode when the user clicks "Cancel" on the VNPay payment page
    private static final String VNPAY_USER_CANCELLED = "24";

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final VnPayService vnPayService;
    private final TransactionTemplate transactionTemplate;

    /*
     * Idempotent: one payment per booking. Not @Transactional on purpose: the create runs in its own short
     * transaction, so when it loses the duplicate-key race we can still re-read the winner in a new one.
     */
    @Override
    public PaymentInitiateResponse initiatePayment(PaymentInitiateRequest request) throws PaymentException {
        log.info("Initiating payment for user: {} with gateway: {}",
                request.getUserId(), request.getGateway());
        try {
            return transactionTemplate.execute(status -> findOrCreatePayment(request));
        } catch (DataIntegrityViolationException duplicate) {
            // Race: two calls for the same booking both saw "no payment yet" and both inserted.
            // UNIQUE booking_id let exactly one win; the loser returns the winner's payment (same link).
            log.info("Payment for booking {} was created by a concurrent call, returning it", request.getBookingId());
            PaymentInitiateResponse existing = transactionTemplate.execute(status ->
                    paymentRepository.findByBookingId(request.getBookingId())
                            .map(p -> toInitiateResponse(p, request.getDescription()))
                            .orElse(null));
            if (existing == null) {
                throw new PaymentException("Failed to initiate payment: " + duplicate.getMessage());
            }
            return existing;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error initiating payment: {}", e.getMessage(), e);
            throw new PaymentException("Failed to initiate payment: " + e.getMessage());
        }
    }

    private PaymentInitiateResponse findOrCreatePayment(PaymentInitiateRequest request) {
        // A repeated call for the same booking (booking retried after a timeout) returns the payment
        // already created instead of opening a second one
        Payment existing = paymentRepository.findByBookingId(request.getBookingId()).orElse(null);
        if (existing != null) {
            if (existing.getStatus() != PaymentStatus.PENDING) {
                // Bug guard: booking only asks for a link while it has none, so this can't happen in the flow
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment for booking "
                        + request.getBookingId() + " is already " + existing.getStatus());
            }
            log.info("Payment {} already exists for booking {}, returning it",
                    existing.getId(), request.getBookingId());
            return toInitiateResponse(existing, request.getDescription());
        }

        // Create payment entity
        Payment payment = Payment.builder()
                .userId(request.getUserId())
                .bookingId(request.getBookingId())
                .amount(request.getAmount())
                .provider(request.getGateway())
                .status(PaymentStatus.PENDING)
                .transactionId(generateTransactionId())
                .build();

        payment = paymentRepository.save(payment);   // INSERT now: a concurrent duplicate fails here
        // Frontend redirects the user to this VNPay page; stored so a repeat returns the same link
        payment.setCheckoutUrl(vnPayService.createPaymentUrl(payment, DEFAULT_CLIENT_IP));

        log.info("Payment initiated successfully with ID: {}", payment.getId());
        return toInitiateResponse(payment, request.getDescription());
    }

    @Override
    @Transactional
    public Map<String, String> handleVnPayIpn(Map<String, String> vnpParams) {
        log.info("VNPay IPN received for txnRef={}", vnpParams.get("vnp_TxnRef"));
        return applyVnPayResult(vnpParams);
    }

    @Override
    @Transactional
    public VnPayReturnResponse handleVnPayReturn(Map<String, String> vnpParams) {
        boolean validSignature = vnPayService.isValidSignature(vnpParams);
        String responseCode = vnpParams.get("vnp_ResponseCode");

        // A cancel on the VNPay page usually comes with NO IPN: this signed browser redirect is the only signal.
        // Record it now with the same handler as the IPN, so the booking is cancelled and its seats freed at once.
        // Success / failure stay with the IPN (source of truth); for them this endpoint is display only.
        if (validSignature && VNPAY_USER_CANCELLED.equals(responseCode)) {
            applyVnPayResult(vnpParams);
        }

        boolean success = validSignature && "00".equals(responseCode);
        return VnPayReturnResponse.builder()
                .validSignature(validSignature)
                .success(success)
                .transactionId(vnpParams.get("vnp_TxnRef"))
                .responseCode(responseCode)
                .message(!validSignature ? "Invalid signature"
                        : success ? "Payment successful" : "Payment failed or cancelled")
                .build();
    }

    /**
     * The one VNPay result handler (IPN for every result, return redirect for a cancel). Runs inside the
     * caller's transaction and answers with VNPay's RspCode.
     */
    private Map<String, String> applyVnPayResult(Map<String, String> vnpParams) {
        String txnRef = vnpParams.get("vnp_TxnRef");
        try {
            // 1. Only VNPay knows our hash secret, so a valid signature proves the call is genuine
            if (!vnPayService.isValidSignature(vnpParams)) {
                log.warn("VNPay IPN rejected: invalid signature, txnRef={}", txnRef);
                return ipnResponse("97", "Invalid signature");
            }

            // Row lock: concurrent IPNs for the same payment are processed one after the other
            Payment payment = paymentRepository.findByTransactionIdForUpdate(txnRef).orElse(null);
            if (payment == null) {
                return ipnResponse("01", "Order not found");
            }

            // 2. The amount VNPay charged must match what we asked for
            long expectedAmount = vnPayService.toVnpAmount(payment.getAmount());
            if (!String.valueOf(expectedAmount).equals(vnpParams.get("vnp_Amount"))) {
                log.warn("VNPay IPN rejected: amount mismatch, txnRef={}", txnRef);
                return ipnResponse("04", "Invalid amount");
            }

            // 3. VNPay may call the IPN more than once: process a payment only the first time
            if (payment.getStatus() != PaymentStatus.PENDING) {
                log.info("VNPay IPN ignored: payment {} already {}", payment.getId(), payment.getStatus());
                return ipnResponse("02", "Order already confirmed");
            }

            // 4. Record the result + its event (outbox row) in THIS transaction: both commit or neither does
            boolean paid = "00".equals(vnpParams.get("vnp_ResponseCode"))
                    && "00".equals(vnpParams.get("vnp_TransactionStatus"));

            if (paid) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setProviderPaymentId(vnpParams.get("vnp_TransactionNo"));
                payment.setMethod(vnpParams.get("vnp_CardType"));
                payment.setPaidAt(LocalDateTime.now());
                payment = paymentRepository.save(payment);
                paymentEventProducer.sendPaymentCompleted(payment);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("VNPay response code " + vnpParams.get("vnp_ResponseCode"));
                payment = paymentRepository.save(payment);
                paymentEventProducer.sendPaymentFailed(payment);
            }

            return ipnResponse("00", "Confirm Success");

        } catch (Exception e) {
            log.error("VNPay result failed for txnRef={}: {}", txnRef, e.getMessage(), e);
            // We swallow the exception to answer VNPay, so Spring would COMMIT whatever was saved.
            // Roll back instead: the payment stays PENDING and VNPay's retry processes it again.
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ipnResponse("99", "Unknown error");
        }
    }

    private PaymentInitiateResponse toInitiateResponse(Payment payment, String description) {
        return PaymentInitiateResponse.builder()
                .paymentId(payment.getId())
                .gateway(payment.getProvider())
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .currency("VND")
                .description(description)
                .checkoutUrl(payment.getCheckoutUrl())
                .expiresAt(vnPayService.expiresAt(payment))   // booking stores it as payment_expires_at
                .success(true)
                .message("Payment initiated successfully")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDTO> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable)
                .map(PaymentMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, PaymentDTO> getPaymentsByBookingIds(List<Long> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) return Map.of();
        return paymentRepository.findByBookingIdIn(bookingIds).stream()
                .collect(Collectors.toMap(Payment::getBookingId, PaymentMapper::toDTO));
    }

    // Also used as vnp_TxnRef, which VNPay expects to be alphanumeric
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() +
                UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private Map<String, String> ipnResponse(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }
}
