package com.ben.services.clients;

import com.ben.common_lib.payload.response.FlightCabinAncillaryResponse;
import com.ben.common_lib.payload.response.FlightMealResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@Component
public class AncillaryClientFallback implements AncillaryClient {

    // Prices must never be faked: 0.0 would undercharge the booking. 503 → create compensates, user retries.
    @Override
    public double calculateAncillariesPrice(List<Long> flightCabinAncillaryIds) {
        throw unavailable();
    }

    @Override
    public List<FlightCabinAncillaryResponse> getAllByIds(List<Long> Ids) {
        return Collections.emptyList();
    }

    @Override
    public List<FlightMealResponse> getMealsByIds(List<Long> Ids) {
        return Collections.emptyList();
    }

    @Override
    public Double calculateMealPrice(List<Long> requests) {
        throw unavailable();
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Pricing is temporarily unavailable. Please retry.");
    }
}
