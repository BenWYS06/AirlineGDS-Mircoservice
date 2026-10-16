package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.enums.AirlineStatus;
import com.ben.common_lib.payload.request.AirlineRequest;
import com.ben.common_lib.payload.response.AirlineDropdownItem;
import com.ben.common_lib.payload.response.AirlineResponse;
import com.ben.services.service.AirlineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/airlines")
@RequiredArgsConstructor
public class AirlineController {

    private final AirlineService airlineService;

    // ---------- CRUD ----------

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping
    public ResponseEntity<AirlineResponse> createAirline(
            @Valid @RequestBody AirlineRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(airlineService.createAirline(request, userId));
    }



    @GetMapping("/admin")
    public ResponseEntity<AirlineResponse> getAirlineByOwner(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(airlineService.getAirlineByOwner(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AirlineResponse> getAirlineById(

            @PathVariable Long id) {
        return ResponseEntity.ok(airlineService.getAirlineById(id));
    }

    @GetMapping
    public ResponseEntity<Page<AirlineResponse>> getAllAirlines(Pageable pageable) {
        return ResponseEntity.ok(airlineService.getAllAirlines(pageable));
    }

    @GetMapping("/dropdown")
    public ResponseEntity<List<AirlineDropdownItem>> getAirlinesForDropdown() {
        return ResponseEntity.ok(airlineService.getAirlinesForDropdown());
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PutMapping
    public ResponseEntity<AirlineResponse> updateAirline(
            @Valid @RequestBody AirlineRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(airlineService.updateAirline(request, userId));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAirline(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        airlineService.deleteAirline(id, userId);
        return ResponseEntity.noContent().build();
    }


    @RequiredRoles({})
    @PostMapping("/{id}/approve")
    public ResponseEntity<AirlineResponse> approveAirline(@PathVariable Long id) {
        return ResponseEntity.ok(airlineService.changeStatusByAdmin(id, AirlineStatus.ACTIVE));
    }

    @RequiredRoles({})
    @PostMapping("/{id}/suspend")
    public ResponseEntity<AirlineResponse> suspendAirline(@PathVariable Long id) {
        return ResponseEntity.ok(airlineService.changeStatusByAdmin(id, AirlineStatus.INACTIVE));
    }

    @RequiredRoles({})
    @PostMapping("/{id}/ban")
    public ResponseEntity<AirlineResponse> banAirline(@PathVariable Long id) {
        return ResponseEntity.ok(airlineService.changeStatusByAdmin(id, AirlineStatus.BANNED));
    }



}
