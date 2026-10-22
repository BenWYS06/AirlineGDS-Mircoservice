package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.payload.request.FareRulesRequest;
import com.ben.common_lib.payload.response.FareRulesResponse;
import com.ben.services.service.FareRulesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fare-rules")
@RequiredArgsConstructor
public class FareRulesController {

    private final FareRulesService fareRulesService;

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping
    public ResponseEntity<FareRulesResponse> createFareRules(
            @Valid @RequestBody FareRulesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fareRulesService.createFareRules(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FareRulesResponse> getFareRulesById(@PathVariable Long id) {
        return ResponseEntity.ok(fareRulesService.getFareRulesById(id));
    }

    @GetMapping("/fare/{fareId}")
    public ResponseEntity<FareRulesResponse> getFareRulesByFareId(
            @PathVariable Long fareId) {
        return ResponseEntity.ok(fareRulesService.getFareRulesByFareId(fareId));
    }

    @GetMapping("/airline/{airlineId}")
    public ResponseEntity<List<FareRulesResponse>> getFareRulesByAirlineId(
            @PathVariable Long airlineId) {
        return ResponseEntity.ok(fareRulesService.getFareRulesByAirlineId(airlineId));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PutMapping("/{id}")
    public ResponseEntity<FareRulesResponse> updateFareRules(
            @PathVariable Long id,
            @Valid @RequestBody FareRulesRequest request) {
        return ResponseEntity.ok(fareRulesService.updateFareRules(id, request));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFareRules(@PathVariable Long id) {
        fareRulesService.deleteFareRules(id);
        return ResponseEntity.noContent().build();
    }
}
