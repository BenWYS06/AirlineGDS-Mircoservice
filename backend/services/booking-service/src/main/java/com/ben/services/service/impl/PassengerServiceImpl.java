package com.ben.services.service.impl;

import com.ben.common_lib.exception.ResourceNotFoundException;
import com.ben.common_lib.payload.request.PassengerRequest;
import com.ben.common_lib.payload.response.PassengerResponse;
import com.ben.services.mapper.PassengerMapper;
import com.ben.services.model.Booking;
import com.ben.services.model.Passenger;
import com.ben.services.repository.PassengerRepository;
import com.ben.services.service.PassengerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PassengerServiceImpl implements PassengerService {

    private final PassengerRepository passengerRepository;

    @Override
    @Transactional
    public PassengerResponse createPassenger(PassengerRequest request, Long userId)
            throws ResourceNotFoundException {
        Passenger passenger = PassengerMapper.toEntity(request);
        passenger.setPrimaryUserId(userId);
        Passenger saved = passengerRepository.save(passenger);
        return PassengerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public Passenger createPassengerForBooking(PassengerRequest request, Long userId, Booking booking) {
        Passenger passenger = PassengerMapper.toEntity(request);
        passenger.setPrimaryUserId(userId);
        passenger.setBooking(booking);
        return passengerRepository.save(passenger);
    }

    @Override
    public boolean existsById(Long id) {
        return passengerRepository.existsById(id);
    }

    @Override
    public long count() {
        return passengerRepository.count();
    }
}
