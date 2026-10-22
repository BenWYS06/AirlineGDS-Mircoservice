package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.exception.ResourceNotFoundException;
import com.ben.common_lib.payload.request.AncillaryRequest;
import com.ben.common_lib.payload.response.AncillaryResponse;
import com.ben.services.service.AncillaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ancillaries")
@RequiredArgsConstructor
public class AncillaryController {

    private final AncillaryService ancillaryService;

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping
    public ResponseEntity<AncillaryResponse> create(
            @Valid @RequestBody AncillaryRequest request,
            @RequestHeader("X-User-Id") Long userId) throws ResourceNotFoundException {
        return ResponseEntity.ok(ancillaryService.create(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AncillaryResponse> getById(@PathVariable Long id)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(ancillaryService.getById(id));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @GetMapping
    public ResponseEntity<List<AncillaryResponse>> getAllByAirlineId(
            @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(ancillaryService.getAllByAirlineId(userId));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PutMapping("/{id}")
    public ResponseEntity<AncillaryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AncillaryRequest request) throws ResourceNotFoundException {
        return ResponseEntity.ok(ancillaryService.update(id, request));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ancillaryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
