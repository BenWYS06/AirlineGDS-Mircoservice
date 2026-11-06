package com.ben.services.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Transactional outbox, read side: publishes saved events to Kafka.
 * - At-least-once: if the service dies after send() but before the row is marked, the event is sent again
 *   on the next run. Every consumer is idempotent (state guards), so a duplicate changes nothing.
 * - Order: rows go out in id order and the batch STOPS at the first failure, so a later event of a booking
 *   can never overtake an earlier one. Key = bookingId keeps them on one partition, in order, in Kafka too.
 * - Kafka down: rows simply wait and go out once it is back. Nothing is lost.
 * (Single instance in this project; several instances would need SELECT ... FOR UPDATE SKIP LOCKED.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final Duration KEEP_PUBLISHED = Duration.ofDays(7);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Scheduled(fixedDelay = 500)
    public void publishPending() {
        for (OutboxEvent row : outboxRepository.findTop100ByPublishedAtIsNullOrderByIdAsc()) {
            try {
                Object event = jsonMapper.readValue(row.getPayload(), Class.forName(row.getEventType()));
                // .get(): wait for the broker's ack before marking the row, otherwise a lost send looks "sent"
                kafkaTemplate.send(row.getTopic(), row.getAggregateId(), event).get(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Outbox event {} ({}) not sent yet, retrying next run: {}",
                        row.getId(), row.getTopic(), e.getMessage());
                return;   // stop here to keep the order; the next run starts again from this row
            }
            row.setPublishedAt(Instant.now());
            outboxRepository.save(row);
        }
    }

    // Once a day: drop rows that were sent more than a week ago
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanUp() {
        int deleted = outboxRepository.deletePublishedBefore(Instant.now().minus(KEEP_PUBLISHED));
        if (deleted > 0) {
            log.info("Outbox clean-up: {} published event(s) deleted", deleted);
        }
    }
}
