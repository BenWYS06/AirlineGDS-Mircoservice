package com.ben.services.service.impl;

import com.ben.common_lib.enums.FlightStatus;
import com.ben.common_lib.payload.request.FlightRequest;
import com.ben.common_lib.payload.response.AircraftResponse;
import com.ben.common_lib.payload.response.AirlineResponse;
import com.ben.common_lib.payload.response.AirportResponse;
import com.ben.common_lib.payload.response.FlightResponse;
import com.ben.services.client.AirlineClient;
import com.ben.services.client.LocationClient;
import com.ben.services.model.Flight;
import com.ben.services.repository.FlightRepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FlightServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final Long AIRLINE_ID = 1L;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private AirlineClient airlineClient;

    @Mock
    private LocationClient locationClient;

    @InjectMocks
    private FlightServiceImpl flightService;

    @Test
    void createTakesTheAirlineFromTheOwnerNotFromTheRequest() throws Exception {
        remoteServicesAnswer();
        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> invocation.getArgument(0));
        FlightRequest request = request("VN100", 5L);
        request.setAirlineId(99L);

        FlightResponse result = flightService.createFlight(USER_ID, request);

        ArgumentCaptor<Flight> saved = ArgumentCaptor.forClass(Flight.class);
        verify(flightRepository).save(saved.capture());
        assertThat(saved.getValue().getAirlineId()).isEqualTo(AIRLINE_ID);
        assertThat(saved.getValue().getStatus()).isEqualTo(FlightStatus.SCHEDULED);
        assertThat(result.getFlightNumber()).isEqualTo("VN100");
    }

    @Test
    void createRejectsAFlightNumberAlreadyInUse() {
        when(flightRepository.existsByFlightNumber("VN100")).thenReturn(true);

        assertThatThrownBy(() -> flightService.createFlight(USER_ID, request("VN100", 5L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VN100");
        verify(flightRepository, never()).save(any());
    }

    @Test
    void createFailsWhenTheUserOwnsNoAirline() {
        FeignException.NotFound notFound = mock(FeignException.NotFound.class);
        when(airlineClient.getAirlineByOwner(USER_ID)).thenThrow(notFound);

        assertThatThrownBy(() -> flightService.createFlight(USER_ID, request("VN100", 5L)))
                .isInstanceOf(EntityNotFoundException.class);
        verify(flightRepository, never()).save(any());
    }

    @Test
    void createFailsWhenTheAircraftDoesNotExist() {
        when(airlineClient.getAirlineByOwner(USER_ID)).thenReturn(airline());
        FeignException.NotFound notFound = mock(FeignException.NotFound.class);
        when(airlineClient.getAircraftById(5L)).thenThrow(notFound);

        assertThatThrownBy(() -> flightService.createFlight(USER_ID, request("VN100", 5L)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Aircraft");
        verify(flightRepository, never()).save(any());
    }

    @Test
    void anOutageOfAirlineCoreIsNotReportedAsNotFound() {
        FeignException.ServiceUnavailable unavailable = mock(FeignException.ServiceUnavailable.class);
        when(airlineClient.getAirlineByOwner(USER_ID)).thenThrow(unavailable);

        assertThatThrownBy(() -> flightService.createFlight(USER_ID, request("VN100", 5L)))
                .isNotInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("airline-core-service");
    }

    @Test
    void bulkCreateSkipsExistingNumbersAndChecksEachAircraftOnce() throws Exception {
        remoteServicesAnswer();
        when(flightRepository.findExistingFlightNumbers(anyList())).thenReturn(Set.of("VN100"));
        when(flightRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<FlightResponse> result = flightService.createFlights(USER_ID, List.of(
                request("VN100", 5L), request("VN101", 5L), request("VN102", 5L)));

        assertThat(result).extracting(FlightResponse::getFlightNumber).containsExactly("VN101", "VN102");
        // One check before saving, one lookup for the responses: never once per flight
        verify(airlineClient, times(2)).getAircraftById(5L);
    }

    @Test
    void updateRejectsANumberAnotherFlightUses() {
        when(flightRepository.findById(3L)).thenReturn(Optional.of(flight(3L)));
        when(flightRepository.existsByFlightNumberAndIdNot("VN200", 3L)).thenReturn(true);

        assertThatThrownBy(() -> flightService.updateFlight(3L, request("VN200", 5L)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(flightRepository, never()).save(any());
    }

    @Test
    void changeStatusSetsTheNewStatus() throws Exception {
        remoteServicesAnswer();
        when(flightRepository.findById(3L)).thenReturn(Optional.of(flight(3L)));

        FlightResponse result = flightService.changeStatus(3L, FlightStatus.CANCELLED);

        assertThat(result.getStatus()).isEqualTo(FlightStatus.CANCELLED);
    }

    @Test
    void missingFlightGivesNotFound() {
        when(flightRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightService.getFlightById(3L)).isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> flightService.deleteFlight(3L)).isInstanceOf(EntityNotFoundException.class);
        verify(flightRepository, never()).delete(any());
    }

    @Test
    void batchLookupWithoutIdsCallsNothing() {
        assertThat(flightService.getFlightsByIds(List.of())).isEmpty();
        assertThat(flightService.getFlightsByIds(null)).isEmpty();
        verifyNoInteractions(flightRepository, airlineClient, locationClient);
    }

    @Test
    void batchLookupFetchesEachRemoteRecordOnce() {
        remoteServicesAnswer();
        when(flightRepository.findAllById(List.of(3L, 4L))).thenReturn(List.of(flight(3L), flight(4L)));

        Map<Long, FlightResponse> result = flightService.getFlightsByIds(List.of(3L, 4L));

        assertThat(result).containsOnlyKeys(3L, 4L);
        verify(airlineClient, times(1)).getAirlineById(AIRLINE_ID);
        verify(airlineClient, times(1)).getAircraftById(5L);
        verify(locationClient, times(1)).getAirportById(10L);
        verify(locationClient, times(1)).getAirportById(20L);
    }

    private void remoteServicesAnswer() {
        when(airlineClient.getAirlineByOwner(USER_ID)).thenReturn(airline());
        when(airlineClient.getAirlineById(anyLong())).thenReturn(airline());
        when(airlineClient.getAircraftById(anyLong())).thenReturn(new AircraftResponse());
        when(locationClient.getAirportById(anyLong())).thenReturn(new AirportResponse());
    }

    private static AirlineResponse airline() {
        return AirlineResponse.builder().id(AIRLINE_ID).name("Airline").build();
    }

    private static FlightRequest request(String number, Long aircraftId) {
        return FlightRequest.builder()
                .flightNumber(number)
                .aircraftId(aircraftId)
                .departureAirportId(10L)
                .arrivalAirportId(20L)
                .build();
    }

    private static Flight flight(Long id) {
        return Flight.builder()
                .id(id)
                .flightNumber("VN" + id)
                .airlineId(AIRLINE_ID)
                .aircraftId(5L)
                .departureAirportId(10L)
                .arrivalAirportId(20L)
                .status(FlightStatus.SCHEDULED)
                .build();
    }
}
