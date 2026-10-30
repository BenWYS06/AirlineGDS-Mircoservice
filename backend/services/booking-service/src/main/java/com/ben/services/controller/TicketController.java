package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.exception.ResourceNotFoundException;
import com.ben.common_lib.payload.response.TicketResponse;
import com.ben.services.mapper.TicketMapper;
import com.ben.services.model.Ticket;
import com.ben.services.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/{ticketNumber}")
    public ResponseEntity<TicketResponse> getTicketByNumber(
            @PathVariable String ticketNumber) throws ResourceNotFoundException {
        Ticket ticket = ticketService.getTicketByNumber(ticketNumber);
        return ResponseEntity.ok(TicketMapper.toResponse(ticket));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<TicketResponse>> getTicketsByBooking(
            @PathVariable Long bookingId) {
        List<Ticket> tickets = ticketService.getTicketsByBooking(bookingId);
        List<TicketResponse> responses = tickets.stream()
                .map(TicketMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<List<TicketResponse>> getTicketsByPassenger(
            @PathVariable Long passengerId) {
        List<Ticket> tickets = ticketService.getTicketsByPassenger(passengerId);
        List<TicketResponse> responses = tickets.stream()
                .map(TicketMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @RequiredRoles(UserRole.ROLE_CUSTOMER)
    @PutMapping("/{ticketId}/cancel")
    public ResponseEntity<TicketResponse> cancelTicket(
            @PathVariable Long ticketId,
            @RequestHeader("X-User-Id") Long userId) throws ResourceNotFoundException {
        Ticket ticket = ticketService.cancelTicket(ticketId);
        return ResponseEntity.ok(TicketMapper.toResponse(ticket));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PutMapping("/{ticketId}/use")
    public ResponseEntity<TicketResponse> markTicketAsUsed(
            @PathVariable Long ticketId,
            @RequestHeader("X-User-Id") Long userId) throws ResourceNotFoundException {
        Ticket ticket = ticketService.markTicketAsUsed(ticketId);
        return ResponseEntity.ok(TicketMapper.toResponse(ticket));
    }

    @RequiredRoles(UserRole.ROLE_AIRLINE_OWNER)
    @PutMapping("/{ticketId}/refund")
    public ResponseEntity<TicketResponse> refundTicket(
            @PathVariable Long ticketId,
            @RequestHeader("X-User-Id") Long userId) throws ResourceNotFoundException {
        Ticket ticket = ticketService.refundTicket(ticketId);
        return ResponseEntity.ok(TicketMapper.toResponse(ticket));
    }
}
