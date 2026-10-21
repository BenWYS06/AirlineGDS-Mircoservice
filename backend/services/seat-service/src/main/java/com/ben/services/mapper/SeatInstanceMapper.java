package com.ben.services.mapper;

import com.ben.common_lib.enums.SeatAvailabilityStatus;
import com.ben.common_lib.payload.request.SeatInstanceRequest;
import com.ben.common_lib.payload.response.SeatInstanceResponse;
import com.ben.services.model.FlightInstanceCabin;
import com.ben.services.model.Seat;
import com.ben.services.model.SeatInstance;

import java.time.Instant;

public class SeatInstanceMapper {

    public static SeatInstance toEntity(SeatInstanceRequest request, Seat seat,
                                        FlightInstanceCabin flightInstanceCabin) {
        return SeatInstance.builder()
                .seat(seat)
                .flightInstanceCabin(flightInstanceCabin)
                .status(request.getStatus() != null ?
                        SeatAvailabilityStatus.valueOf(request.getStatus().toUpperCase()) :
                        SeatAvailabilityStatus.AVAILABLE)
                .mealPreference(request.getMealPreference())
                .fare(request.getFare())
                .build();
    }

    public static SeatInstanceResponse toResponse(SeatInstance si) {
        return SeatInstanceResponse.builder()
                .id(si.getId())
                .seatId(si.getSeat() != null ? si.getSeat().getId() : null)
                .seatNumber(si.getSeat() != null ? si.getSeat().getSeatNumber() : null)
                .seatType(si.getSeat() != null ? si.getSeat().getSeatType().name() : null)
                .seatPosition(si.getSeat() != null ? si.getSeat().getFullPosition() : null)
                .seat(SeatMapper.toResponse(si.getSeat()))
                .status(si.getStatus())
                .flightInstanceId(si.getFlightInstanceCabin() != null ? si.getFlightInstanceCabin().getFlightInstanceId() : null)
                .flightCabinId(si.getFlightInstanceCabin() != null ? si.getFlightInstanceCabin().getId() : null)
                .mealPreference(si.getMealPreference())
                .fare(si.getFare())
                .price(si.getPremiumSurcharge())
                .version(si.getVersion())
                .createdAt(si.getCreatedAt())
                .updatedAt(si.getUpdatedAt())
                .isAvailable(isFree(si))
                .isBooked(si.getStatus() == SeatAvailabilityStatus.BOOKED)
                .isOccupied(si.getStatus() == SeatAvailabilityStatus.OCCUPIED)
                .seatCharacteristics(
                        si.getSeat() != null ? si.getSeat().getSeatCharacteristics() : null)
                .build();
    }

    // Same rule as SeatInstanceRepository "free": AVAILABLE, or a HELD seat whose hold expired
    private static boolean isFree(SeatInstance si) {
        return si.getStatus() == SeatAvailabilityStatus.AVAILABLE
                || (si.getStatus() == SeatAvailabilityStatus.HELD
                && si.getHeldUntil() != null
                && si.getHeldUntil().isBefore(Instant.now()));
    }
}
