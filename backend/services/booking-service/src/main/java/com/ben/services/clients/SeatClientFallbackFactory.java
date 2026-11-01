package com.ben.services.clients;

import com.ben.common_lib.payload.request.SeatHoldRequest;
import com.ben.common_lib.payload.response.SeatHoldResponse;
import com.ben.common_lib.payload.response.SeatInstanceResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class SeatClientFallbackFactory implements FallbackFactory<SeatClient> {

    @Override
    public SeatClient create(Throwable cause) {
        return new SeatClient() {

            @Override
            public Double calculateSeatPrice(List<Long> seatInstanceIds) {
                // Never fake a price (0.0 would undercharge): 503 → create compensates, user retries
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Pricing is temporarily unavailable. Please retry.");
            }

            @Override
            public List<SeatInstanceResponse> getAllByIds(List<Long> Ids) {
                return Collections.emptyList();
            }

            @Override
            public SeatHoldResponse holdSeats(SeatHoldRequest request) {
                // Business answer from seat-service: another booking holds a seat → tell the user
                if (cause instanceof FeignException.Conflict) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "One of the selected seats was just taken. Please choose another seat.");
                }
                // Input rejected by seat-service (seat ids not on this flight): the request is wrong, not retryable
                if (cause instanceof FeignException.BadRequest) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "The selected seats do not belong to this flight.");
                }
                // Real outage: no hold means no booking (never sell a seat we could not reserve)
                log.error("Seat hold for booking {} failed: {}", request.getBookingId(), cause.toString());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Seat service is temporarily unavailable. Please retry.");
            }

            @Override
            public Integer confirmSeats(Long bookingId, List<Long> seatInstanceIds) {
                // 409 = seat-service answered "another booking has a seat" → the caller turns it into REFUND_REQUIRED
                if (cause instanceof FeignException.Conflict) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Seats of booking " + bookingId + " were lost");
                }
                // Outage: throw 503 so the payment.completed message is retried later (money already paid)
                log.error("Seat confirm for booking {} failed: {}", bookingId, cause.toString());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Seat service is temporarily unavailable.");
            }

            @Override
            public Integer releaseSeats(Long bookingId) {
                // Not fatal: the hold expires on its own (held_until), so the seat frees up anyway
                log.warn("Could not release seats of booking {}: {}", bookingId, cause.toString());
                return 0;
            }
        };
    }
}
