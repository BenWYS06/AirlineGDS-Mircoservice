# booking-service

> Loaded when working inside `backend/services/booking-service/`. Design of record: [DESIGN.md](../../../docs/booking-refactor/DESIGN.md).
> This file describes the **target** design. What is already built: "Current status" in [PROGRESS.md](../../../docs/booking-refactor/PROGRESS.md).

## Role

**Saga orchestrator** for the booking flow. Booking decides; seat-service and payment-service execute. Notification is outside the saga (fire-and-forget on `booking.confirmed`).

```
POST /api/bookings
  validate → INSERT booking PENDING (idempotency gate)
  → reads (flight, prices) → seat.hold → payment.initiate
  → UPDATE AWAITING_PAYMENT + checkout_url → return checkout_url

payment.completed → confirm seats (sync) → CONFIRMED → booking.confirmed
payment.failed / cancel / expiry job → CANCELLED | EXPIRED + seat release
```

## Statuses (the status is the saga state)

`PENDING` → `AWAITING_PAYMENT` → `CONFIRMED`, failure branches `CANCELLED`, `EXPIRED`, `REFUND_REQUIRED` (money arrived after the booking closed).

- "Alive" = `PENDING`, `AWAITING_PAYMENT`. Every guard that closes a booking uses `status IN (PENDING, AWAITING_PAYMENT)`.
- A `CONFIRMED` booking cannot be cancelled (409).

## Rules for this service

- **Database is PostgreSQL** (every other service is MySQL). Flyway uses `flyway-database-postgresql`. Write migrations in PostgreSQL syntax.
- **Idempotent create** comes from the partial unique index `uk_booking_active (user_id, flight_instance_id, seats_hash) WHERE status IN ('PENDING','AWAITING_PAYMENT','CONFIRMED')`. A duplicate key means "answer from the existing booking", not an error.
- **No Feign call inside a transaction.** Use `TransactionTemplate` for short write transactions between remote calls.
- **Reads before writes:** flight check and price calls run before `seat.hold`, so a read failure needs no compensation.
- **State changes are conditional updates** (`... WHERE id = ? AND status = ?`). Zero rows updated = someone else got there first; never overwrite.
- Feign fallbacks **throw 503**. They never fake a hold or a payment link. `SeatClientFallbackFactory` lets 409 (seat taken) pass through.
- Prices are computed server-side from ids. Never trust a price from the client.

## Key classes

| Class | Purpose |
|---|---|
| `service/impl/BookingServiceImpl` | Create flow, compensation, cancel |
| `clients/*Client`, `*Fallback*` | Feign clients to seat, payment, flight-ops, pricing, ancillary, user, airline-core |
| `event/listener/PaymentEventListener` | Consumes `payment.completed` / `payment.failed` |
| `event/publisher/BookingEventProducer` | Publishes `booking.confirmed` |
| `model/Booking`, `Passenger`, `Ticket` | Ticket holds `seatInstanceId` (one ticket per seat) |
| `resources/db/migration/V1__init.sql` | PostgreSQL schema |

## Verify

```bash
cd backend && mvn -q compile -pl services/booking-service -am
```
If `common-lib` DTOs changed, also compile every service that uses them, and check `frontend/src/Redux/booking`.
