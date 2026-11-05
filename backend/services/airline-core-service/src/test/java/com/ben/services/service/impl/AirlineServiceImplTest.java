package com.ben.services.service.impl;

import com.ben.common_lib.enums.AirlineStatus;
import com.ben.common_lib.payload.request.AirlineRequest;
import com.ben.common_lib.payload.response.AirlineDropdownItem;
import com.ben.common_lib.payload.response.AirlineResponse;
import com.ben.services.model.Airline;
import com.ben.services.repository.AirlineRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// The owner comes from the trusted X-User-Id header, so owner-scoped calls must use it, never a client value
@ExtendWith(MockitoExtension.class)
class AirlineServiceImplTest {

    private static final Long OWNER_ID = 7L;

    @Mock
    private AirlineRepository airlineRepository;

    @InjectMocks
    private AirlineServiceImpl airlineService;

    @Test
    void createsTheAirlineForTheCallingOwner() {
        when(airlineRepository.save(any(Airline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AirlineResponse result = airlineService.createAirline(request("VN"), OWNER_ID);

        assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(result.getIataCode()).isEqualTo("VN");
    }

    @Test
    void ownerWithoutAnAirlineGetsNotFound() {
        when(airlineRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airlineService.getAirlineByOwner(OWNER_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateChangesTheOwnersAirlineAndKeepsItsOwner() {
        Airline airline = airline(1L);
        when(airlineRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(airline));
        when(airlineRepository.save(airline)).thenReturn(airline);

        AirlineResponse result = airlineService.updateAirline(request("QH"), OWNER_ID);

        assertThat(result.getIataCode()).isEqualTo("QH");
        assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);
    }

    @Test
    void updateFailsWhenTheOwnerHasNoAirline() {
        when(airlineRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airlineService.updateAirline(request("QH"), OWNER_ID))
                .isInstanceOf(EntityNotFoundException.class);
        verify(airlineRepository, never()).save(any());
    }

    @Test
    void deleteFailsWhenTheOwnerHasNoAirline() {
        when(airlineRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airlineService.deleteAirline(1L, OWNER_ID))
                .isInstanceOf(EntityNotFoundException.class);
        verify(airlineRepository, never()).delete(any());
    }

    @Test
    void adminChangesTheStatus() {
        Airline airline = airline(1L);
        when(airlineRepository.findById(1L)).thenReturn(Optional.of(airline));
        when(airlineRepository.save(airline)).thenReturn(airline);

        AirlineResponse result = airlineService.changeStatusByAdmin(1L, AirlineStatus.BANNED);

        assertThat(result.getStatus()).isEqualTo(AirlineStatus.BANNED);
    }

    @Test
    void adminCannotChangeTheStatusOfAMissingAirline() {
        when(airlineRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airlineService.changeStatusByAdmin(1L, AirlineStatus.BANNED))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void dropdownAsksOnlyForActiveAirlines() {
        when(airlineRepository.findByStatus(AirlineStatus.ACTIVE)).thenReturn(List.of(airline(1L)));

        List<AirlineDropdownItem> result = airlineService.getAirlinesForDropdown();

        assertThat(result).singleElement()
                .satisfies(item -> assertThat(item.getId()).isEqualTo(1L));
    }

    private static AirlineRequest request(String iataCode) {
        return AirlineRequest.builder()
                .iataCode(iataCode)
                .icaoCode("HVN")
                .name("Airline")
                .country("Vietnam")
                .status(AirlineStatus.ACTIVE)
                .build();
    }

    private static Airline airline(Long id) {
        return Airline.builder()
                .id(id)
                .iataCode("VN")
                .name("Airline")
                .status(AirlineStatus.ACTIVE)
                .ownerId(OWNER_ID)
                .build();
    }
}
