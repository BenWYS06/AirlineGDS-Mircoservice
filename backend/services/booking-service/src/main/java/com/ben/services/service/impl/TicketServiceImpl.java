package com.ben.services.service.impl;

import com.ben.common_lib.enums.TicketStatus;
import com.ben.common_lib.exception.ResourceNotFoundException;
import com.ben.services.model.Booking;
import com.ben.services.model.Passenger;
import com.ben.services.model.Ticket;
import com.ben.services.repository.TicketRepository;
import com.ben.services.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public Ticket issueTicket(Booking booking, Passenger passenger, Long seatInstanceId) {
        String ticketNumber = generateUniqueTicketNumber();

        Ticket ticket = Ticket.builder()
                .ticketNumber(ticketNumber)
                .status(TicketStatus.BOOKED)
                .issuedAt(LocalDateTime.now())
                .booking(booking)
                .passenger(passenger)
                .seatInstanceId(seatInstanceId)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);

        log.info("Generated ticket {} (seat instance {}) for passenger {} {} on booking {}",
                ticketNumber,
                seatInstanceId,
                passenger.getFirstName(),
                passenger.getLastName(),
                booking.getBookingReference());

        return savedTicket;
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getTicketByNumber(String ticketNumber) throws ResourceNotFoundException {
        return ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with number: " + ticketNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> getTicketsByBooking(Long bookingId) {
        return ticketRepository.findByBookingIdWithDetails(bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> getTicketsByPassenger(Long passengerId) {
        return ticketRepository.findByPassengerId(passengerId);
    }

    @Override
    @Transactional
    public Ticket cancelTicket(Long ticketId) throws ResourceNotFoundException {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with id: " + ticketId));

        if (ticket.getStatus() == TicketStatus.USED) {
            throw new IllegalStateException("Cannot cancel a ticket that has already been used");
        }

        if (ticket.getStatus() == TicketStatus.REFUNDED) {
            throw new IllegalStateException("Cannot cancel a ticket that has already been refunded");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        Ticket updatedTicket = ticketRepository.save(ticket);

        log.info("Ticket {} cancelled successfully", ticket.getTicketNumber());
        return updatedTicket;
    }

    @Override
    @Transactional
    public Ticket markTicketAsUsed(Long ticketId) throws ResourceNotFoundException {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with id: " + ticketId));

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Cannot use a cancelled ticket");
        }

        if (ticket.getStatus() == TicketStatus.REFUNDED) {
            throw new IllegalStateException("Cannot use a refunded ticket");
        }

        ticket.setStatus(TicketStatus.USED);
        Ticket updatedTicket = ticketRepository.save(ticket);

        log.info("Ticket {} marked as used", ticket.getTicketNumber());
        return updatedTicket;
    }

    @Override
    @Transactional
    public Ticket refundTicket(Long ticketId) throws ResourceNotFoundException {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with id: " + ticketId));

        if (ticket.getStatus() == TicketStatus.USED) {
            throw new IllegalStateException("Cannot refund a ticket that has already been used");
        }

        ticket.setStatus(TicketStatus.REFUNDED);
        Ticket updatedTicket = ticketRepository.save(ticket);

        log.info("Ticket {} refunded successfully", ticket.getTicketNumber());
        return updatedTicket;
    }

    private String generateUniqueTicketNumber() {
        String ticketNumber;
        do {
            String datePart = LocalDateTime.now().toString().substring(0, 10).replace("-", "");
            String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            ticketNumber = String.format("TKT-%s-%s", datePart, randomPart);
        } while (ticketRepository.existsByTicketNumber(ticketNumber));

        return ticketNumber;
    }
}
