package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.exception.ResourceNotFoundException;
import com.ben.common_lib.payload.request.FlightMealRequest;
import com.ben.common_lib.payload.response.FlightMealResponse;
import com.ben.services.service.FlightMealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flight-meals")
@RequiredArgsConstructor
public class FlightMealController {

    private final FlightMealService flightMealService;

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping
    public ResponseEntity<FlightMealResponse> createFlightMeal(
            @Valid @RequestBody FlightMealRequest request) throws ResourceNotFoundException {
        FlightMealResponse response = flightMealService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping("/bulk")
    public ResponseEntity<List<FlightMealResponse>> bulkCreateFlightMeals(
            @Valid @RequestBody List<FlightMealRequest> requests) throws ResourceNotFoundException {
        List<FlightMealResponse> responses = flightMealService.bulkCreate(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @PostMapping("/price/total")
    public ResponseEntity<Double> calculateMealPrice(
            @RequestBody List<Long> requests) {
        double responses = flightMealService.calculateMealPrice(requests);
        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<FlightMealResponse> getFlightMealById(@PathVariable Long id)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(flightMealService.getById(id));
    }

    @GetMapping("/flight/{flightId:\\d+}")
    public ResponseEntity<List<FlightMealResponse>> getMealsByFlightId(
            @PathVariable Long flightId) {
        return ResponseEntity.ok(flightMealService.getByFlightId(flightId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<FlightMealResponse>> getMealsByIds(
            @RequestParam List<Long> Ids) {
        return ResponseEntity.ok(flightMealService.getAllByIds(Ids));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PutMapping("/{id:\\d+}")
    public ResponseEntity<FlightMealResponse> updateFlightMeal(
            @PathVariable Long id,
            @Valid @RequestBody FlightMealRequest request) throws ResourceNotFoundException {
        return ResponseEntity.ok(flightMealService.update(id, request));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PatchMapping("/{id:\\d+}/availability")
    public ResponseEntity<FlightMealResponse> updateFlightMealAvailability(
            @PathVariable Long id,
            @RequestParam Boolean available) throws ResourceNotFoundException {
        return ResponseEntity.ok(flightMealService.updateAvailability(id, available));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteFlightMeal(@PathVariable Long id)
            throws ResourceNotFoundException {
        flightMealService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
