package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.exception.PaymentException;
import com.ben.common_lib.payload.request.PaymentInitiateRequest;
import com.ben.common_lib.payload.response.PaymentDTO;
import com.ben.common_lib.payload.response.PaymentInitiateResponse;
import com.ben.common_lib.payload.response.VnPayReturnResponse;
import com.ben.services.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequest request,
            @RequestHeader("X-User-Id") Long userId) throws PaymentException {


            PaymentInitiateResponse response = paymentService
                    .initiatePayment(request);
            return ResponseEntity.ok(response);


    }

    // Called by VNPay's servers (no user JWT; the gateway lets this path through unauthenticated).
    // VNPay reads only the JSON body, so always answer 200 with an RspCode.
    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnPayIpn(@RequestParam Map<String, String> vnpParams) {
        return ResponseEntity.ok(paymentService.handleVnPayIpn(vnpParams));
    }

    // Called by the FE after VNPay redirects the browser back. POST: it records a cancel (code 24).
    @PostMapping("/vnpay/return")
    public ResponseEntity<VnPayReturnResponse> vnPayReturn(@RequestParam Map<String, String> vnpParams) {
        return ResponseEntity.ok(paymentService.handleVnPayReturn(vnpParams));
    }

    @PostMapping("/batch/bookings")
    public ResponseEntity<Map<Long, PaymentDTO>> getPaymentsByBookingIds(@RequestBody List<Long> bookingIds) {
        return ResponseEntity.ok(paymentService
                .getPaymentsByBookingIds(bookingIds));
    }

    @RequiredRoles({})
    @GetMapping
    public ResponseEntity<Page<PaymentDTO>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestHeader("X-User-Id") Long userId) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<PaymentDTO> payments = paymentService.getAllPayments(pageable);
        return ResponseEntity.ok(payments);
    }





    private record ErrorResponse(String message) {}
}
