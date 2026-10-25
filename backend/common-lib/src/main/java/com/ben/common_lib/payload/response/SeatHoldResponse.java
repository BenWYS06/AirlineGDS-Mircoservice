package com.ben.common_lib.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHoldResponse {

    private Long bookingId;
    private List<Long> seatInstanceIds;

    // Seats go back to the pool after this moment if the booking is not paid
    private Instant heldUntil;
}
