---
paths:
  - "backend/**/event/**"
  - "backend/**/listener/**"
  - "backend/**/config/Kafka*.java"
  - "backend/common-lib/**/event/**"
---

# Kafka and event rules

**Delivery is at-least-once.** Every consumer must be safe to run twice on the same message.

- **Idempotent consumers:** check the current state in the DB inside the same transaction before acting (`UPDATE ... WHERE status = ?`). For side effects outside the DB (email), use a Redis `SET NX` per message and channel.
- **Message key = `bookingId`** for every booking-related event, so all events of one booking land on the same partition, in order.
- **Never swallow an exception in a listener.** Let it propagate, so the error handler retries with backoff and finally sends the message to the DLT.
- **Don't send to Kafka right after a DB commit and hope.** Write the event to the service's `outbox` table in the same transaction; the relay publishes it. (Being introduced in build step 6; until then, follow what the step says.)
- Event classes live in `common-lib` (`event/`). Changing one = scan every producer and consumer (see `api-contract.md`).
- Topics in use: `payment.completed`, `payment.failed`, `booking.confirmed`, `flight-instance-created` (flight-ops → seat, generates seats). `booking.cancelled` (seat release) arrives in build step 6.
