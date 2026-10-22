package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.exception.ResourceNotFoundException;
import com.ben.common_lib.payload.request.InsuranceCoverageRequest;
import com.ben.common_lib.payload.response.ApiResponse;
import com.ben.common_lib.payload.response.InsuranceCoverageResponse;
import com.ben.services.service.InsuranceCoverageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insurance-coverages")
@RequiredArgsConstructor
public class InsuranceCoverageController {

    private final InsuranceCoverageService coverageService;

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping
    public ResponseEntity<InsuranceCoverageResponse> createCoverage(
            @Valid @RequestBody InsuranceCoverageRequest request,
            @RequestHeader("X-User-Id") Long userId) throws ResourceNotFoundException {
        InsuranceCoverageResponse response = coverageService.createCoverage(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping("/bulk")
    public ResponseEntity<List<InsuranceCoverageResponse>> createCoveragesBulk(
            @Valid @RequestBody List<InsuranceCoverageRequest> requests,
            @RequestHeader("X-User-Id") Long userId) throws ResourceNotFoundException {
        List<InsuranceCoverageResponse> responses = coverageService.createCoveragesBulk(requests);
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PutMapping("/{id:\\d+}")
    public ResponseEntity<InsuranceCoverageResponse> updateCoverage(
            @PathVariable Long id,
            @Valid @RequestBody InsuranceCoverageRequest request) throws ResourceNotFoundException {
        return ResponseEntity.ok(coverageService.updateCoverage(id, request));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse> deleteCoverage(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) throws ResourceNotFoundException {
        coverageService.deleteCoverage(id);
        return ResponseEntity.ok(new ApiResponse("Coverage deleted successfully"));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<InsuranceCoverageResponse> getCoverageById(@PathVariable Long id)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(coverageService.getCoverageById(id));
    }

    @GetMapping
    public ResponseEntity<List<InsuranceCoverageResponse>> getAllCoverages() {
        return ResponseEntity.ok(coverageService.getAllCoverages());
    }

    @GetMapping("/ancillary/{ancillaryId:\\d+}")
    public ResponseEntity<List<InsuranceCoverageResponse>> getCoveragesByAncillaryId(
            @PathVariable Long ancillaryId) {
        return ResponseEntity.ok(coverageService.getCoveragesByAncillaryId(ancillaryId));
    }

    @GetMapping("/ancillary/{ancillaryId:\\d+}/active")
    public ResponseEntity<List<InsuranceCoverageResponse>> getActiveCoveragesByAncillaryId(
            @PathVariable Long ancillaryId) {
        return ResponseEntity.ok(coverageService.getActiveCoveragesByAncillaryId(ancillaryId));
    }
}
