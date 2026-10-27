-- Build step 6: transactional outbox (DESIGN.md §11).
-- payment.completed / payment.failed are INSERTed here in the same transaction that sets the payment
-- SUCCESS / FAILED, so a crash right after the commit can never lose the event. OutboxRelay publishes them.
CREATE TABLE `outbox` (
  `id`           bigint        NOT NULL AUTO_INCREMENT,
  `aggregate_id` varchar(64)   NOT NULL,   -- bookingId, used as the Kafka key (per-booking ordering)
  `topic`        varchar(255)  NOT NULL,
  `event_type`   varchar(255)  NOT NULL,   -- event class, so the relay can rebuild the object
  `payload`      varchar(4000) NOT NULL,   -- event as JSON
  `created_at`   datetime(6)   NOT NULL,
  `published_at` datetime(6)   DEFAULT NULL,   -- NULL = not sent yet
  PRIMARY KEY (`id`),
  KEY `idx_outbox_published_at` (`published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
