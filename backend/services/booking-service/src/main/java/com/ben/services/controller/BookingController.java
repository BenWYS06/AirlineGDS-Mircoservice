package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.enums.BookingStatus;
import com.ben.common_lib.exception.ResourceNotFoundException;
import com.ben.common_lib.payload.request.BookingRequest;
import com.ben.common_lib.payload.response.*;
import com.ben.services.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @RequiredRoles(UserRole.ROLE_CUSTOMER)
    @PostMapping
    public ResponseEntity<PaymentInitiateResponse> createBooking(
            @Valid @RequestBody BookingRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        PaymentInitiateResponse response = bookingService.createBooking(request, userId);
        // 201 = new booking; a duplicate request (reused) gets 202 while the first one is still
        // being processed, otherwise 200 with the existing booking's answer
        HttpStatus status = !Boolean.TRUE.equals(response.getReused()) ? HttpStatus.CREATED
                // still processing, or a payment may be landing (link expired, inside the grace): wait
                : !Boolean.TRUE.equals(response.getSuccess()) ? HttpStatus.ACCEPTED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable Long id,
            // trusted headers set by the gateway: used for the owner check (403 for someone else's booking)
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles) throws ResourceNotFoundException {
        BookingResponse response = bookingService.getBookingById(id, userId, userRoles);
        return ResponseEntity.ok(response);
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @GetMapping("/airline")
    public ResponseEntity<List<BookingResponse>> getBookingsByAirline(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long flightInstanceId,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestHeader("X-User-Id") Long userId
    ) {
        List<BookingResponse> responses = bookingService.getBookingsByAirline(
                userId,
                search,
                status,
                flightInstanceId,
                sortDirection
        );
        return ResponseEntity.ok(responses);
    }

    @RequiredRoles(UserRole.ROLE_CUSTOMER)
    @GetMapping("/user/history")
    public ResponseEntity<List<BookingResponse>> getBookingsByUser(
            @RequestHeader("X-User-Id") Long userId) {
        List<BookingResponse> responses = bookingService.getBookingsByUser(userId);
        return ResponseEntity.ok(responses);
    }

    @RequiredRoles(UserRole.ROLE_CUSTOMER)
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) throws ResourceNotFoundException {
        BookingResponse response = bookingService.cancelBooking(id, userId);
        return ResponseEntity.ok(response);
    }

    @RequiredRoles(UserRole.ROLE_CUSTOMER)
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteBooking(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) throws ResourceNotFoundException {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/flight/{flightId}")
    public ResponseEntity<Long> getBookingCountByFlight(@PathVariable Long flightId) {
        long count = bookingService.countByFlightId(flightId);
        return ResponseEntity.ok(count);
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @GetMapping("/statistics/airline")
    public ResponseEntity<BookingStatisticsResponse> getBookingStatisticsForAirline(
            @RequestParam Long airlineId) {
        BookingStatisticsResponse statistics = bookingService.getBookingStatisticsForAirline(airlineId);
        return ResponseEntity.ok(statistics);
    }
}
