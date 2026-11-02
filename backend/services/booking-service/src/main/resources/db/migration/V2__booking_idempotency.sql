-- Build step 3: idempotent create (DESIGN.md §2).

-- Identifies "the same booking request": sha256 of the sorted seat instance ids
ALTER TABLE bookings ADD COLUMN seats_hash VARCHAR(64);

-- Stored when the VNPay link is ready (status AWAITING_PAYMENT), so a duplicate request can
-- get the same link back without calling payment-service again
ALTER TABLE bookings ADD COLUMN checkout_url       VARCHAR(2048);
ALTER TABLE bookings ADD COLUMN payment_expires_at TIMESTAMP(6) WITH TIME ZONE;

-- The idempotency gate: only ONE live booking per (user, flight, seat set).
-- Partial index = uniqueness applies only to these statuses, so after CANCELLED / EXPIRED
-- the same seats can be booked again. A second INSERT of the same request fails here,
-- atomically, even when both requests arrive at the same moment.
CREATE UNIQUE INDEX uk_booking_active
    ON bookings (user_id, flight_instance_id, seats_hash)
    WHERE status IN ('PENDING', 'AWAITING_PAYMENT', 'CONFIRMED');
