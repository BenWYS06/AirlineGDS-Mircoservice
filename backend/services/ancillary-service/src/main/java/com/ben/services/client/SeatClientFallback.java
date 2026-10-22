package com.ben.services.client;

import com.ben.common_lib.payload.response.CabinClassResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class SeatClientFallback implements SeatClient {

    @Override
    public List<CabinClassResponse> getCabinClassesByAircraftId(Long aircraftId) {
        return Collections.emptyList();
    }
}
