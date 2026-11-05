package com.ben.services.service.impl;

import com.ben.common_lib.enums.FlightStatus;
import com.ben.common_lib.event.FlightInstanceCreatedEvent;
import com.ben.common_lib.exception.ResourceNotFoundException;
import com.ben.common_lib.payload.request.FlightInstanceRequest;
import com.ben.common_lib.payload.response.AircraftResponse;
import com.ben.common_lib.payload.response.AirlineResponse;
import com.ben.common_lib.payload.response.AirportResponse;
import com.ben.common_lib.payload.response.FlightInstanceResponse;
import com.ben.services.client.AirlineClient;
import com.ben.services.client.LocationClient;
import com.ben.services.event.FlightInstanceEventProducer;
import com.ben.services.model.Flight;
import com.ben.services.model.FlightInstance;
import com.ben.services.repository.FlightInstanceRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FlightInstanceServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final Long AIRLINE_ID = 1L;

    @Mock
    private FlightInstanceRepository flightInstanceRepository;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private AirlineClient airlineClient;

    @Mock
    private FlightInstanceEventProducer flightInstanceEventProducer;

    @Mock
    private LocationClient locationClient;

    @InjectMocks
    private FlightInstanceServiceImpl flightInstanceService;

    private final Flight flight = Flight.builder().id(3L).flightNumber("VN3").aircraftId(5L).build();

    @Test
    void createTakesSeatsFromTheAircraftAndTheAirlineFromTheOwner() throws Exception {
        remoteServicesAnswer();
        flightExistsAndSaveAssignsId();

        // The client's seat counts and airline must lose to the aircraft and the owner's airline
        FlightInstanceRequest request = request();
        request.setTotalSeats(1);
        request.setAvailableSeats(1);
        request.setAirlineId(99L);
        FlightInstanceResponse result = flightInstanceService.createFlightInstanceWithCabins(USER_ID, request);

        assertThat(result.getAirlineId()).isEqualTo(AIRLINE_ID);
        assertThat(result.getTotalSeats()).isEqualTo(180);
        assertThat(result.getAvailableSeats()).isEqualTo(180);
    }

    @Test
    void createTellsSeatServiceWhichCabinsToBuild() throws Exception {
        remoteServicesAnswer();
        flightExistsAndSaveAssignsId();

        flightInstanceService.createFlightInstanceWithCabins(USER_ID, request());

        ArgumentCaptor<FlightInstanceCreatedEvent> event = ArgumentCaptor.forClass(FlightInstanceCreatedEvent.class);
        verify(flightInstanceEventProducer).sendFlightInstanceCreated(event.capture());
        assertThat(event.getValue().getFlightInstanceId()).isEqualTo(50L);
        assertThat(event.getValue().getFlightId()).isEqualTo(3L);
        assertThat(event.getValue().getAircraftId()).isEqualTo(5L);
    }

    @Test
    void createStopsWhenTheFlightIsMissing() {
        when(airlineClient.getAirlineByOwner(USER_ID)).thenReturn(airline());
        when(flightRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightInstanceService.createFlightInstanceWithCabins(USER_ID, request()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(flightInstanceRepository, never()).save(any());
        verifyNoInteractions(flightInstanceEventProducer);
    }

    @Test
    void createStopsWhenTheAircraftIsMissing() {
        when(airlineClient.getAirlineByOwner(USER_ID)).thenReturn(airline());
        when(flightRepository.findById(3L)).thenReturn(Optional.of(flight));
        FeignException.NotFound notFound = mock(FeignException.NotFound.class);
        when(airlineClient.getAircraftById(5L)).thenThrow(notFound);

        assertThatThrownBy(() -> flightInstanceService.createFlightInstanceWithCabins(USER_ID, request()))
                .isInstanceOf(EntityNotFoundException.class);
        verify(flightInstanceRepository, never()).save(any());
        verifyNoInteractions(flightInstanceEventProducer);
    }

    @Test
    void createFailsWhenTheUserOwnsNoAirline() {
        FeignException.NotFound notFound = mock(FeignException.NotFound.class);
        when(airlineClient.getAirlineByOwner(USER_ID)).thenThrow(notFound);

        assertThatThrownBy(() -> flightInstanceService.createFlightInstanceWithCabins(USER_ID, request()))
                .isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(flightRepository, flightInstanceEventProducer);
    }

    @Test
    void dateFilterCoversExactlyThatDay() {
        when(airlineClient.getAirlineByOwner(USER_ID)).thenReturn(airline());
        Pageable pageable = PageRequest.of(0, 10);
        when(flightInstanceRepository.findByAirlineIdWithFilters(
                any(), any(), any(), any(), any(), any(), eq(pageable))).thenReturn(Page.empty(pageable));

        flightInstanceService.getByAirlineId(USER_ID, null, null, null, LocalDate.of(2026, 12, 25), pageable);

        verify(flightInstanceRepository).findByAirlineIdWithFilters(AIRLINE_ID, null, null, null,
                LocalDateTime.of(2026, 12, 25, 0, 0), LocalDateTime.of(2026, 12, 26, 0, 0), pageable);
    }

    @Test
    void noDateMeansNoDayBounds() {
        when(airlineClient.getAirlineByOwner(USER_ID)).thenReturn(airline());
        Pageable pageable = PageRequest.of(0, 10);
        when(flightInstanceRepository.findByAirlineIdWithFilters(
                any(), any(), any(), any(), any(), any(), eq(pageable))).thenReturn(Page.empty(pageable));

        flightInstanceService.getByAirlineId(USER_ID, 10L, 20L, 3L, null, pageable);

        verify(flightInstanceRepository).findByAirlineIdWithFilters(
                eq(AIRLINE_ID), eq(10L), eq(20L), eq(3L), isNull(), isNull(), eq(pageable));
    }

    @Test
    void missingInstanceGivesNotFound() {
        when(flightInstanceRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightInstanceService.getFlightInstanceById(50L))
                .isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> flightInstanceService.deleteFlightInstance(50L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(flightInstanceRepository, never()).delete(any(FlightInstance.class));
    }

    @Test
    void batchLookupWithoutIdsCallsNothing() {
        assertThat(flightInstanceService.getFlightInstancesByIds(List.of())).isEmpty();
        assertThat(flightInstanceService.getFlightInstancesByIds(null)).isEmpty();
        verifyNoInteractions(flightInstanceRepository, airlineClient, locationClient);
    }

    @Test
    void batchLookupFetchesEachRemoteRecordOnce() {
        remoteServicesAnswer();
        when(flightInstanceRepository.findAllByIdInWithFlight(List.of(50L, 51L)))
                .thenReturn(List.of(instance(50L), instance(51L)));

        Map<Long, FlightInstanceResponse> result =
                flightInstanceService.getFlightInstancesByIds(List.of(50L, 51L));

        assertThat(result).containsOnlyKeys(50L, 51L);
        verify(airlineClient, times(1)).getAirlineById(AIRLINE_ID);
        verify(airlineClient, times(1)).getAircraftById(5L);
        verify(locationClient, times(1)).getAirportById(10L);
        verify(locationClient, times(1)).getAirportById(20L);
    }

    private void flightExistsAndSaveAssignsId() {
        when(flightRepository.findById(3L)).thenReturn(Optional.of(flight));
        when(flightInstanceRepository.save(any(FlightInstance.class))).thenAnswer(invocation -> {
            FlightInstance instance = invocation.getArgument(0);
            instance.setId(50L);
            return instance;
        });
    }

    private void remoteServicesAnswer() {
        when(airlineClient.getAirlineByOwner(USER_ID)).thenReturn(airline());
        when(airlineClient.getAirlineById(anyLong())).thenReturn(airline());
        when(airlineClient.getAircraftById(anyLong())).thenReturn(AircraftResponse.builder().totalSeats(180).build());
        when(locationClient.getAirportById(anyLong())).thenReturn(new AirportResponse());
    }

    private static AirlineResponse airline() {
        return AirlineResponse.builder().id(AIRLINE_ID).name("Airline").build();
    }

    private static FlightInstanceRequest request() {
        return FlightInstanceRequest.builder()
                .flightId(3L)
                .departureAirportId(10L)
                .arrivalAirportId(20L)
                .departureDateTime(LocalDateTime.of(2026, 12, 25, 8, 0))
                .arrivalDateTime(LocalDateTime.of(2026, 12, 25, 10, 0))
                .status(FlightStatus.SCHEDULED)
                .build();
    }

    private FlightInstance instance(Long id) {
        return FlightInstance.builder()
                .id(id)
                .flight(flight)
                .airlineId(AIRLINE_ID)
                .departureAirportId(10L)
                .arrivalAirportId(20L)
                .departureDateTime(LocalDateTime.of(2026, 12, 25, 8, 0))
                .arrivalDateTime(LocalDateTime.of(2026, 12, 25, 10, 0))
                .build();
    }
}
