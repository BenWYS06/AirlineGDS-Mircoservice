package com.ben.services.event.publisher;

import com.ben.common_lib.dto.UserDTO;
import com.ben.common_lib.event.BookingCancelledEvent;
import com.ben.common_lib.event.BookingConfirmedEvent;
import com.ben.common_lib.event.PassengerNotificationData;
import com.ben.common_lib.payload.response.AirportResponse;
import com.ben.common_lib.payload.response.BaggagePolicyResponse;
import com.ben.common_lib.payload.response.CityResponse;
import com.ben.common_lib.payload.response.FareResponse;
import com.ben.common_lib.payload.response.FlightInstanceResponse;
import com.ben.common_lib.event.PaymentCompletedEvent;
import com.ben.services.model.Booking;
import com.ben.services.model.Passenger;
import com.ben.services.model.Ticket;
import com.ben.services.outbox.OutboxWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds booking events and puts them in the outbox. Must be called inside the transaction that changes the
 * booking, so the event and the status change commit together; OutboxRelay sends them (key = bookingId).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEventProducer {

    private final OutboxWriter outboxWriter;

    // booking.cancelled → seat-service releases the booking's HELD seats (guaranteed compensation, step 6)
    public void sendBookingCancelled(Long bookingId, String finalStatus) {
        outboxWriter.add("booking.cancelled", bookingId, BookingCancelledEvent.builder()
                .bookingId(bookingId)
                .finalStatus(finalStatus)
                .build());
    }

    public void sendBookingConfirmed(Booking booking,
                                     PaymentCompletedEvent payment,
                                     FlightInstanceResponse flight,
                                     FareResponse fare,
                                     UserDTO user) {

        // ── Passengers ────────────────────────────────────────────────────────
        // Build a passengerId → ticketNumber lookup from the Ticket relation
        Map<Long, String> ticketByPassenger = booking.getTickets().stream()
                .filter(t -> t.getPassenger() != null)
                .collect(Collectors.toMap(
                        t -> t.getPassenger().getId(),
                        Ticket::getTicketNumber,
                        (a, b) -> a   // keep first if duplicate
                ));

        List<PassengerNotificationData> passengers = booking.getPassengers().stream()
                .map(p -> PassengerNotificationData.builder()
                        .firstName(p.getFirstName())
                        .lastName(p.getLastName())
                        .ticketNumber(ticketByPassenger.getOrDefault(p.getId(), "N/A"))
                        .passportNumber(p.getPassportNumber())
                        .nationality(p.getNationality())
                        .gender(p.getGender() != null ? p.getGender().name() : "")
                        .adult(p.isAdult())
                        .frequentFlyerNumber(p.getFrequentFlyerNumber())
                        .requiresWheelchair(Boolean.TRUE.equals(p.getRequiresWheelchairAssistance()))
                        .dietaryPreferences(p.getDietaryPreferences())
                        .build())
                .collect(Collectors.toList());

        // ── Contact Info ──────────────────────────────────────────────────────
        // Prefer booking.contactInfo; fall back to first passenger
        String contactEmail = booking.getContactInfo() != null
                ? booking.getContactInfo().getEmail() : null;
        String contactPhone = booking.getContactInfo() != null
                ? booking.getContactInfo().getPhone() : null;

        if (contactEmail == null || contactEmail.isBlank()) {
            contactEmail = booking.getPassengers().stream()
                    .map(Passenger::getEmail)
                    .filter(e -> e != null && !e.isBlank())
                    .findFirst().orElse(null);
        }
        if (contactPhone == null || contactPhone.isBlank()) {
            contactPhone = booking.getPassengers().stream()
                    .map(Passenger::getPhone)
                    .filter(p -> p != null && !p.isBlank())
                    .findFirst().orElse(null);
        }

        // ── Flight Details ─────────────────────────────────────────────────────
        String flightNumber   = flight != null ? flight.getFlightNumber()  : "N/A";
        String airlineName    = flight != null ? flight.getAirlineName()   : "N/A";
        String airlineLogo    = flight != null ? flight.getAirlineLogo()   : null;
        String aircraftModel  = flight != null ? flight.getAircraftModal() : null;
        String terminal       = flight != null ? flight.getTerminal()      : null;
        String gate           = flight != null ? flight.getGate()          : null;
        String duration       = flight != null ? flight.getFormattedDuration() : null;
        LocalDateTime depTime = flight != null ? flight.getDepartureDateTime() : null;
        LocalDateTime arrTime = flight != null ? flight.getArrivalDateTime()   : null;

        AirportResponse dep    = flight != null ? flight.getDepartureAirport() : null;
        CityResponse    depCity = dep != null ? dep.getCity() : null;
        String depCode         = dep != null ? dep.getIataCode()        : "N/A";
        String depName         = dep != null ? dep.getName()            : "N/A";
        String depCityName     = depCity != null ? depCity.getName()    : "N/A";
        String depCountry      = depCity != null ? depCity.getCountryName() : "N/A";

        AirportResponse arr     = flight != null ? flight.getArrivalAirport() : null;
        CityResponse    arrCity = arr != null ? arr.getCity() : null;
        String arrCode          = arr != null ? arr.getIataCode()        : "N/A";
        String arrName          = arr != null ? arr.getName()            : "N/A";
        String arrCityName      = arrCity != null ? arrCity.getName()    : "N/A";
        String arrCountry       = arrCity != null ? arrCity.getCountryName() : "N/A";

        // ── Fare & Baggage ────────────────────────────────────────────────────
        String fareName         = fare != null ? fare.getName()                     : null;
        Double baseFare         = fare != null ? fare.getBaseFare()                 : null;
        Double taxes            = fare != null ? fare.getTaxesAndFees()             : null;
        BaggagePolicyResponse bag = fare != null ? fare.getBaggagePolicy()          : null;
        Integer ciPieces        = bag  != null ? bag.getCheckInBaggagePieces()     : null;
        Double  ciWeightPer     = bag  != null ? bag.getCheckInBaggageWeightPerPiece() : null;
        Integer cbPieces        = bag  != null ? bag.getCabinBaggagePieces()        : null;
        Double  cbWeightPer     = bag  != null ? bag.getCabinBaggageWeightPerPiece() : null;

        // ── Build & Publish ───────────────────────────────────────────────────
        BookingConfirmedEvent event = BookingConfirmedEvent.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .confirmedAt(LocalDateTime.now())
                .bookingDate(booking.getBookingDate())
                .cabinClass(booking.getCabinClass() != null ? booking.getCabinClass().name() : "ECONOMY")
                .tripType(booking.getTripType() != null ? booking.getTripType().name() : "ONE_WAY")
                .flexibleTicket(booking.isFlexibleTicket())
                // Contact
                .userId(booking.getUserId())
                .userName(user != null ? user.getFullName() : "Valued Customer")
                .contactEmail(contactEmail)
                .contactPhone(contactPhone)
                // Passengers
                .passengers(passengers)
                // Flight
                .flightInstanceId(booking.getFlightInstanceId())
                .flightNumber(flightNumber)
                .airlineName(airlineName)
                .airlineLogo(airlineLogo)
                .aircraftModel(aircraftModel)
                .departureAirportCode(depCode)
                .departureAirportName(depName)
                .departureCity(depCityName)
                .departureCountry(depCountry)
                .departureTerminal(terminal)
                .departureGate(gate)
                .departureDateTime(depTime)
                .arrivalAirportCode(arrCode)
                .arrivalAirportName(arrName)
                .arrivalCity(arrCityName)
                .arrivalCountry(arrCountry)
                .arrivalDateTime(arrTime)
                .flightDuration(duration)
                // Payment
                .totalAmount(payment.getAmount())
                .currency("VND")
                .transactionId(payment.getTransactionId())
                .providerPaymentId(payment.getProviderPaymentId())
                .paymentGateway("VNPAY")
                .paidAt(payment.getPaidAt())
                // Fare breakdown
                .fareName(fareName)
                .baseFare(baseFare)
                .taxesAndFees(taxes)
                // Baggage
                .checkinBaggagePieces(ciPieces)
                .checkinBaggageWeightPerPiece(ciWeightPer)
                .cabinBaggagePieces(cbPieces)
                .cabinBaggageWeightPerPiece(cbWeightPer)
                // Policies
                .freeDateChange(fare != null ? fare.getFreeDateChange()       : null)
                .partialRefund(fare != null  ? fare.getPartialRefund()        : null)
                .fullRefund(fare != null     ? fare.getFullRefund()           : null)
                .priorityBoarding(fare != null ? fare.getPriorityBoarding()   : null)
                .loungeAccess(fare != null   ? fare.getLoungeAccess()         : null)
                .complimentaryMeals(fare != null ? fare.getComplimentaryMeals() : null)
                // Informational only: booking confirms the seats itself (sync call) before publishing this
                .seatInstanceIds(booking.getSeatInstanceIds())
                .build();

        outboxWriter.add("booking.confirmed", booking.getId(), event);
        log.info("Outbox: enriched BookingConfirmedEvent for booking={}", booking.getBookingReference());
    }
}
