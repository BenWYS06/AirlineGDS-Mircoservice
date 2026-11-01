package com.ben.services.controller;

import com.ben.common_lib.payload.request.SeatHoldRequest;
import com.ben.common_lib.payload.response.SeatHoldResponse;
import com.ben.services.service.SeatInstanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Service-to-service API for booking-service (Feign). Under /internal, which the api-gateway
 * does not route, so users cannot hold or release seats directly.
 */
@RestController
@RequestMapping("/internal/seat-instances")
@RequiredArgsConstructor
public class InternalSeatHoldController {

    private final SeatInstanceService seatInstanceService;

    // 200 + held seats, or 409 if any seat is already taken (nothing is held then)
    @PostMapping("/hold")
    public ResponseEntity<SeatHoldResponse> holdSeats(@Valid @RequestBody SeatHoldRequest request) {
        return ResponseEntity.ok(seatInstanceService.holdSeats(request));
    }

    // Called by booking (the saga orchestrator) after payment: 200 + count, or 409 if seats were lost.
    // Safe to call more than once: seats already BOOKED by this booking count as confirmed.
    @PostMapping("/bookings/{bookingId}/confirm")
    public ResponseEntity<Integer> confirmSeats(@PathVariable Long bookingId,
                                                @RequestBody List<Long> seatInstanceIds) {
        return ResponseEntity.ok(seatInstanceService.confirmSeats(bookingId, seatInstanceIds));
    }

    // Safe to call more than once: only seats still HELD by this booking are released
    @PostMapping("/bookings/{bookingId}/release")
    public ResponseEntity<Integer> releaseSeats(@PathVariable Long bookingId) {
        return ResponseEntity.ok(seatInstanceService.releaseSeats(bookingId));
    }
}
