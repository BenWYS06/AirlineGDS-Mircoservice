package com.ben.services.service.impl;

import com.ben.common_lib.enums.BookingStatus;
import com.ben.common_lib.enums.PaymentGateway;
import com.ben.common_lib.enums.UserRole;
import com.ben.common_lib.exception.ForbiddenException;
import com.ben.common_lib.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.ben.common_lib.payload.request.BookingRequest;
import com.ben.common_lib.payload.request.PassengerRequest;
import com.ben.common_lib.payload.request.PaymentInitiateRequest;
import com.ben.common_lib.payload.request.SeatHoldRequest;
import com.ben.common_lib.payload.response.*;
import com.ben.services.clients.*;
import com.ben.services.integration.PricingIntegrationService;
import com.ben.services.mapper.BookingMapper;
import com.ben.services.model.Booking;
import com.ben.services.model.Passenger;
import com.ben.services.repository.BookingRepository;
import com.ben.services.job.BookingExpiryJob;
import com.ben.services.service.BookingCompensator;
import com.ben.services.service.BookingService;
import com.ben.services.service.PassengerService;

import com.ben.services.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import org.springframework.data.domain.Sort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final PassengerService passengerService;
    private final TicketService ticketService;
    private final PricingIntegrationService pricingIntegrationService;
    private final PricingClient pricingClient;
    private final AncillaryClient ancillaryClient;
    private final PaymentClient paymentClient;
    private final SeatClient seatClient;
    private final FlightClient flightClient;
    private final AirlineClient airlineClient;
    private final TransactionTemplate transactionTemplate;
    private final BookingCompensator bookingCompensator;   // the one compensation used by every failure path


    /*
     * The saga's forward steps (booking-service = orchestrator). Not @Transactional on purpose: this method
     * makes several network calls (Feign). Keeping a DB transaction open while waiting on the network holds
     * connections and row locks for seconds. Only the DB writes run inside short transactions.
     *
     *   1. save booking PENDING + passengers + tickets   (local transaction, = idempotency gate)
     *   2. reads: prices                                  (before any write in another service)
     *   3. hold the seats in seat-service                 (all-or-nothing, 409 if any seat is taken)
     *   4. create the VNPay payment
     *   5. PENDING → AWAITING_PAYMENT + store the link    (conditional update)
     *   Any failure after step 1: one compensation (BookingCompensator: CANCELLED + tickets + release seats).
     *   A duplicate request (same user + flight + seats) fails at step 1 and gets the existing booking's answer.
     */
    @Override
    public PaymentInitiateResponse createBooking(BookingRequest request, Long userId) {
        log.info("Creating booking for user: {}", userId);

        List<Long> seatInstanceIds = request.getPassengers().stream()
                .map(PassengerRequest::getSeatInstanceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (seatInstanceIds.size() != new HashSet<>(seatInstanceIds).size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Each passenger needs a different seat");
        }
        // One seat per passenger (DESIGN A4); also guarantees a non-null idempotency key below
        if (seatInstanceIds.isEmpty() || seatInstanceIds.size() != request.getPassengers().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Every passenger needs exactly one seat");
        }

        // Idempotency key = (user, flight instance, this hash): the same request always gives the same key
        String seatsHash = hashSeats(seatInstanceIds);

        // Generate unique booking reference
        String bookingReference = generateBookingReference();

        // check flightExist
        FlightResponse flightResponse = flightClient.getFlightById(request.getFlightId());
        // null = flight-ops unavailable (its fallback returns null for display callers): fail fast with 503
        if (flightResponse == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Flight information is temporarily unavailable. Please retry.");
        }
        Long airlineId = flightResponse.getAirline().getId();

        // 1. Booking + passengers + tickets in one short local transaction.
        //    The INSERT is the idempotency gate: uk_booking_active rejects a second live booking with the
        //    same key, atomically, even when two identical requests arrive at the same moment.
        Booking booking;
        try {
            booking = insertPendingBooking(request, userId, bookingReference, airlineId, seatsHash);
        } catch (DataIntegrityViolationException e) {
            if (!isBookingKeyViolation(e)) {
                throw e;   // some other constraint (bad input, ...): not a duplicate request
            }
            // Duplicate request: answer from the live booking that blocked the INSERT.
            // Not found = that booking was closed in between (rare): the user simply tries again.
            Booking existing = bookingRepository.findLiveByKey(userId, request.getFlightInstanceId(), seatsHash)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Your previous booking for these seats just changed. Please try again."));
            if (!isPastPaymentGrace(existing)) {
                return answerFromExistingBooking(userId, existing);
            }
            // The old booking's link and grace are over (the job just hasn't run yet): expire it now with the
            // same compensation the job uses, which frees the key and the seats, then book again
            bookingCompensator.close(existing.getId(), BookingStatus.EXPIRED);
            try {
                booking = insertPendingBooking(request, userId, bookingReference, airlineId, seatsHash);
            } catch (DataIntegrityViolationException again) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Your previous booking for these seats just changed. Please try again.");
            }
        }
        Long bookingId = booking.getId();

        try {
            // 2. Reads before writes: price everything BEFORE holding seats, so a pricing failure has no
            //    seat hold to undo (amounts come from ids, server-side; never from the client)
            int passengerCount = request.getPassengers().size();
            Double fareTotal = pricingIntegrationService.calculateFareTotal(request.getFareId()) * passengerCount;
            Double seatPrice = seatClient.calculateSeatPrice(seatInstanceIds);
            Double ancillaryPrice = ancillaryClient.calculateAncillariesPrice(request.getAncillaryIds());
            Double mealPrice = ancillaryClient.calculateMealPrice(request.getMealIds());
            Double totalPrice = fareTotal + seatPrice + ancillaryPrice + mealPrice;

            // 3. Reserve the seats before asking for money. 409 here = someone else got a seat first.
            seatClient.holdSeats(SeatHoldRequest.builder()
                    .bookingId(bookingId)
                    // lets seat-service reject seats that belong to another flight (400)
                    .flightInstanceId(request.getFlightInstanceId())
                    .seatInstanceIds(seatInstanceIds)
                    .build());

            // 4. Initiate payment
            PaymentInitiateRequest paymentRequest = PaymentInitiateRequest.builder()
                    .userId(userId)
                    .bookingId(bookingId)
                    .amount(totalPrice)
                    .gateway(PaymentGateway.VNPAY)
                    .description("Booking: " + bookingReference)
                    .build();

            // On an outage the fallback throws 503 (never a fake link), which runs the compensation below
            PaymentInitiateResponse paymentInit = paymentClient.initiatePayment(paymentRequest, userId);

            // 5. Link ready: PENDING → AWAITING_PAYMENT, only if the booking is still PENDING.
            //    Storing the link lets a duplicate request get it back without calling payment again.
            int updated = bookingRepository.markAwaitingPayment(bookingId, paymentInit.getCheckoutUrl(),
                    paymentInit.getExpiresAt(), paymentInit.getPaymentId());
            if (updated == 0) {
                // Someone closed the booking meanwhile (e.g. the recovery job): don't hand out a link for it
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Your booking could not be completed. Please try again.");
            }

            paymentInit.setBookingId(bookingId);
            paymentInit.setBookingStatus(BookingStatus.AWAITING_PAYMENT);
            paymentInit.setReused(false);
            log.info("Booking {} created, seats held, waiting for payment", bookingReference);
            return paymentInit;

        } catch (RuntimeException e) {
            // Compensation for ANY failed step: close the booking, cancel its tickets, release its seats.
            // Release runs even when the hold was refused or never sent: it only touches seats HELD by this
            // booking, so it is a harmless no-op then, and it covers a hold whose answer was lost (timeout).
            bookingCompensator.close(bookingId, BookingStatus.CANCELLED);
            throw e;
        }
    }   

    private Booking insertPendingBooking(BookingRequest request, Long userId, String bookingReference,
                                         Long airlineId, String seatsHash) {
        return transactionTemplate.execute(status ->
                savePendingBooking(request, userId, bookingReference, airlineId, seatsHash));
    }

    // true only for the idempotency gate (uk_booking_active), not for any other constraint
    private static boolean isBookingKeyViolation(DataIntegrityViolationException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof org.hibernate.exception.ConstraintViolationException cve) {
                return cve.getConstraintName() != null
                        && cve.getConstraintName().toLowerCase().contains("uk_booking_active");
            }
        }
        return false;
    }

    // Link expired AND the grace for a late IPN is over: nothing can still pay this booking
    private static boolean isPastPaymentGrace(Booking booking) {
        return booking.getStatus() == BookingStatus.AWAITING_PAYMENT
                && booking.getPaymentExpiresAt() != null
                && booking.getPaymentExpiresAt().plus(BookingExpiryJob.PAYMENT_GRACE).isBefore(Instant.now());
    }

    /*
     * Idempotent create: answers a duplicate request from the live booking it collided with,
     * so refresh / Back / double click / lost response never create a second booking.
     */
    private PaymentInitiateResponse answerFromExistingBooking(Long userId, Booking existing) {
        log.info("Duplicate booking request from user {}: answering with booking {} ({})",
                userId, existing.getId(), existing.getStatus());

        PaymentInitiateResponse.PaymentInitiateResponseBuilder answer = PaymentInitiateResponse.builder()
                .bookingId(existing.getId())
                .bookingStatus(existing.getStatus())
                .paymentId(existing.getPaymentId())
                .reused(true);

        switch (existing.getStatus()) {
            // First request still running (holding seats / creating the link): controller answers 202
            case PENDING -> answer.success(false)
                    .message("Your booking is still being processed. Please wait a moment.");
            case AWAITING_PAYMENT -> {
                if (existing.getPaymentExpiresAt() != null
                        && existing.getPaymentExpiresAt().isBefore(Instant.now())) {
                    // Link expired but still inside the grace: a payment may be landing right now → 202, wait
                    answer.success(false)
                            .message("We're checking your payment for these seats. Please wait a moment.");
                } else {
                    // Link ready: hand out the SAME link while it can still be paid
                    answer.success(true)
                            .gateway(PaymentGateway.VNPAY)
                            .checkoutUrl(existing.getCheckoutUrl())
                            .expiresAt(existing.getPaymentExpiresAt())
                            .message("You already have a booking for these seats waiting for payment.");
                }
            }
            // Already paid: nothing to pay, point the user to the booking
            default -> answer.success(true)
                    .message("These seats are already booked for you.");
        }
        return answer.build();
    }

    // sha256 of the sorted seat ids, so the order the client sends them in doesn't matter
    private static String hashSeats(List<Long> seatInstanceIds) {
        String joined = seatInstanceIds.stream().sorted().map(String::valueOf)
                .collect(Collectors.joining(","));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(joined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private Booking savePendingBooking(BookingRequest request, Long userId,
                                       String bookingReference, Long airlineId, String seatsHash) {
        // Create booking entity with cross-service IDs
        Booking booking = BookingMapper.toEntity(
                request, userId, new HashSet<>(), bookingReference);
        booking.setStatus(BookingStatus.PENDING);
        booking.setAirlineId(airlineId);
        booking.setSeatsHash(seatsHash);   // part of the uk_booking_active idempotency key

        // Save booking
        booking = bookingRepository.save(booking);

        // One passenger row and one ticket (holding that passenger's seat) per traveller
        for (PassengerRequest passengerRequest : request.getPassengers()) {
            Passenger passenger = passengerService
                    .createPassengerForBooking(passengerRequest, userId, booking);
            booking.getPassengers().add(passenger);
            booking.getTickets().add(ticketService.issueTicket(
                    booking, passenger, passengerRequest.getSeatInstanceId()));
        }
        return booking;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id, Long userId, String userRoles) throws ResourceNotFoundException {
        Booking booking = bookingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with ID: " + id));
        // A booking (and its payment link) belongs to its traveller; airline staff and admins may view any (G3)
        boolean staff = userRoles != null && (userRoles.contains(UserRole.ROLE_AIRLINE_OWNER.name())
                || userRoles.contains(UserRole.ROLE_SYSTEM_ADMIN.name()));
        if (!staff && !Objects.equals(booking.getUserId(), userId)) {
            throw new ForbiddenException("You can only view your own bookings");
        }
        return convertBookingResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByAirline(
            Long userId,
            String searchQuery,
            BookingStatus status,
            Long flightInstanceId,
            String sortDirection
    ) {
        AirlineResponse airlineResponse = airlineClient.getAirlineByOwner(userId);
        Long airlineId = airlineResponse.getId();

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, "bookingDate");

        List<Booking> bookings = bookingRepository.findByAirlineWithFilters(
                airlineId, searchQuery, status, flightInstanceId, sort);

        return enrichBatch(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(this::convertBookingResponse)
                .collect(Collectors.toList());
    }

    // Not @Transactional: the status change commits first, then seat-service is called outside it
    @Override
    public BookingResponse cancelBooking(Long id, Long userId) throws ResourceNotFoundException {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));
        if (!Objects.equals(booking.getUserId(), userId)) {
            throw new ForbiddenException("You can only cancel your own bookings");
        }

        // Same compensation as every other failure path; only a live booking (PENDING / AWAITING_PAYMENT) closes
        if (!bookingCompensator.close(id, BookingStatus.CANCELLED)) {
            BookingStatus now = bookingRepository.findById(id).map(Booking::getStatus).orElse(null);
            // Already cancelled / expired: cancelling again is a no-op (idempotent). Paid: needs a refund flow (D2)
            if (now != BookingStatus.CANCELLED && now != BookingStatus.EXPIRED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking can no longer be cancelled");
            }
        }
        return getBookingById(id, userId, null);
    }

    @Override
    @Transactional
    public void deleteBooking(Long id) throws ResourceNotFoundException {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with ID: " + id));
        bookingRepository.delete(booking);
        log.info("Booking deleted: {}", booking.getBookingReference());
    }

    @Override
    public boolean existsById(Long id) {
        return bookingRepository.existsById(id);
    }

    @Override
    public long count() {
        return bookingRepository.count();
    }

    @Override
    public long countByFlightId(Long flightId) {
        return bookingRepository.countByFlightInstanceId(flightId);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingStatisticsResponse getBookingStatisticsForAirline(Long airlineId) {
        LocalDateTime now = LocalDateTime.now();

        // Today's statistics
        LocalDateTime startOfDay = now.with(LocalTime.MIN);
        LocalDateTime endOfDay = now.with(LocalTime.MAX);

        // This month's statistics
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);

        // Note: In the microservice, airlineId -> flightId mapping is needed via inter-service call
        // For now, we provide basic statistics
        Long todayBookings = bookingRepository.count(); // Simplified
        Double todayRevenue = 0.0;
        Long monthBookings = bookingRepository.count();
        Double monthRevenue = 0.0;

        // Build daily trend (last 30 days)
        List<BookingStatisticsResponse.DailyBookingData> dailyTrend = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 29; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dailyTrend.add(BookingStatisticsResponse.DailyBookingData.builder()
                    .date(date.format(dateFormatter))
                    .bookingCount(0L)
                    .revenue(0.0)
                    .build());
        }

        // Build monthly data (last 12 months)
        List<BookingStatisticsResponse.MonthlyData> monthlyData = new ArrayList<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 11; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            monthlyData.add(BookingStatisticsResponse.MonthlyData.builder()
                    .month(month.format(monthFormatter))
                    .bookingCount(0L)
                    .revenue(0.0)
                    .build());
        }

        return BookingStatisticsResponse.builder()
                .totalBookingsToday(todayBookings)
                .revenueToday(todayRevenue)
                .totalBookingsThisMonth(monthBookings)
                .revenueThisMonth(monthRevenue)
                .dailyTrend(dailyTrend)
                .monthlyData(monthlyData)
                .build();
    }

    private String generateBookingReference() {
        String reference;
        do {
            reference = "BK" + UUID.randomUUID().toString()
                    .substring(0, 8).toUpperCase();
        } while (bookingRepository.findByBookingReference(reference).isPresent());
        return reference;
    }

    private List<BookingResponse> enrichBatch(List<Booking> bookings) {
        if (bookings.isEmpty()) return Collections.emptyList();

        List<Long> bookingIds = bookings.stream().map(Booking::getId).collect(Collectors.toList());

        List<Long> fareIds = bookings.stream().map(Booking::getFareId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> flightIds = bookings.stream().map(Booking::getFlightId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> flightInstanceIds = bookings.stream().map(Booking::getFlightInstanceId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> allSeatIds = bookings.stream()
                .flatMap(b -> b.getSeatInstanceIds() != null ? b.getSeatInstanceIds().stream() : Stream.empty())
                .distinct().collect(Collectors.toList());
        List<Long> allAncillaryIds = bookings.stream()
                .flatMap(b -> b.getAncillaryIds() != null ? b.getAncillaryIds().stream() : Stream.empty())
                .distinct().collect(Collectors.toList());
        List<Long> allMealIds = bookings.stream()
                .flatMap(b -> b.getMealIds() != null ? b.getMealIds().stream() : Stream.empty())
                .distinct().collect(Collectors.toList());

        // One call per service
        Map<Long, FareResponse> fareMap = pricingClient.getFaresByIds(fareIds);
        Map<Long, FlightResponse> flightMap = flightClient.getFlightsByIds(flightIds);
        Map<Long, FlightInstanceResponse> flightInstanceMap = flightClient.getFlightInstancesByIds(flightInstanceIds);
        Map<Long, PaymentDTO> paymentMap = paymentClient.getPaymentsByBookingIds(bookingIds);

        Map<Long, SeatInstanceResponse> seatMap = allSeatIds.isEmpty()
                ? Collections.emptyMap()
                : seatClient.getAllByIds(allSeatIds).stream()
                        .collect(Collectors.toMap(SeatInstanceResponse::getId, s -> s));
        Map<Long, FlightCabinAncillaryResponse> ancillaryMap = allAncillaryIds.isEmpty()
                ? Collections.emptyMap()
                : ancillaryClient.getAllByIds(allAncillaryIds).stream()
                        .collect(Collectors.toMap(FlightCabinAncillaryResponse::getId, a -> a));
        Map<Long, FlightMealResponse> mealMap = allMealIds.isEmpty()
                ? Collections.emptyMap()
                : ancillaryClient.getMealsByIds(allMealIds).stream()
                        .collect(Collectors.toMap(FlightMealResponse::getId, m -> m));

        return bookings.stream().map(booking -> {
            List<SeatInstanceResponse> seats = booking.getSeatInstanceIds() != null
                    ? booking.getSeatInstanceIds().stream()
                            .map(seatMap::get).filter(Objects::nonNull).collect(Collectors.toList())
                    : Collections.emptyList();
            List<FlightCabinAncillaryResponse> ancillaries = booking.getAncillaryIds() != null
                    ? booking.getAncillaryIds().stream()
                            .map(ancillaryMap::get).filter(Objects::nonNull).collect(Collectors.toList())
                    : Collections.emptyList();
            List<FlightMealResponse> meals = booking.getMealIds() != null
                    ? booking.getMealIds().stream()
                            .map(mealMap::get).filter(Objects::nonNull).collect(Collectors.toList())
                    : Collections.emptyList();

            return BookingMapper.toResponse(
                    booking,
                    paymentMap.get(booking.getId()),
                    fareMap.get(booking.getFareId()),
                    flightMap.get(booking.getFlightId()),
                    flightInstanceMap.get(booking.getFlightInstanceId()),
                    ancillaries,
                    meals,
                    seats
            );
        }).collect(Collectors.toList());
    }

    private BookingResponse convertBookingResponse(Booking booking)  {
        List<FlightCabinAncillaryResponse> ancillaryResponses=ancillaryClient.getAllByIds(
                booking.getAncillaryIds()
        );
        List<FlightMealResponse> mealResponses=ancillaryClient.getMealsByIds(booking.getMealIds());
        PaymentDTO paymentDTO=paymentClient.getPaymentByBookingId(booking.getId());
        FareResponse fareResponse=pricingClient.getFareById(booking.getFareId());
        FlightResponse flightResponse=flightClient.getFlightById(booking.getFlightId());

        List<SeatInstanceResponse> seatInstanceResponses=seatClient.getAllByIds(booking.getSeatInstanceIds());
        FlightInstanceResponse flightInstanceResponse=flightClient.getFlightInstanceResponse(booking.getFlightInstanceId());


        System.out.println("seat instances -------- "+seatInstanceResponses.size());

        return BookingMapper.toResponse(booking,
                paymentDTO,
                fareResponse,
                flightResponse,
                flightInstanceResponse,
                ancillaryResponses,
                mealResponses,
                seatInstanceResponses
        );
    }
}
