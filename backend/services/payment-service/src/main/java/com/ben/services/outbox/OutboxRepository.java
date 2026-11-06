package com.ben.services.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    // The relay's batch: oldest unsent events first
    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByIdAsc();

    // Housekeeping: sent events are only kept for a while (debugging / replay)
    @Transactional
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.publishedAt < :before")
    int deletePublishedBefore(@Param("before") Instant before);
}
