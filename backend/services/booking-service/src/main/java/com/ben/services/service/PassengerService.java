package com.ben.services.service;

import com.ben.common_lib.exception.ResourceNotFoundException;
import com.ben.common_lib.payload.request.PassengerRequest;
import com.ben.common_lib.payload.response.PassengerResponse;
import com.ben.services.model.Booking;
import com.ben.services.model.Passenger;

public interface PassengerService {

    PassengerResponse createPassenger(PassengerRequest request, Long userId)
            throws ResourceNotFoundException;

    // Each booking gets its own passenger rows (a snapshot of the traveller's details at booking time)
    Passenger createPassengerForBooking(PassengerRequest request, Long userId, Booking booking);

    boolean existsById(Long id);

    long count();
}
