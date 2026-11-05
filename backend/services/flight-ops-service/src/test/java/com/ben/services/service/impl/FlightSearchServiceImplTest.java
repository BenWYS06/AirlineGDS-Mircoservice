package com.ben.services.service.impl;

import com.ben.common_lib.enums.CabinClassType;
import com.ben.common_lib.payload.request.FlightSearchRequest;
import com.ben.common_lib.payload.response.AircraftResponse;
import com.ben.common_lib.payload.response.AirlineResponse;
import com.ben.common_lib.payload.response.AirportResponse;
import com.ben.common_lib.payload.response.CabinClassResponse;
import com.ben.common_lib.payload.response.FareResponse;
import com.ben.common_lib.payload.response.FlightInstanceResponse;
import com.ben.services.client.AirlineClient;
import com.ben.services.client.LocationClient;
import com.ben.services.client.PricingClient;
import com.ben.services.client.SeatClient;
import com.ben.services.model.Flight;
import com.ben.services.model.FlightInstance;
import com.ben.services.repository.FlightInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Search must degrade, not fail: a bad flight or a service that is down drops that result, never the whole page
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FlightSearchServiceImplTest {

    private static final Long AIRCRAFT_ID = 5L;
    private static final Long CABIN_ID = 30L;
    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);

    @Mock
    private FlightInstanceRepository flightInstanceRepository;

    @Mock
    private LocationClient locationClient;

    @Mock
    private AirlineClient airlineClient;

    @Mock
    private PricingClient pricingClient;

    @Mock
    private SeatClient seatClient;

    @InjectMocks
    private FlightSearchServiceImpl flightSearchService;

    @BeforeEach
    void remoteServicesAnswer() {
        when(airlineClient.getAirlineById(anyLong())).thenReturn(AirlineResponse.builder().name("Airline").build());
        when(airlineClient.getAircraftById(anyLong())).thenReturn(new AircraftResponse());
        when(locationClient.getAirportById(anyLong())).thenReturn(new AirportResponse());
        when(seatClient.getCabinClassByAircraftIdAndName(CabinClassType.ECONOMY, AIRCRAFT_ID))
                .thenReturn(CabinClassResponse.builder().id(CABIN_ID).build());
    }

    @Test
    void noMatchInTheDatabaseCallsNoOtherService() {
        databaseReturns();

        Page<FlightInstanceResponse> result = flightSearchService.searchFlights(search(null, null), FIRST_PAGE);

        assertThat(result).isEmpty();
        verifyNoInteractions(seatClient, pricingClient, airlineClient, locationClient);
    }

    @Test
    void returnsTheFareOfTheRequestedCabin() {
        databaseReturns(instance(50L, 3L));
        fareIs(3L, 100.0);

        Page<FlightInstanceResponse> result = flightSearchService.searchFlights(search(null, null), FIRST_PAGE);

        assertThat(result.getContent()).singleElement()
                .satisfies(flight -> assertThat(flight.getFare().getTotalPrice()).isEqualTo(100.0));
    }

    @Test
    void withoutACabinNoFareIsLookedUp() {
        databaseReturns(instance(50L, 3L));
        FlightSearchRequest request = search(null, null);
        request.setCabinClass(null);

        Page<FlightInstanceResponse> result = flightSearchService.searchFlights(request, FIRST_PAGE);

        assertThat(result.getContent()).singleElement()
                .satisfies(flight -> assertThat(flight.getFare()).isNull());
        verifyNoInteractions(seatClient, pricingClient);
    }

    @Test
    void dropsAFlightWhoseAircraftLacksTheCabin() {
        databaseReturns(instance(50L, 3L));
        when(seatClient.getCabinClassByAircraftIdAndName(CabinClassType.ECONOMY, AIRCRAFT_ID)).thenReturn(null);

        assertThat(flightSearchService.searchFlights(search(null, null), FIRST_PAGE)).isEmpty();
        verifyNoInteractions(pricingClient);
    }

    @Test
    void seatServiceDownDropsTheFlightInsteadOfFailingTheSearch() {
        databaseReturns(instance(50L, 3L));
        when(seatClient.getCabinClassByAircraftIdAndName(CabinClassType.ECONOMY, AIRCRAFT_ID))
                .thenThrow(new IllegalStateException("seat-service down"));

        assertThat(flightSearchService.searchFlights(search(null, null), FIRST_PAGE)).isEmpty();
    }

    @Test
    void dropsAFlightWithoutAFare() {
        databaseReturns(instance(50L, 3L));
        when(pricingClient.getLowestFareForFlightAndCabinClass(3L, CABIN_ID)).thenReturn(null);

        assertThat(flightSearchService.searchFlights(search(null, null), FIRST_PAGE)).isEmpty();
    }

    @Test
    void priceRangeIncludesItsLimitsAndDropsWhatIsOutside() {
        databaseReturns(instance(50L, 3L), instance(51L, 4L), instance(52L, 6L), instance(53L, 8L));
        fareIs(3L, 99.0);
        fareIs(4L, 100.0);
        fareIs(6L, 200.0);
        fareIs(8L, 201.0);

        Page<FlightInstanceResponse> result = flightSearchService.searchFlights(search(100.0, 200.0), FIRST_PAGE);

        assertThat(result.getContent()).extracting(FlightInstanceResponse::getId).containsExactly(51L, 52L);
    }

    @Test
    void aPriceFilterDropsAFareWithoutAPrice() {
        databaseReturns(instance(50L, 3L));
        when(pricingClient.getLowestFareForFlightAndCabinClass(3L, CABIN_ID)).thenReturn(new FareResponse());

        assertThat(flightSearchService.searchFlights(search(null, 200.0), FIRST_PAGE)).isEmpty();
    }

    @Test
    void aFailedLookupDropsOnlyThatFlight() {
        FlightInstance broken = instance(50L, 3L);
        broken.setAirlineId(99L);
        databaseReturns(broken, instance(51L, 4L));
        fareIs(3L, 100.0);
        fareIs(4L, 100.0);
        when(airlineClient.getAirlineById(99L)).thenThrow(new IllegalStateException("airline-core down"));

        Page<FlightInstanceResponse> result = flightSearchService.searchFlights(search(null, null), FIRST_PAGE);

        assertThat(result.getContent()).extracting(FlightInstanceResponse::getId).containsExactly(51L);
    }

    @Test
    void enrichmentFetchesEachRemoteRecordOncePerSearch() {
        databaseReturns(instance(50L, 3L), instance(51L, 4L));
        fareIs(3L, 100.0);
        fareIs(4L, 100.0);

        flightSearchService.searchFlights(search(null, null), FIRST_PAGE);

        verify(airlineClient, times(1)).getAirlineById(1L);
        verify(airlineClient, times(1)).getAircraftById(AIRCRAFT_ID);
        verify(locationClient, times(1)).getAirportById(10L);
        verify(locationClient, times(1)).getAirportById(20L);
    }

    @Test
    void sortsByDepartureAscendingByDefault() {
        databaseReturns();

        flightSearchService.searchFlights(search(null, null), FIRST_PAGE);

        assertThat(sortUsed().getOrderFor("departureDateTime").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void sortsByArrivalDescendingWhenAsked() {
        databaseReturns();
        FlightSearchRequest request = search(null, null);
        request.setSortBy("ARRIVAL");
        request.setSortOrder("desc");

        flightSearchService.searchFlights(request, FIRST_PAGE);

        assertThat(sortUsed().getOrderFor("arrivalDateTime").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void sortingByPriceFallsBackToDeparture() {
        databaseReturns();
        FlightSearchRequest request = search(null, null);
        request.setSortBy("price");

        flightSearchService.searchFlights(request, FIRST_PAGE);

        assertThat(sortUsed().getOrderFor("departureDateTime")).isNotNull();
    }

    @SuppressWarnings("unchecked")
    private Sort sortUsed() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(flightInstanceRepository).findAll(any(Specification.class), pageable.capture());
        return pageable.getValue().getSort();
    }

    @SuppressWarnings("unchecked")
    private void databaseReturns(FlightInstance... instances) {
        when(flightInstanceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(instances)));
    }

    private void fareIs(Long flightId, Double price) {
        when(pricingClient.getLowestFareForFlightAndCabinClass(flightId, CABIN_ID))
                .thenReturn(FareResponse.builder().totalPrice(price).build());
    }

    private static FlightSearchRequest search(Double minPrice, Double maxPrice) {
        return FlightSearchRequest.builder()
                .departureDate(LocalDate.of(2026, 12, 25))
                .passengers(1)
                .cabinClass(CabinClassType.ECONOMY)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .build();
    }

    private static FlightInstance instance(Long id, Long flightId) {
        return FlightInstance.builder()
                .id(id)
                .flight(Flight.builder().id(flightId).flightNumber("VN" + flightId).aircraftId(AIRCRAFT_ID).build())
                .airlineId(1L)
                .departureAirportId(10L)
                .arrivalAirportId(20L)
                .departureDateTime(LocalDateTime.of(2026, 12, 25, 8, 0))
                .arrivalDateTime(LocalDateTime.of(2026, 12, 25, 10, 0))
                .build();
    }
}
