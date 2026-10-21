package com.ben.services.service.impl;

import com.ben.common_lib.payload.request.FlightInstanceCabinRequest;
import com.ben.common_lib.payload.response.FlightInstanceCabinResponse;
import com.ben.services.mapper.FlightInstanceCabinMapper;
import com.ben.services.model.CabinClass;
import com.ben.services.model.FlightInstanceCabin;
import com.ben.services.repository.*;
import com.ben.services.service.FlightInstanceCabinService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FlightInstanceCabinServiceImpl implements FlightInstanceCabinService {

    private final FlightInstanceCabinRepository flightInstanceCabinRepository;
    private final CabinClassRepository cabinClassRepository;

    @Override
    @Transactional(readOnly = true)
    public FlightInstanceCabinResponse getFlightInstanceCabinById(Long id) {
        FlightInstanceCabin fic = flightInstanceCabinRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Flight instance cabin not found with id: " + id));
        return FlightInstanceCabinMapper.toResponse(fic);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FlightInstanceCabinResponse> getByFlightInstanceId(Long flightInstanceId, Pageable pageable) {
        return flightInstanceCabinRepository.findByFlightInstanceId(flightInstanceId, pageable)
                .map(FlightInstanceCabinMapper::toResponse);
    }

    @Override
    public FlightInstanceCabinResponse getByFlightInstanceIdAndCabinClassId(Long flightInstanceId, Long cabinClassId) {
       FlightInstanceCabin cabin= flightInstanceCabinRepository.findByFlightInstanceIdAndCabinClassId(
                flightInstanceId,
                cabinClassId
       );
       return FlightInstanceCabinMapper.toResponse(cabin);
    }

    @Override
    public FlightInstanceCabinResponse updateFlightInstanceCabin(Long id, FlightInstanceCabinRequest request) {
        FlightInstanceCabin existing = flightInstanceCabinRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Flight instance cabin not found with id: " + id));

        if (request.getCabinClassId() != null) {
            CabinClass cabinClass = cabinClassRepository.findById(request.getCabinClassId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Cabin class not found with id: " + request.getCabinClassId()));
            existing.setCabinClass(cabinClass);
        }

        FlightInstanceCabin saved = flightInstanceCabinRepository.save(existing);
        return FlightInstanceCabinMapper.toResponse(saved);
    }

    @Override
    public void deleteFlightInstanceCabin(Long id) {
        FlightInstanceCabin fic = flightInstanceCabinRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Flight instance cabin not found with id: " + id));
        flightInstanceCabinRepository.delete(fic);
    }
}
