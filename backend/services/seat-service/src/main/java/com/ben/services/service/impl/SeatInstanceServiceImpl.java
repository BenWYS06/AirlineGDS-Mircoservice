package com.ben.services.service.impl;

import com.ben.common_lib.enums.SeatAvailabilityStatus;
import com.ben.common_lib.payload.request.SeatHoldRequest;
import com.ben.common_lib.payload.request.SeatInstanceRequest;
import com.ben.common_lib.payload.response.SeatHoldResponse;
import com.ben.common_lib.payload.response.SeatInstanceResponse;
import com.ben.services.mapper.SeatInstanceMapper;
import com.ben.services.model.FlightInstanceCabin;
import com.ben.services.model.Seat;
import com.ben.services.model.SeatInstance;
import com.ben.services.repository.FlightInstanceCabinRepository;
import com.ben.services.repository.SeatInstanceRepository;
import com.ben.services.repository.SeatRepository;
import com.ben.services.service.SeatInstanceService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SeatInstanceServiceImpl implements SeatInstanceService {

    private final SeatInstanceRepository seatInstanceRepository;
    private final SeatRepository seatRepository;
    private final FlightInstanceCabinRepository flightInstanceCabinRepository;

    // Longer than the VNPay payment link (15 min) so a user who pays at the last minute still has the seat
    @Value("${seat.hold-minutes:20}")
    private long holdMinutes;

    @Override
    public SeatInstanceResponse createSeatInstance(SeatInstanceRequest request) {
        Seat seat = seatRepository.findById(request.getSeatId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Seat not found with id: " + request.getSeatId()));

        // Required: the cabin is the seat instance's only link to its dated flight
        FlightInstanceCabin fic = flightInstanceCabinRepository.findById(request.getFlightInstanceCabinId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Flight instance cabin not found with id: " + request.getFlightInstanceCabinId()));

        SeatInstance seatInstance = SeatInstanceMapper.toEntity(request, seat, fic);
        SeatInstance saved = seatInstanceRepository.save(seatInstance);
        return SeatInstanceMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatInstanceResponse getSeatInstanceById(Long id) {
        SeatInstance si = seatInstanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Seat instance not found with id: " + id));
        return SeatInstanceMapper.toResponse(si);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatInstanceResponse> getSeatInstancesByFlightInstanceId(Long flightInstanceId) {
        return seatInstanceRepository.findByFlightInstanceId(flightInstanceId).stream()
                .map(SeatInstanceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatInstanceResponse> getAvailableSeatsByFlightInstanceId(Long flightInstanceId) {
        return seatInstanceRepository.findAvailableByFlightInstanceId(flightInstanceId, Instant.now()).stream()
                .map(SeatInstanceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SeatInstanceResponse> getAllByIds(List<Long> Ids) {
        List<SeatInstance> seatInstances = seatInstanceRepository.findAllById(Ids);
        return seatInstances.stream().map(
                SeatInstanceMapper::toResponse
        ).collect(Collectors.toList());
    }

    @Override
    public SeatInstanceResponse updateSeatInstanceStatus(Long id, SeatAvailabilityStatus status) {
        SeatInstance si = seatInstanceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Seat instance not found with id: " + id));

        si.setStatus(status);
        // Manual admin override (AVAILABLE / BLOCKED): the seat no longer belongs to any booking
        si.setBookingId(null);
        si.setHeldUntil(null);

        SeatInstance saved = seatInstanceRepository.save(si);
        return SeatInstanceMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countAvailableByFlightInstanceId(Long flightInstanceId) {
        return seatInstanceRepository.countAvailableByFlightInstanceId(flightInstanceId, Instant.now());
    }

    @Override
    public Double calculateSeatPrice(List<Long> seatInstanceIds) {
        List<SeatInstance> seatInstances = seatInstanceRepository.findAllById(seatInstanceIds);
        double total=0.0;
        for (SeatInstance si : seatInstances) {

            double seatPremium = si.getPremiumSurcharge() != null
                    ? si.getPremiumSurcharge()
                    : 0.0;

            total+=seatPremium;

        }
        return total;
    }

    // ── Seat hold ───────────────────────────────────────────────────────────

    @Override
    public SeatHoldResponse holdSeats(SeatHoldRequest request) {
        // Lock rows in ascending id order. Two bookings that want overlapping seats then both try
        // the lowest shared id first, so one simply waits for the other instead of each holding
        // a row the other needs (= deadlock).
        List<Long> seatIds = request.getSeatInstanceIds().stream()
                .distinct()
                .sorted()
                .toList();

        Instant now = Instant.now();
        Instant heldUntil = now.plus(holdMinutes, ChronoUnit.MINUTES);

        for (Long seatId : seatIds) {
            int updated = seatInstanceRepository.holdIfFree(seatId, request.getBookingId(), heldUntil, now);
            if (updated == 0) {
                // Throwing rolls back the holds already taken in this transaction: all-or-nothing
                log.info("Seat hold rejected for booking {}: seat instance {} is not free",
                        request.getBookingId(), seatId);
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Seat instance " + seatId + " is no longer available");
            }
        }

        log.info("Booking {} holds seat instances {} until {}", request.getBookingId(), seatIds, heldUntil);
        return SeatHoldResponse.builder()
                .bookingId(request.getBookingId())
                .seatInstanceIds(seatIds)
                .heldUntil(heldUntil)
                .build();
    }

    @Override
    public int confirmSeats(Long bookingId, List<Long> seatInstanceIds) {
        if (seatInstanceIds == null || seatInstanceIds.isEmpty()) {
            return 0;
        }
        int confirmed = seatInstanceRepository.confirmHeld(bookingId, seatInstanceIds);
        if (confirmed < seatInstanceIds.size()) {
            // The hold expired and another booking took the seat before payment arrived.
            // Handled properly by the saga (refund) in a later step; for now make it visible.
            log.warn("Booking {} confirmed only {}/{} seats: some holds were lost",
                    bookingId, confirmed, seatInstanceIds.size());
        }
        return confirmed;
    }

    @Override
    public int releaseSeats(Long bookingId) {
        int released = seatInstanceRepository.releaseHeld(bookingId);
        log.info("Released {} held seat(s) of booking {}", released, bookingId);
        return released;
    }
}
