package com.ben.services.integration.impl;

import com.ben.common_lib.payload.response.FareResponse;
import com.ben.services.clients.PricingClient;
import com.ben.services.integration.PricingIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@Service
public class PriceIntegrationServiceImpl implements PricingIntegrationService {

    private final PricingClient pricingClient;

    @Override
    public Double calculateFareTotal(Long fareId) {
        FareResponse fare=pricingClient.getFareById(fareId);
        // null = pricing-service unavailable (its fallback returns null for display callers): never price without it
        if (fare == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Pricing is temporarily unavailable. Please retry.");
        }
        Double baseFare = fare.getBaseFare();
        Double taxesAndFees = fare.getTaxesAndFees() != null ? fare.getTaxesAndFees() : 0.0;
        Double airlineFees = fare.getAirlineFees() != null ? fare.getAirlineFees() : 0.0;
        return baseFare+taxesAndFees+airlineFees;

    }
}
