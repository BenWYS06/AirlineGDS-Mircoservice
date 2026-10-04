package com.ben.common_lib.payload.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatInstanceRequest {

    @NotNull(message = "Flight instance cabin ID is required")
    private Long flightInstanceCabinId;

    @NotNull
    private Long seatId;

    private String status;
    private String mealPreference;
    private Double fare;
}
