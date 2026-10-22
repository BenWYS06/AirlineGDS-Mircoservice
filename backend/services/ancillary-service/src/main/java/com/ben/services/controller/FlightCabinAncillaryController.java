package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.enums.AncillaryType;
import com.ben.common_lib.exception.ResourceNotFoundException;
import com.ben.common_lib.payload.request.FlightCabinAncillaryRequest;
import com.ben.common_lib.payload.response.FlightCabinAncillaryResponse;
import com.ben.services.service.FlightCabinAncillaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flight-cabin-ancillaries")
@RequiredArgsConstructor
public class FlightCabinAncillaryController {

    private final FlightCabinAncillaryService service;

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping
    public ResponseEntity<FlightCabinAncillaryResponse> create(
            @Valid @RequestBody FlightCabinAncillaryRequest request)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(service.create(request));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping("/bulk")
    public ResponseEntity<List<FlightCabinAncillaryResponse>> bulkCreate(
            @Valid @RequestBody List<FlightCabinAncillaryRequest> requests)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(service.bulkCreate(requests));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<FlightCabinAncillaryResponse> getById(@PathVariable Long id)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/all")
    public ResponseEntity<List<FlightCabinAncillaryResponse>> getAllByIds(
            @RequestParam List<Long> Ids) {
        return ResponseEntity.ok(service.getAllByIds(Ids));
    }

    @GetMapping("/flight/{flightId:\\d+}/cabin/{cabinClassId}")
    public ResponseEntity<List<FlightCabinAncillaryResponse>> getAllByFlightAndCabinClass(
            @PathVariable Long flightId,
            @PathVariable Long cabinClassId) {
        return ResponseEntity.ok(service.getAllByFlightAndCabinClass(flightId, cabinClassId));
    }

    @GetMapping("/flight/{flightId}/cabin/{cabinClassId}/type/{type}")
    public ResponseEntity<FlightCabinAncillaryResponse> getByFlightAndCabinClassAndType(
            @PathVariable Long flightId,
            @PathVariable Long cabinClassId,
            @PathVariable AncillaryType type) throws ResourceNotFoundException {
        return ResponseEntity.ok(
                service.getByFlightIdAndCabinClassAndType(flightId, cabinClassId, type));
    }

    @GetMapping("/flight/{flightId}/cabin/{cabinClassId}/type/{type}/all")
    public ResponseEntity<?> getAllByFlightAndCabinClassAndType(
            @PathVariable Long flightId,
            @PathVariable Long cabinClassId,
            @PathVariable AncillaryType type) throws ResourceNotFoundException {
        return ResponseEntity.ok(
                service.getAllByFlightIdAndCabinClassAndType(flightId, cabinClassId, type));
    }



    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PutMapping("/{id:\\d+}")
    public ResponseEntity<FlightCabinAncillaryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FlightCabinAncillaryRequest request)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(service.update(id, request));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/price/total")
    public ResponseEntity<?> calculateAncillariesPrice(
            @RequestBody List<Long> flightCabinAncillaryIds)
             {
        return ResponseEntity.ok(service.calculateAncillaryPrice(flightCabinAncillaryIds));
    }

}
