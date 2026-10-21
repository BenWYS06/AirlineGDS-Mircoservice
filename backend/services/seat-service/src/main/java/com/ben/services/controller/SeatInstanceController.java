package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.enums.SeatAvailabilityStatus;
import com.ben.common_lib.payload.request.SeatInstanceRequest;
import com.ben.common_lib.payload.response.SeatInstanceResponse;
import com.ben.services.service.SeatInstanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/seat-instances")
@RequiredArgsConstructor
public class SeatInstanceController {

    private final SeatInstanceService seatInstanceService;

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping
    public ResponseEntity<SeatInstanceResponse> createSeatInstance(
            @Valid @RequestBody SeatInstanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seatInstanceService.createSeatInstance(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatInstanceResponse> getSeatInstanceById(@PathVariable Long id) {
        return ResponseEntity.ok(seatInstanceService.getSeatInstanceById(id));
    }

    @PostMapping("/price/total")
    public ResponseEntity<Double> calculateSeatPrice(@RequestBody List<Long> seatInstanceIds) {
        return ResponseEntity.ok(seatInstanceService.calculateSeatPrice(seatInstanceIds));
    }

    @GetMapping("/flight-instance/{flightInstanceId}")
    public ResponseEntity<List<SeatInstanceResponse>> getSeatInstancesByFlightInstanceId(
            @PathVariable Long flightInstanceId) {
        return ResponseEntity.ok(seatInstanceService.getSeatInstancesByFlightInstanceId(flightInstanceId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<SeatInstanceResponse>> getAllByIds(
            @RequestParam List<Long> Ids) {
        return ResponseEntity.ok(seatInstanceService.getAllByIds(Ids));
    }

    @GetMapping("/flight-instance/{flightInstanceId}/available")
    public ResponseEntity<List<SeatInstanceResponse>> getAvailableSeatsByFlightInstanceId(
            @PathVariable Long flightInstanceId) {
        return ResponseEntity.ok(seatInstanceService.getAvailableSeatsByFlightInstanceId(flightInstanceId));
    }

    @GetMapping("/flight-instance/{flightInstanceId}/available/count")
    public ResponseEntity<Long> countAvailableByFlightInstanceId(@PathVariable Long flightInstanceId) {
        return ResponseEntity.ok(seatInstanceService.countAvailableByFlightInstanceId(flightInstanceId));
    }

    // Manual admin override only (e.g. block a broken seat). BOOKED is set by the booking flow,
    // never by a direct API call.
    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PatchMapping("/{id}/status")
    public ResponseEntity<SeatInstanceResponse> updateSeatInstanceStatus(
            @PathVariable Long id,
            @RequestParam SeatAvailabilityStatus status) {
        if (status != SeatAvailabilityStatus.AVAILABLE && status != SeatAvailabilityStatus.BLOCKED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only AVAILABLE or BLOCKED can be set manually");
        }
        return ResponseEntity.ok(seatInstanceService.updateSeatInstanceStatus(id, status));
    }
}
