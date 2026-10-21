package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.enums.CabinClassType;
import com.ben.common_lib.payload.request.CabinClassRequest;
import com.ben.common_lib.payload.response.CabinClassResponse;
import com.ben.services.service.CabinClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cabin-classes")
@RequiredArgsConstructor
public class CabinClassController {

    private final CabinClassService cabinClassService;

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping
    public ResponseEntity<CabinClassResponse> createCabinClass(
            @Valid @RequestBody CabinClassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cabinClassService.createCabinClass(request));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping("/create/bulk")
    public ResponseEntity<List<CabinClassResponse>> createCabinClasses(
            @Valid @RequestBody List<CabinClassRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cabinClassService.createCabinClasses(requests));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CabinClassResponse> getCabinClassById(@PathVariable Long id) {
        return ResponseEntity.ok(cabinClassService.getCabinClassById(id));
    }

    @GetMapping("/aircraft/{id}/name/{cabinClass}")
    public ResponseEntity<CabinClassResponse> getCabinClassByAircraftIdAndName(
            @PathVariable CabinClassType cabinClass,
            @PathVariable Long id) {
        return ResponseEntity.ok(
                cabinClassService.getByAircraftIdAndName(
                id,cabinClass
        ));
    }

    @GetMapping("/aircraft/{aircraftId}")
    public ResponseEntity<List<CabinClassResponse>> getCabinClassesByAircraftId(
            @PathVariable Long aircraftId) {
        return ResponseEntity.ok(cabinClassService.getCabinClassesByAircraftId(aircraftId));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PutMapping("/{id}")
    public ResponseEntity<CabinClassResponse> updateCabinClass(
            @PathVariable Long id,
            @Valid @RequestBody CabinClassRequest request) {
        return ResponseEntity.ok(cabinClassService.updateCabinClass(id, request));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCabinClass(@PathVariable Long id) {
        cabinClassService.deleteCabinClass(id);
        return ResponseEntity.noContent().build();
    }
}
