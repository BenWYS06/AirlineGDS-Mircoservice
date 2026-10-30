package com.ben.services.service;

import com.ben.common_lib.exception.ResourceNotFoundException;
import com.ben.services.model.Booking;
import com.ben.services.model.Passenger;
import com.ben.services.model.Ticket;

import java.util.List;

public interface TicketService {

    Ticket issueTicket(Booking booking, Passenger passenger, Long seatInstanceId);

    Ticket getTicketByNumber(String ticketNumber) throws ResourceNotFoundException;

    List<Ticket> getTicketsByBooking(Long bookingId);

    List<Ticket> getTicketsByPassenger(Long passengerId);

    Ticket cancelTicket(Long ticketId) throws ResourceNotFoundException;

    Ticket markTicketAsUsed(Long ticketId) throws ResourceNotFoundException;

    Ticket refundTicket(Long ticketId) throws ResourceNotFoundException;
}
