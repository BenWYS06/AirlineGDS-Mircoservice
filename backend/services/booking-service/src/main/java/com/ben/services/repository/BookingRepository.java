package com.ben.services.repository;

import com.ben.common_lib.enums.BookingStatus;
import com.ben.services.model.Booking;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, BookingPerformanceRepository {

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByUserId(Long userId);



    long countByFlightInstanceId(Long flightInstanceId);


    // ── Idempotency (build step 3) ─────────────────────────────────────────

    // The live booking a duplicate request collided with (same columns + statuses as uk_booking_active)
    @Query("SELECT b FROM Booking b WHERE b.userId = :userId AND b.flightInstanceId = :flightInstanceId " +
            "AND b.seatsHash = :seatsHash AND b.status IN (PENDING, AWAITING_PAYMENT, CONFIRMED)")
    Optional<Booking> findLiveByKey(@Param("userId") Long userId,
                                    @Param("flightInstanceId") Long flightInstanceId,
                                    @Param("seatsHash") String seatsHash);

    // Last saga step of create: PENDING → AWAITING_PAYMENT only if nobody changed the booking meanwhile
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Booking b SET b.status = AWAITING_PAYMENT, b.checkoutUrl = :checkoutUrl, " +
            "b.paymentExpiresAt = :expiresAt, b.paymentId = :paymentId, b.lastModified = CURRENT_TIMESTAMP " +
            "WHERE b.id = :id AND b.status = PENDING")
    int markAwaitingPayment(@Param("id") Long id,
                            @Param("checkoutUrl") String checkoutUrl,
                            @Param("expiresAt") Instant expiresAt,
                            @Param("paymentId") Long paymentId);

    // ── Saga state changes (build step 4): every one is conditional, 0 rows = someone else got there first ──

    // Compensation: only a live booking can be closed (CANCELLED / EXPIRED). Used by every failure path.
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Booking b SET b.status = :status, b.lastModified = CURRENT_TIMESTAMP " +
            "WHERE b.id = :id AND b.status IN (PENDING, AWAITING_PAYMENT)")
    int closeIfAlive(@Param("id") Long id, @Param("status") BookingStatus status);

    // Paid + seats booked. EXPIRED is accepted too: late money whose seats were still free.
    // A repeated payment.completed matches 0 rows (already CONFIRMED) and changes nothing.
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Booking b SET b.status = CONFIRMED, b.paymentId = :paymentId, b.lastModified = CURRENT_TIMESTAMP " +
            "WHERE b.id = :id AND b.status IN (AWAITING_PAYMENT, EXPIRED)")
    int markConfirmed(@Param("id") Long id, @Param("paymentId") Long paymentId);

    // Money arrived but the seats are gone or the user had cancelled: the admin refunds it manually
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Booking b SET b.status = REFUND_REQUIRED, b.paymentId = :paymentId, b.lastModified = CURRENT_TIMESTAMP " +
            "WHERE b.id = :id AND b.status IN (AWAITING_PAYMENT, EXPIRED, CANCELLED)")
    int markRefundRequired(@Param("id") Long id, @Param("paymentId") Long paymentId);

    // Recovery job: bookings stuck in PENDING (booking-service died mid-create)
    List<Booking> findTop100ByStatusAndBookingDateBefore(BookingStatus status, LocalDateTime before);

    // Expiry job: AWAITING_PAYMENT bookings whose link (+ grace) has run out
    List<Booking> findTop100ByStatusAndPaymentExpiresAtBefore(BookingStatus status, Instant before);

    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.passengers " +
//            "LEFT JOIN FETCH b.payment " +
            "LEFT JOIN FETCH b.tickets " +
            "WHERE b.id = :id")
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);


    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.passengers p " +
            "WHERE b.airlineId = :airlineId " +
            "AND (:search IS NULL OR " +
            "LOWER(b.bookingReference) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(b.contactInfo.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(b.contactInfo.phone) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR b.status = :status) " +
            "AND (:flightInstanceId IS NULL OR b.flightInstanceId = :flightInstanceId)")
    List<Booking> findByAirlineWithFilters(
            @Param("airlineId") Long airlineId,
            @Param("search") String search,
            @Param("status") BookingStatus status,
            @Param("flightInstanceId") Long flightInstanceId,
            Sort sort);
}
