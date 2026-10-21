package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.payload.request.FlightInstanceCabinRequest;
import com.ben.common_lib.payload.response.FlightInstanceCabinResponse;
import com.ben.services.service.FlightInstanceCabinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/flight-instance-cabins")
@RequiredArgsConstructor
public class FlightInstanceCabinController {

    private final FlightInstanceCabinService flightInstanceCabinService;

    // Cabins and their seat instances are created only by FlightInstanceEventConsumer
    // (Kafka "flight-instance-created"), so there is no POST endpoint here.

    @GetMapping("/{id}")
    public ResponseEntity<FlightInstanceCabinResponse> getFlightInstanceCabinById(
            @PathVariable Long id) {
        return ResponseEntity.ok(flightInstanceCabinService.getFlightInstanceCabinById(id));
    }

    @GetMapping("/flight-instance/{flightInstanceId}/cabin-class/{cabinClassId}")
    public ResponseEntity<?> getByFlightInstanceIdAndCabinClassId(
            @PathVariable Long cabinClassId,
            @PathVariable Long flightInstanceId) {
        return ResponseEntity.ok(
                flightInstanceCabinService.getByFlightInstanceIdAndCabinClassId(
                        flightInstanceId,cabinClassId
                ));
    }

    @GetMapping("/flight-instance/{flightInstanceId}")
    public ResponseEntity<Page<FlightInstanceCabinResponse>> getByFlightInstanceId(
            @PathVariable Long flightInstanceId, Pageable pageable) {
        return ResponseEntity.ok(
                flightInstanceCabinService.getByFlightInstanceId(flightInstanceId, pageable));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PutMapping("/{id}")
    public ResponseEntity<FlightInstanceCabinResponse> updateFlightInstanceCabin(
            @PathVariable Long id,
            @Valid @RequestBody FlightInstanceCabinRequest request) {
        return ResponseEntity.ok(
                flightInstanceCabinService.updateFlightInstanceCabin(id, request));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlightInstanceCabin(@PathVariable Long id) {
        flightInstanceCabinService.deleteFlightInstanceCabin(id);
        return ResponseEntity.noContent().build();
    }
}
