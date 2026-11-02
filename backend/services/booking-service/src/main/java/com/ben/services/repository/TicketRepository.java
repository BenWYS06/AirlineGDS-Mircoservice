package com.ben.services.repository;

import com.ben.common_lib.enums.TicketStatus;
import com.ben.services.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    List<Ticket> findByBookingId(Long bookingId);

    List<Ticket> findByPassengerId(Long passengerId);

    List<Ticket> findByStatus(TicketStatus status);

    boolean existsByTicketNumber(String ticketNumber);

    // A closed booking's tickets must not be usable (DESIGN G1): BOOKED → CANCELLED
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Ticket t SET t.status = CANCELLED WHERE t.booking.id = :bookingId AND t.status = BOOKED")
    int cancelByBooking(@Param("bookingId") Long bookingId);

    // Late money confirmed an EXPIRED booking: its tickets were cancelled by the job, make them valid again
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Ticket t SET t.status = BOOKED WHERE t.booking.id = :bookingId AND t.status = CANCELLED")
    int restoreByBooking(@Param("bookingId") Long bookingId);

    @Query("SELECT t FROM Ticket t " +
            "LEFT JOIN FETCH t.booking " +
            "LEFT JOIN FETCH t.passenger " +
            "WHERE t.booking.id = :bookingId")
    List<Ticket> findByBookingIdWithDetails(@Param("bookingId") Long bookingId);
}
