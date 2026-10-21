package com.ben.services.service;

import com.ben.common_lib.enums.SeatAvailabilityStatus;
import com.ben.common_lib.payload.request.SeatHoldRequest;
import com.ben.common_lib.payload.request.SeatInstanceRequest;
import com.ben.common_lib.payload.response.SeatHoldResponse;
import com.ben.common_lib.payload.response.SeatInstanceResponse;

import java.util.List;

public interface SeatInstanceService {

    SeatInstanceResponse createSeatInstance(SeatInstanceRequest request);
    SeatInstanceResponse getSeatInstanceById(Long id);
    List<SeatInstanceResponse> getSeatInstancesByFlightInstanceId(Long flightInstanceId);
    List<SeatInstanceResponse> getAvailableSeatsByFlightInstanceId(Long flightInstanceId);
    List<SeatInstanceResponse> getAllByIds(List<Long> Ids);
    SeatInstanceResponse updateSeatInstanceStatus(Long id, SeatAvailabilityStatus status);
    Long countAvailableByFlightInstanceId(Long flightInstanceId);
    Double calculateSeatPrice(List<Long> seatInstanceId);

    // ── Seat hold lifecycle: AVAILABLE → HELD (waiting for payment) → BOOKED, or back to AVAILABLE ──

    /** All-or-nothing: holds every seat for the booking, or none (409 if any seat is taken). */
    SeatHoldResponse holdSeats(SeatHoldRequest request);

    /** Payment succeeded: the booking's HELD seats become BOOKED. Returns how many were confirmed. */
    int confirmSeats(Long bookingId, List<Long> seatInstanceIds);

    /** Payment failed / booking cancelled: the booking's HELD seats become AVAILABLE again. */
    int releaseSeats(Long bookingId);
}
