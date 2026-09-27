-- =============================================================================
-- MySQL init script: creates one database per microservice.
-- For running services locally against ONE MySQL on localhost:3306:
--   mysql -u root -p < docker/init-databases.sql
-- (docker-compose.yml does not need it: each DB container creates its own database.)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS airline_user;
CREATE DATABASE IF NOT EXISTS airline_core_db;
CREATE DATABASE IF NOT EXISTS airline_flight_db;
CREATE DATABASE IF NOT EXISTS airline_location_db;
CREATE DATABASE IF NOT EXISTS airline_seat_db;
CREATE DATABASE IF NOT EXISTS airline_pricing_db;
CREATE DATABASE IF NOT EXISTS airline_ancillary_db;
CREATE DATABASE IF NOT EXISTS airline_booking_db;
CREATE DATABASE IF NOT EXISTS airline_payment_db;
