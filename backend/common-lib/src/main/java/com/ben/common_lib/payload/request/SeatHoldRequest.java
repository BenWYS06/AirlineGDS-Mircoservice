package com.ben.common_lib.payload.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** booking-service → seat-service: reserve these seats for this booking until it is paid. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHoldRequest {

    @NotNull
    private Long bookingId;

    @NotEmpty
    private List<Long> seatInstanceIds;
}
