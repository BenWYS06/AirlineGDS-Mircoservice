package com.ben.services.Integration;

import com.ben.common_lib.payload.response.AircraftResponse;

public interface AirlineIntegrationService {
    Long getAirlineIdForUser(Long userId);
    AircraftResponse getAircraftById(Long aircraftId);
}
