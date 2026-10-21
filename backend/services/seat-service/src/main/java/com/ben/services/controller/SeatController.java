package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.payload.request.SeatRequest;
import com.ben.common_lib.payload.response.SeatResponse;
import com.ben.services.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;



    @GetMapping
    public ResponseEntity<List<SeatResponse>> getAllSeats() {
        return ResponseEntity.ok(seatService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatResponse> getSeatById(@PathVariable Long id) {
        return ResponseEntity.ok(seatService.getSeatById(id));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PutMapping("/{id}")
    public ResponseEntity<SeatResponse> updateSeat(
            @PathVariable Long id,
            @Valid @RequestBody SeatRequest request) {
        return ResponseEntity.ok(seatService.updateSeat(id, request));
    }


}
