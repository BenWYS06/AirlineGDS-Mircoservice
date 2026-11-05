package com.ben.services.service.impl;

import com.ben.common_lib.enums.AircraftStatus;
import com.ben.common_lib.exception.ResourceNotFoundException;
import com.ben.common_lib.payload.request.AircraftRequest;
import com.ben.common_lib.payload.response.AircraftResponse;
import com.ben.services.model.Aircraft;
import com.ben.services.model.Airline;
import com.ben.services.repository.AircraftRepository;
import com.ben.services.repository.AirlineRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Seat counts and the code decide what seat-service and pricing build on, so bad data must stop here
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AircraftServiceImplTest {

    private static final Long OWNER_ID = 7L;

    @Mock
    private AircraftRepository aircraftRepository;

    @Mock
    private AirlineRepository airlineRepository;

    @InjectMocks
    private AircraftServiceImpl aircraftService;

    private final Airline airline = Airline.builder().id(1L).name("Airline").ownerId(OWNER_ID).build();

    @BeforeEach
    void ownerHasAnAirline() {
        when(airlineRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(airline));
        when(aircraftRepository.save(any(Aircraft.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsTheAircraftUnderTheOwnersAirline() throws Exception {
        AircraftResponse result = aircraftService.createAircraft(request("A321", 100, 80, 20), OWNER_ID);

        assertThat(result.getAirlineId()).isEqualTo(1L);
        assertThat(result.getTotalSeats()).isEqualTo(100);
    }

    @Test
    void ownerWithoutAnAirlineCannotCreate() {
        when(airlineRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aircraftService.createAircraft(request("A321", 100, 80, 20), OWNER_ID))
                .isInstanceOf(EntityNotFoundException.class);
        verify(aircraftRepository, never()).save(any());
    }

    @Test
    void rejectsADuplicateCode() {
        when(aircraftRepository.existsByCode("A321")).thenReturn(true);

        assertThatThrownBy(() -> aircraftService.createAircraft(request("A321", 100, 80, 20), OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A321");
        verify(aircraftRepository, never()).save(any());
    }

    @Test
    void rejectsCapacityThatIsNotPositive() {
        assertThatThrownBy(() -> aircraftService.createAircraft(request("A321", 0, null, null), OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("capacity");
        verify(aircraftRepository, never()).save(any());
    }

    @Test
    void rejectsMoreSeatsThanTheCapacity() {
        assertThatThrownBy(() -> aircraftService.createAircraft(request("A321", 100, 81, 20), OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceed");
        verify(aircraftRepository, never()).save(any());
    }

    @Test
    void acceptsSeatsThatExactlyFillTheCapacityWithSomeClassesEmpty() throws Exception {
        AircraftRequest request = request("A321", 100, 100, null);

        assertThat(aircraftService.createAircraft(request, OWNER_ID).getTotalSeats()).isEqualTo(100);
    }

    @Test
    void rejectsAYearBefore1900() {
        AircraftRequest request = request("A321", 100, 80, 20);
        request.setYearOfManufacture(1899);

        assertThatThrownBy(() -> aircraftService.createAircraft(request, OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("year");
    }

    @Test
    void rejectsAYearInTheFuture() {
        AircraftRequest request = request("A321", 100, 80, 20);
        request.setYearOfManufacture(LocalDate.now().getYear() + 1);

        assertThatThrownBy(() -> aircraftService.createAircraft(request, OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("year");
    }

    @Test
    void updateKeepingTheSameCodeDoesNotCountAsADuplicate() throws Exception {
        when(aircraftRepository.findById(5L)).thenReturn(Optional.of(aircraft("A321")));

        AircraftResponse result = aircraftService.updateAircraft(5L, request("A321", 120, 100, 20), OWNER_ID);

        assertThat(result.getSeatingCapacity()).isEqualTo(120);
        verify(aircraftRepository, never()).existsByCode(any());
    }

    @Test
    void updateCannotTakeACodeAnotherAircraftUses() {
        when(aircraftRepository.findById(5L)).thenReturn(Optional.of(aircraft("A321")));
        when(aircraftRepository.existsByCode("B737")).thenReturn(true);

        assertThatThrownBy(() -> aircraftService.updateAircraft(5L, request("B737", 100, 80, 20), OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("B737");
        verify(aircraftRepository, never()).save(any());
    }

    @Test
    void updateAndDeleteFailOnAMissingAircraft() {
        when(aircraftRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aircraftService.updateAircraft(5L, request("A321", 100, 80, 20), OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> aircraftService.deleteAircraft(5L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(aircraftRepository, never()).delete(any());
    }

    private static AircraftRequest request(String code, Integer capacity, Integer economy, Integer business) {
        AircraftRequest request = new AircraftRequest();
        request.setCode(code);
        request.setModel("Model");
        request.setManufacturer("Airbus");
        request.setSeatingCapacity(capacity);
        request.setEconomySeats(economy);
        request.setBusinessSeats(business);
        request.setStatus(AircraftStatus.ACTIVE);
        request.setIsAvailable(true);
        return request;
    }

    private Aircraft aircraft(String code) {
        return Aircraft.builder()
                .id(5L)
                .code(code)
                .seatingCapacity(100)
                .status(AircraftStatus.ACTIVE)
                .isAvailable(true)
                .airline(airline)
                .build();
    }
}
