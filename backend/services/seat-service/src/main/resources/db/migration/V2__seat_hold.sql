-- Seat hold: a booking reserves its seats (HELD) while the user pays.
--   booking_id  : which booking holds / owns the seat (cross-service id, no FK)
--   held_until  : after this moment a HELD seat counts as free again (abandoned payment)

ALTER TABLE `seat_instances`
  ADD COLUMN `booking_id` bigint DEFAULT NULL,
  ADD COLUMN `held_until` datetime(6) DEFAULT NULL,
  MODIFY `status` enum('AVAILABLE','BLOCKED','BOOKED','HELD','OCCUPIED') NOT NULL;

-- confirm / release look seats up by booking
CREATE INDEX `idx_seat_instances_booking_id` ON `seat_instances` (`booking_id`);
