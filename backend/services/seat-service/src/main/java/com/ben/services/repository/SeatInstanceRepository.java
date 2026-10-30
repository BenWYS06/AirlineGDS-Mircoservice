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
     * Holds every requested seat in ONE statement and returns how many rows it changed.
     * - Race condition: the "is it free?" check lives in the WHERE, evaluated while the UPDATE holds the
     *   row lock, so when two bookings race for a seat only one matches it; the other gets fewer rows.
     * - Deadlock: one UPDATE on the primary key locks the rows in id order, so two bookings with
     *   overlapping seats queue on the same first row instead of each holding a row the other needs.
     * - Idempotency: a seat already HELD by this same booking matches again, so a retry still succeeds.
     * - Lazy expiry: a HELD seat whose hold has passed counts as free.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SeatInstance si SET si.status = HELD, si.bookingId = :bookingId, " +
            "si.heldUntil = :heldUntil, si.version = si.version + 1 " +
            "WHERE si.id IN :seatInstanceIds AND (si.status = AVAILABLE " +
            "OR (si.status = HELD AND (si.bookingId = :bookingId OR si.heldUntil < :now)))")
    int holdAll(@Param("seatInstanceIds") List<Long> seatInstanceIds,
                @Param("bookingId") Long bookingId,
                @Param("heldUntil") Instant heldUntil,
                @Param("now") Instant now);

    // Input check before holding: how many of these seats belong to the flight being booked
    @Query("SELECT COUNT(si) FROM SeatInstance si WHERE si.id IN :seatInstanceIds " +
            "AND si.flightInstanceCabin.flightInstanceId = :flightInstanceId")
    long countInFlightInstance(@Param("seatInstanceIds") List<Long> seatInstanceIds,
                               @Param("flightInstanceId") Long flightInstanceId);

    /**
     * Payment arrived: make the booking's seats BOOKED (DESIGN.md §8). Matches, per seat:
     * - HELD or BOOKED by this booking → normal confirm, and idempotent on a repeat (Kafka redelivery)
     * - AVAILABLE, or HELD by someone whose hold expired → late money: the hold was gone, but the seat
     *   is still free, so the paying user still gets it
     * A seat now held/booked by ANOTHER live booking doesn't match → fewer rows → caller rolls back ("seats lost").
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SeatInstance si SET si.status = BOOKED, si.bookingId = :bookingId, si.heldUntil = null, " +
            "si.version = si.version + 1 WHERE si.id IN :seatInstanceIds AND (" +
            "(si.status IN (HELD, BOOKED) AND si.bookingId = :bookingId) " +
            "OR si.status = AVAILABLE " +
            "OR (si.status = HELD AND si.heldUntil < :now))")
    int confirmForBooking(@Param("bookingId") Long bookingId,
                          @Param("seatInstanceIds") List<Long> seatInstanceIds,
                          @Param("now") Instant now);

    /** HELD → AVAILABLE for every seat this booking holds (payment failed / booking cancelled). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SeatInstance si SET si.status = AVAILABLE, si.bookingId = null, si.heldUntil = null, " +
            "si.version = si.version + 1 WHERE si.bookingId = :bookingId AND si.status = HELD")
    int releaseHeld(@Param("bookingId") Long bookingId);
}
