package com.ben.services.model;

import com.ben.common_lib.enums.SeatAvailabilityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@Entity
@Table(name = "seat_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The dated flight is reached through the cabin: flightInstanceCabin.flightInstanceId
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flight_instance_cabin_id", nullable = false)
    private FlightInstanceCabin flightInstanceCabin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SeatAvailabilityStatus status = SeatAvailabilityStatus.AVAILABLE;

    // Cross-service ref: Booking (booking-service) holding or owning this seat
    @Column(name = "booking_id")
    private Long bookingId;

    // While status is HELD: the hold expires at this moment (payment abandoned)
    @Column(name = "held_until")
    private Instant heldUntil;

    private String mealPreference;
    private Double fare;
    private Double premiumSurcharge;

    @Version
    private Long version;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @PrePersist
    public void preCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
