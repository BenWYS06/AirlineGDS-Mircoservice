package com.ben.services.event;

import com.ben.common_lib.event.BookingConfirmedEvent;
import com.ben.services.service.SeatInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEventListener {

    private final SeatInstanceService seatInstanceService;

    @KafkaListener(topics = "booking.confirmed", groupId = "seat-service-group")
    @Transactional
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        if (event.getSeatInstanceIds() == null || event.getSeatInstanceIds().isEmpty()) {
            log.warn("No seat instance IDs in BookingConfirmedEvent for booking: {}", event.getBookingReference());
            return;
        }

        // Only seats this booking still HELD become BOOKED (never overwrite another booking's seat)
        int confirmed = seatInstanceService.confirmSeats(event.getBookingId(), event.getSeatInstanceIds());
        log.info("Booking {}: {} seat(s) marked BOOKED", event.getBookingReference(), confirmed);
    }
}
