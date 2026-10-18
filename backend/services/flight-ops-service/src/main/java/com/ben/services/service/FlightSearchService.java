package com.ben.services.service;


import com.ben.common_lib.payload.request.FlightSearchRequest;
import com.ben.common_lib.payload.response.FlightInstanceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface FlightSearchService {

    Page<FlightInstanceResponse> searchFlights(
            FlightSearchRequest request,
            Pageable pageable
    );

}
