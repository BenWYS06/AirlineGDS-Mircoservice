package com.ben.services.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** One event waiting to be (or already) published to Kafka. Table `outbox` (Flyway migration). */
@Entity
@Table(name = "outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // increasing: the relay publishes in this order

    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;   // bookingId → Kafka key, so one booking's events stay in order

    @Column(nullable = false)
    private String topic;

    @Column(name = "event_type", nullable = false)
    private String eventType;   // event class name, used to rebuild the object before sending

    @Column(nullable = false, length = 4000)
    private String payload;   // event as JSON

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;   // null = not sent yet
}
