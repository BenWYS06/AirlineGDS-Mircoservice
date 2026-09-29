-- Remove redundant flight references from seat_instances.
-- A seat instance reaches its dated flight through its cabin:
--   seat_instances.flight_instance_cabin_id -> flight_instance_cabins.flight_instance_id
-- Hibernate (ddl-auto=update) never drops columns, so existing databases need this once.
-- flight_id was NOT NULL: without this migration, inserting seat instances fails with
-- "Field 'flight_id' doesn't have a default value".

USE airline_seat_db;

ALTER TABLE seat_instances
  DROP COLUMN flight_id,
  DROP COLUMN flight_instance_id,
  DROP COLUMN flight_schedule_id,
  MODIFY flight_instance_cabin_id bigint NOT NULL;
