package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.payload.request.SeatMapRequest;
import com.ben.common_lib.payload.response.SeatMapResponse;
import com.ben.services.service.SeatMapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seat-maps")
@RequiredArgsConstructor
public class SeatMapController {

    private final SeatMapService seatMapService;

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping
    public ResponseEntity<SeatMapResponse> createSeatMap(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody SeatMapRequest request) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seatMapService.createSeatMap(userId, request));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping("/create/bulk")
    public ResponseEntity<List<SeatMapResponse>> createSeatMaps(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody List<SeatMapRequest> requests) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seatMapService.createSeatMaps(userId, requests));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatMapResponse> getSeatMapById(@PathVariable Long id) {
        return ResponseEntity.ok(seatMapService.getSeatMapById(id));
    }


    @GetMapping("/cabin-class/{cabinClassId}")
    public ResponseEntity<SeatMapResponse> getSeatMapsByCabinClass(
            @PathVariable Long cabinClassId) {
        SeatMapResponse responses = seatMapService.getSeatMapsByCabinClass(cabinClassId);
        return ResponseEntity.ok(responses);
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PutMapping("/{id}")
    public ResponseEntity<SeatMapResponse> updateSeatMap(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody SeatMapRequest request) {
        return ResponseEntity.ok(seatMapService.updateSeatMap(userId, id, request));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSeatMap(@PathVariable Long id) throws Exception {
        seatMapService.deleteSeatMap(id);
        return ResponseEntity.ok("Seat map deleted");
    }


}
