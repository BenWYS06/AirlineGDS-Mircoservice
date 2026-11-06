package com.ben.services.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

/**
 * Transactional outbox, write side: instead of kafkaTemplate.send(...) right after a commit (lost if the
 * service dies in between), the event is saved as a row in the SAME transaction as the state change.
 * Both commit together or neither does; OutboxRelay sends the row later.
 */
@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxRepository outboxRepository;
    private final JsonMapper jsonMapper;

    // MANDATORY: fails loudly if called outside a transaction, the whole point is to share the caller's one
    @Transactional(propagation = Propagation.MANDATORY)
    public void add(String topic, Long bookingId, Object event) {
        outboxRepository.save(OutboxEvent.builder()
                .aggregateId(String.valueOf(bookingId))
                .topic(topic)
                .eventType(event.getClass().getName())
                .payload(jsonMapper.writeValueAsString(event))
                .createdAt(Instant.now())
                .build());
    }
}
