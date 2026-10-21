package com.ben.services.mapper;

import com.ben.common_lib.payload.request.SeatRequest;
import com.ben.common_lib.payload.response.SeatResponse;
import com.ben.services.model.CabinClass;
import com.ben.services.model.Seat;
import com.ben.services.model.SeatMap;

public class SeatMapper {


    public static void updateEntity(SeatRequest request, Seat seat, SeatMap seatMap) {
        seat.setSeatNumber(request.getSeatNumber());
        seat.setSeatRow(request.getSeatRow());
        seat.setColumnLetter(request.getColumnLetter());
        seat.setSeatType(request.getSeatType());
        seat.setSeatMap(seatMap);
        if (request.getIsAvailable() != null) seat.setIsAvailable(request.getIsAvailable());
        if (request.getIsBlocked() != null) seat.setIsBlocked(request.getIsBlocked());
        if (request.getIsEmergencyExit() != null) seat.setIsEmergencyExit(request.getIsEmergencyExit());
        if (request.getIsActive() != null) seat.setIsActive(request.getIsActive());
        seat.setBasePrice(request.getBasePrice());
        seat.setPremiumSurcharge(request.getPremiumSurcharge());
        if (request.getHasExtraLegroom() != null) seat.setHasExtraLegroom(request.getHasExtraLegroom());
        if (request.getHasBassinet() != null) seat.setHasBassinet(request.getHasBassinet());
        if (request.getIsNearLavatory() != null) seat.setIsNearLavatory(request.getIsNearLavatory());
        if (request.getIsNearGalley() != null) seat.setIsNearGalley(request.getIsNearGalley());
        if (request.getHasPowerOutlet() != null) seat.setHasPowerOutlet(request.getHasPowerOutlet());
        if (request.getHasTvScreen() != null) seat.setHasTvScreen(request.getHasTvScreen());
        if (request.getIsWheelchairAccessible() != null) seat.setIsWheelchairAccessible(request.getIsWheelchairAccessible());
        if (request.getHasExtraWidth() != null) seat.setHasExtraWidth(request.getHasExtraWidth());
        seat.setSeatPitch(request.getSeatPitch());
        seat.setSeatWidth(request.getSeatWidth());
        seat.setReclineAngle(request.getReclineAngle());
    }

    public static SeatResponse toResponse(Seat seat) {
        // A seat's cabin is always its seat map's cabin
        CabinClass cabinClass = seat.getSeatMap() != null ? seat.getSeatMap().getCabinClass() : null;
        return SeatResponse.builder()
                .id(seat.getId())
                .seatNumber(seat.getSeatNumber())
                .seatRow(seat.getSeatRow())
                .columnLetter(seat.getColumnLetter())
                .seatType(seat.getSeatType())
                .isAvailable(seat.getIsAvailable())
                .isBlocked(seat.getIsBlocked())
                .isEmergencyExit(seat.getIsEmergencyExit())
                .isActive(seat.getIsActive())
                .basePrice(seat.getBasePrice())
                .premiumSurcharge(seat.getPremiumSurcharge())
                .totalPrice(seat.getTotalPrice())
                .hasExtraLegroom(seat.getHasExtraLegroom())
                .hasBassinet(seat.getHasBassinet())
                .isNearLavatory(seat.getIsNearLavatory())
                .isNearGalley(seat.getIsNearGalley())
                .hasPowerOutlet(seat.getHasPowerOutlet())
                .hasTvScreen(seat.getHasTvScreen())
                .isWheelchairAccessible(seat.getIsWheelchairAccessible())
                .hasExtraWidth(seat.getHasExtraWidth())
                .seatPitch(seat.getSeatPitch())
                .seatWidth(seat.getSeatWidth())
                .reclineAngle(seat.getReclineAngle())
                .seatMapId(seat.getSeatMap() != null ? seat.getSeatMap().getId() : null)
                .seatMapName(seat.getSeatMap() != null ? seat.getSeatMap().getName() : null)
                .cabinClassId(cabinClass != null ? cabinClass.getId() : null)
                .cabinClassName(cabinClass != null ? cabinClass.getName().toString() : null)
                .createdAt(seat.getCreatedAt())
                .updatedAt(seat.getUpdatedAt())
                .createdBy(seat.getCreatedBy())
                .updatedBy(seat.getUpdatedBy())
                .isPremiumSeat(seat.isPremiumSeat())
                .isBookable(seat.isBookable())
                .fullPosition(seat.getFullPosition())
                .seatCharacteristics(seat.getSeatCharacteristics())
                .build();
    }
}
