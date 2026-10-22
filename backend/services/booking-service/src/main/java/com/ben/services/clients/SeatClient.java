package com.ben.services.clients;

import com.ben.common_lib.payload.request.SeatHoldRequest;
import com.ben.common_lib.payload.response.SeatHoldResponse;
import com.ben.common_lib.payload.response.SeatInstanceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// fallbackFactory (not fallback) so the fallback can see WHY the call failed:
// a 409 "seat taken" must reach the user, only real outages get a degraded answer.
@FeignClient(name = "seat-service", fallbackFactory = SeatClientFallbackFactory.class)
public interface SeatClient {

    @PostMapping("/api/seat-instances/price/total")
    Double calculateSeatPrice(@RequestBody List<Long> seatInstanceIds);

    @GetMapping("/api/seat-instances/all")
    List<SeatInstanceResponse> getAllByIds(
            @RequestParam List<Long> Ids);

    @PostMapping("/internal/seat-instances/hold")
    SeatHoldResponse holdSeats(@RequestBody SeatHoldRequest request);

    @PostMapping("/internal/seat-instances/bookings/{bookingId}/release")
    Integer releaseSeats(@PathVariable("bookingId") Long bookingId);

}
