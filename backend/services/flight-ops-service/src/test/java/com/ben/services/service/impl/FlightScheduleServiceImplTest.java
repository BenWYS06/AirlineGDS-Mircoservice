package com.ben.services.service.impl;

import com.ben.common_lib.payload.request.FlightInstanceRequest;
import com.ben.common_lib.payload.request.FlightScheduleRequest;
import com.ben.common_lib.payload.response.AircraftResponse;
import com.ben.common_lib.payload.response.AirportResponse;
import com.ben.services.Integration.AirlineIntegrationService;
import com.ben.services.client.LocationClient;
import com.ben.services.model.Flight;
import com.ben.services.model.FlightSchedule;
import com.ben.services.repository.FlightRepository;
import com.ben.services.repository.FlightScheduleRepository;
import com.ben.services.service.FlightInstanceService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FlightScheduleServiceImplTest {

    private static final Long USER_ID = 7L;
    // A Monday, so the week below is easy to read
    private static final LocalDate MONDAY = LocalDate.of(2026, 12, 7);

    @Mock
    private FlightScheduleRepository flightScheduleRepository;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private FlightInstanceService flightInstanceService;

    @Mock
    private AirlineIntegrationService airlineIntegrationService;

    @Mock
    private LocationClient locationClient;

    @InjectMocks
    private FlightScheduleServiceImpl flightScheduleService;

    // The service reuses one request object in its loop, so each departure is copied when the call happens
    private final List<LocalDateTime> departures = new ArrayList<>();

    @BeforeEach
    void flightAndRemoteServicesExist() throws Exception {
        Flight flight = Flight.builder().id(3L).flightNumber("VN3").aircraftId(5L)
                .departureAirportId(10L).arrivalAirportId(20L).build();
        when(flightRepository.findById(3L)).thenReturn(Optional.of(flight));
        when(flightScheduleRepository.save(any(FlightSchedule.class))).thenAnswer(invocation -> {
            FlightSchedule schedule = invocation.getArgument(0);
            schedule.setId(70L);
            return schedule;
        });
        when(airlineIntegrationService.getAircraftById(5L))
                .thenReturn(AircraftResponse.builder().totalSeats(180).build());
        when(locationClient.getAirportById(anyLong())).thenReturn(new AirportResponse());
        when(flightInstanceService.createFlightInstanceWithCabins(eq(USER_ID), any(FlightInstanceRequest.class)))
                .thenAnswer(invocation -> {
                    FlightInstanceRequest request = invocation.getArgument(1);
                    departures.add(request.getDepartureDateTime());
                    return null;
                });
    }

    @Test
    void createsOneInstanceForEachOperatingDayInTheRange() throws Exception {
        flightScheduleService.createFlightSchedule(USER_ID,
                request(MONDAY, MONDAY.plusDays(6), DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));

        assertThat(departures).containsExactly(
                MONDAY.atTime(8, 0),
                MONDAY.plusDays(2).atTime(8, 0));
    }

    @Test
    void theFirstAndLastDayOfTheRangeBothCount() throws Exception {
        flightScheduleService.createFlightSchedule(USER_ID,
                request(MONDAY, MONDAY.plusDays(7), DayOfWeek.MONDAY));

        assertThat(departures).containsExactly(
                MONDAY.atTime(8, 0),
                MONDAY.plusDays(7).atTime(8, 0));
    }

    @Test
    void noOperatingDayInTheRangeCreatesNoInstance() throws Exception {
        flightScheduleService.createFlightSchedule(USER_ID, request(MONDAY, MONDAY, DayOfWeek.SUNDAY));

        assertThat(departures).isEmpty();
        verify(flightScheduleRepository).save(any());
    }

    @Test
    void rejectsAnEndDateBeforeTheStartDate() {
        assertThatThrownBy(() -> flightScheduleService.createFlightSchedule(USER_ID,
                request(MONDAY, MONDAY.minusDays(1), DayOfWeek.MONDAY)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(flightScheduleRepository, never()).save(any());
        verifyNoInteractions(flightInstanceService);
    }

    @Test
    void rejectsAMissingFlight() {
        when(flightRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightScheduleService.createFlightSchedule(USER_ID,
                request(MONDAY, MONDAY, DayOfWeek.MONDAY)))
                .isInstanceOf(EntityNotFoundException.class);
        verify(flightScheduleRepository, never()).save(any());
    }

    @Test
    void missingScheduleGivesNotFound() {
        when(flightScheduleRepository.findById(70L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightScheduleService.getFlightScheduleById(70L))
                .isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> flightScheduleService.deleteFlightSchedule(70L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(flightScheduleRepository, never()).delete(any());
    }

    private static FlightScheduleRequest request(LocalDate start, LocalDate end, DayOfWeek... days) {
        return FlightScheduleRequest.builder()
                .flightId(3L)
                .departureTime(LocalTime.of(8, 0))
                .arrivalTime(LocalTime.of(10, 0))
                .startDate(start)
                .endDate(end)
                .operatingDays(List.of(days))
                .build();
    }
}
