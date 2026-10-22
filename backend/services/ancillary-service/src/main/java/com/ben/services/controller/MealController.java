package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.exception.ResourceNotFoundException;
import com.ben.common_lib.payload.request.MealRequest;
import com.ben.common_lib.payload.response.MealResponse;
import com.ben.services.service.MealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping
    public ResponseEntity<MealResponse> createMeal(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody MealRequest request) throws ResourceNotFoundException {
        MealResponse response = mealService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PostMapping("/bulk")
    public ResponseEntity<List<MealResponse>> bulkCreateMeals(
            @Valid @RequestBody List<MealRequest> requests,
            @RequestHeader("X-User-Id") Long userId) throws ResourceNotFoundException {
        List<MealResponse> responses = mealService.bulkCreate(userId, requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<MealResponse> getMealById(@PathVariable Long id)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(mealService.getById(id));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @GetMapping("/airline")
    public ResponseEntity<List<MealResponse>> getMealsByAirlineId(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(mealService.getByAirlineId(userId));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PutMapping("/{id:\\d+}")
    public ResponseEntity<MealResponse> updateMeal(
            @PathVariable Long id,
            @Valid @RequestBody MealRequest request,
            @RequestHeader("X-User-Id") Long userId) throws ResourceNotFoundException {
        return ResponseEntity.ok(mealService.update(userId, id, request));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PatchMapping("/{id:\\d+}/availability")
    public ResponseEntity<MealResponse> updateMealAvailability(
            @PathVariable Long id,
            @RequestParam Boolean available) throws ResourceNotFoundException {
        return ResponseEntity.ok(mealService.updateAvailability(id, available));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteMeal(@PathVariable Long id) throws ResourceNotFoundException {
        mealService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
