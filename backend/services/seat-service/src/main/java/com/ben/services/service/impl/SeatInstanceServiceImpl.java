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

    // Outlives the VNPay link (10 min) + IPN grace (2 min) + one expiry-job interval (DESIGN.md §1)
    @Value("${seat.hold-minutes:15}")
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
        // De-duplicate so the row count below can be compared with the number of seats asked for
        List<Long> seatIds = request.getSeatInstanceIds().stream()
                .distinct()
                .sorted()
                .toList();

        // Never trust the caller: every seat must belong to the flight being booked
        long inFlight = seatInstanceRepository.countInFlightInstance(seatIds, request.getFlightInstanceId());
        if (inFlight != seatIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Some seats do not exist or do not belong to flight instance " + request.getFlightInstanceId());
        }

        Instant now = Instant.now();
        Instant heldUntil = now.plus(holdMinutes, ChronoUnit.MINUTES);

        // One atomic UPDATE for all seats (race- and deadlock-safe, see SeatInstanceRepository.holdAll)
        int held = seatInstanceRepository.holdAll(seatIds, request.getBookingId(), heldUntil, now);
        if (held != seatIds.size()) {
            // All-or-nothing: throwing rolls back the seats this UPDATE did take
            log.info("Seat hold rejected for booking {}: {} of {} seats free",
                    request.getBookingId(), held, seatIds.size());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "One of the selected seats is no longer available");
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
        List<Long> seatIds = seatInstanceIds.stream().distinct().toList();
        int confirmed = seatInstanceRepository.confirmForBooking(bookingId, seatIds, Instant.now());
        if (confirmed != seatIds.size()) {
            // All-or-nothing: another booking has one of the seats. Throwing rolls back the ones we did book;
            // booking-service turns this 409 into REFUND_REQUIRED.
            log.warn("Booking {} lost seats: only {}/{} could be confirmed", bookingId, confirmed, seatIds.size());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Some seats of this booking were lost");
        }
        log.info("Booking {}: {} seat(s) BOOKED", bookingId, confirmed);
        return confirmed;
    }

    @Override
    public int releaseSeats(Long bookingId) {
        int released = seatInstanceRepository.releaseHeld(bookingId);
        log.info("Released {} held seat(s) of booking {}", released, bookingId);
        return released;
    }
}
