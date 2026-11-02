package com.ben.services.service;

import com.ben.common_lib.enums.BookingStatus;
import com.ben.common_lib.exception.ResourceNotFoundException;
import com.ben.common_lib.payload.request.BookingRequest;
import com.ben.common_lib.payload.response.*;

import java.util.List;

public interface BookingService {

    PaymentInitiateResponse createBooking(BookingRequest request, Long userId);

    /** 403 unless the caller owns the booking or has an airline/admin role (userRoles = X-User-Roles). */
    BookingResponse getBookingById(Long id, Long userId, String userRoles) throws ResourceNotFoundException;



    List<BookingResponse> getBookingsByAirline(
            Long userId,
            String searchQuery,
            BookingStatus status,
            Long flightInstanceId,
            String sortDirection
    );

    List<BookingResponse> getBookingsByUser(Long userId);

    /** Owner only (403); PENDING / AWAITING_PAYMENT only (CONFIRMED → 409); cancelling twice is a no-op. */
    BookingResponse cancelBooking(Long id, Long userId) throws ResourceNotFoundException;

    void deleteBooking(Long id) throws ResourceNotFoundException;

    boolean existsById(Long id);

    long count();

    long countByFlightId(Long flightId);

    BookingStatisticsResponse getBookingStatisticsForAirline(Long airlineId);
}
