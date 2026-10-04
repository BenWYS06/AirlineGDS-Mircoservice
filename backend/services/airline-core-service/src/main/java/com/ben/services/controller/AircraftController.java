package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.exception.ResourceNotFoundException;
import com.ben.common_lib.payload.request.AircraftRequest;
import com.ben.common_lib.payload.response.AircraftResponse;
import com.ben.services.service.AircraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aircrafts")
@RequiredArgsConstructor
public class AircraftController {

    private final AircraftService aircraftService;

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping
    public ResponseEntity<AircraftResponse> createAircraft(
            @RequestBody AircraftRequest request,
            @RequestHeader("X-User-Id") Long userId) throws ResourceNotFoundException {
        return ResponseEntity.ok(aircraftService.createAircraft(request, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AircraftResponse> getAircraftById(@PathVariable Long id)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(aircraftService.getAircraftById(id));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @GetMapping
    public ResponseEntity<List<AircraftResponse>> listAllAircrafts(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(aircraftService.listAllAircraftsByOwner(userId));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PutMapping("/{id}")
    public ResponseEntity<AircraftResponse> updateAircraft(
            @PathVariable Long id,
            @RequestBody AircraftRequest request,
            @RequestHeader("X-User-Id") Long userId) throws ResourceNotFoundException {
        return ResponseEntity.ok(aircraftService.updateAircraft(id, request, userId));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAircraft(@PathVariable Long id)
            throws ResourceNotFoundException {
        aircraftService.deleteAircraft(id);
        return ResponseEntity.noContent().build();
    }
}
