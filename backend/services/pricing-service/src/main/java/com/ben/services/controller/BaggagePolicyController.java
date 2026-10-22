package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.payload.request.BaggagePolicyRequest;
import com.ben.common_lib.payload.response.BaggagePolicyResponse;
import com.ben.services.service.BaggagePolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/baggage-policies")
@RequiredArgsConstructor
public class BaggagePolicyController {

    private final BaggagePolicyService baggagePolicyService;

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping
    public ResponseEntity<BaggagePolicyResponse> createBaggagePolicy(
            @Valid @RequestBody BaggagePolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(baggagePolicyService.createBaggagePolicy(request));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping("/bulk")
    public ResponseEntity<List<BaggagePolicyResponse>> createBaggagePolicies(
            @Valid @RequestBody List<BaggagePolicyRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(baggagePolicyService.createBaggagePolicies(requests));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaggagePolicyResponse> getBaggagePolicyById(@PathVariable Long id) {
        return ResponseEntity.ok(baggagePolicyService.getBaggagePolicyById(id));
    }

    @GetMapping("/fare/{fareId}")
    public ResponseEntity<BaggagePolicyResponse> getBaggagePolicyByFareId(
            @PathVariable Long fareId) {
        return ResponseEntity.ok(baggagePolicyService.getBaggagePolicyByFareId(fareId));
    }

    @GetMapping("/airline/{airlineId}")
    public ResponseEntity<List<BaggagePolicyResponse>> getBaggagePoliciesByAirlineId(
            @PathVariable Long airlineId) {
        return ResponseEntity.ok(baggagePolicyService.getBaggagePoliciesByAirlineId(airlineId));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PutMapping("/{id}")
    public ResponseEntity<BaggagePolicyResponse> updateBaggagePolicy(
            @PathVariable Long id,
            @Valid @RequestBody BaggagePolicyRequest request) {
        return ResponseEntity.ok(baggagePolicyService.updateBaggagePolicy(id, request));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBaggagePolicy(@PathVariable Long id) {
        baggagePolicyService.deleteBaggagePolicy(id);
        return ResponseEntity.noContent().build();
    }
}
