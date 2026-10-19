package com.ben.services.repository;

import com.ben.services.model.SeatInstance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SeatInstanceRepository extends JpaRepository<SeatInstance, Long> {
    List<SeatInstance> findBySeatId(Long seatId);
    List<SeatInstance> findByFlightInstanceCabinId(Long id);

    // A seat instance reaches its dated flight through its cabin
    @Query("SELECT si FROM SeatInstance si WHERE si.flightInstanceCabin.flightInstanceId = :flightInstanceId")
    List<SeatInstance> findByFlightInstanceId(@Param("flightInstanceId") Long flightInstanceId);

    // "Free" = AVAILABLE, or HELD by a booking whose hold has expired (payment abandoned)
    @Query("SELECT si FROM SeatInstance si WHERE si.flightInstanceCabin.flightInstanceId = :flightInstanceId " +
            "AND (si.status = AVAILABLE OR (si.status = HELD AND si.heldUntil < :now))")
    List<SeatInstance> findAvailableByFlightInstanceId(@Param("flightInstanceId") Long flightInstanceId,
                                                       @Param("now") Instant now);

    @Query("SELECT COUNT(si) FROM SeatInstance si WHERE si.flightInstanceCabin.flightInstanceId = :flightInstanceId " +
            "AND (si.status = AVAILABLE OR (si.status = HELD AND si.heldUntil < :now))")
    Long countAvailableByFlightInstanceId(@Param("flightInstanceId") Long flightInstanceId,
                                          @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT si FROM SeatInstance si JOIN FETCH si.seat s JOIN FETCH si.flightInstanceCabin fc WHERE si.id = :seatInstanceId")
    Optional<SeatInstance> findByIdForUpdate(@Param("seatInstanceId") Long seatInstanceId);

    // ── Seat hold (race condition safe) ─────────────────────────────────────

    /**
     * Atomic check-and-set: the WHERE clause re-checks "free" while the UPDATE holds the row lock,
     * so when two bookings race for the same seat exactly one gets 1 row, the other gets 0.
     * Re-holding by the same booking is allowed, so a retried request does not fail.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SeatInstance si SET si.status = HELD, si.bookingId = :bookingId, " +
            "si.heldUntil = :heldUntil, si.version = si.version + 1 " +
            "WHERE si.id = :seatInstanceId AND (si.status = AVAILABLE " +
            "OR (si.status = HELD AND (si.heldUntil < :now OR si.bookingId = :bookingId)))")
    int holdIfFree(@Param("seatInstanceId") Long seatInstanceId,
                   @Param("bookingId") Long bookingId,
                   @Param("heldUntil") Instant heldUntil,
                   @Param("now") Instant now);

    /** HELD → BOOKED, only for seats this booking still holds (a hold lost to expiry is not touched). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SeatInstance si SET si.status = BOOKED, si.heldUntil = null, si.version = si.version + 1 " +
            "WHERE si.bookingId = :bookingId AND si.status = HELD AND si.id IN :seatInstanceIds")
    int confirmHeld(@Param("bookingId") Long bookingId,
                    @Param("seatInstanceIds") List<Long> seatInstanceIds);

    /** HELD → AVAILABLE for every seat this booking holds (payment failed / booking cancelled). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SeatInstance si SET si.status = AVAILABLE, si.bookingId = null, si.heldUntil = null, " +
            "si.version = si.version + 1 WHERE si.bookingId = :bookingId AND si.status = HELD")
    int releaseHeld(@Param("bookingId") Long bookingId);
}
