-- Ben Airline - database schema (MySQL) for DrawSQL import
-- Generated 2026-09-25 from the JPA entities. Each microservice owns its own database;
-- links between services (e.g. bookings.flight_id -> flights.id) are IDs only, not foreign keys.

-- ============ user-service (airline_user) ============

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `full_name` varchar(255) NOT NULL,
  `last_login` datetime(6) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `role` tinyint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `verified` bit(1) NOT NULL,
  `keycloak_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UK366dgrd625s5659shyen79mmw` (`keycloak_id`)
);

-- ============ location-service (airline_location_db) ============

CREATE TABLE `cities` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `country_code` varchar(5) NOT NULL,
  `city_code` varchar(10) NOT NULL,
  `region_code` varchar(10) DEFAULT NULL,
  `time_zone_id` varchar(50) DEFAULT NULL,
  `country_name` varchar(100) NOT NULL,
  `name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmmxm1v9ejho8cyhd1xs01g0w8` (`city_code`),
  KEY `idx_city_code` (`city_code`),
  KEY `idx_city_name` (`name`),
  KEY `idx_country_code` (`country_code`)
);

CREATE TABLE `airports` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `airlines_count` int DEFAULT NULL,
  `annual_passengers` double DEFAULT NULL,
  `destinations_count` int DEFAULT NULL,
  `iata_code` varchar(3) NOT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `on_time_performance` double DEFAULT NULL,
  `traveler_score` int DEFAULT NULL,
  `city_id` bigint NOT NULL,
  `postal_code` varchar(20) DEFAULT NULL,
  `size_category` varchar(20) DEFAULT NULL,
  `time_zone_id` varchar(50) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `street` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKrck10qn096aw10ds8rjqf35ah` (`iata_code`),
  KEY `idx_airport_iata` (`iata_code`),
  KEY `idx_airport_city_id` (`city_id`),
  CONSTRAINT `FKo1ddf0ullcf8qfgsf1b2nomed` FOREIGN KEY (`city_id`) REFERENCES `cities` (`id`)
);

-- ============ airline-core-service (airline_core_db) ============

CREATE TABLE `airlines` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `iata_code` varchar(2) NOT NULL,
  `icao_code` varchar(3) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `headquarters_city_id` bigint DEFAULT NULL,
  `owner_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by_user_id` bigint DEFAULT NULL,
  `alias` varchar(255) DEFAULT NULL,
  `alliance` varchar(255) DEFAULT NULL,
  `country` varchar(255) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `hours` varchar(255) DEFAULT NULL,
  `logo_url` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `status` enum('ACTIVE','BANNED','INACTIVE') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjdcwwct3mtl4d1k83eyqync97` (`iata_code`),
  UNIQUE KEY `UKi1fik122ximup2cwhq8jae18u` (`icao_code`)
);

CREATE TABLE `aircrafts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_seats` int DEFAULT NULL,
  `cruising_speed_kmh` int DEFAULT NULL,
  `economy_seats` int DEFAULT NULL,
  `first_class_seats` int DEFAULT NULL,
  `is_available` bit(1) NOT NULL,
  `max_altitude_ft` int DEFAULT NULL,
  `next_maintenance_date` date DEFAULT NULL,
  `premium_economy_seats` int DEFAULT NULL,
  `range_km` int DEFAULT NULL,
  `registration_date` date DEFAULT NULL,
  `seating_capacity` int NOT NULL,
  `year_of_manufacture` int DEFAULT NULL,
  `airline_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `current_airport_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `aircraft_code` varchar(20) NOT NULL,
  `manufacturer` varchar(50) NOT NULL,
  `model` varchar(50) NOT NULL,
  `status` enum('ACTIVE','INACTIVE','MAINTENANCE','RETIRED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK8si0l3ymr3vl1t2nd9u85poqp` (`aircraft_code`),
  KEY `idx_aircraft_code` (`aircraft_code`),
  KEY `idx_aircraft_model` (`model`),
  KEY `idx_aircraft_airline` (`airline_id`),
  CONSTRAINT `FKlrgmua8va47so5ldeqh752s9r` FOREIGN KEY (`airline_id`) REFERENCES `airlines` (`id`)
);

-- ============ flight-ops-service (airline_flight_db) ============

CREATE TABLE `flights` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aircraft_id` bigint NOT NULL,
  `airline_id` bigint NOT NULL,
  `arrival_airport_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `departure_airport_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `flight_number` varchar(10) NOT NULL,
  `status` enum('ARRIVED','BOARDING','CANCELLED','COMPLETED','DELAYED','DEPARTED','DIVERTED','IN_AIR','LANDED','SCHEDULED') DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `flight_instances` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `available_seats` int NOT NULL,
  `is_active` bit(1) NOT NULL,
  `max_advance_booking_days` int DEFAULT NULL,
  `min_advance_booking_days` int DEFAULT NULL,
  `total_seats` int NOT NULL,
  `airline_id` bigint DEFAULT NULL,
  `arrival_airport_id` bigint NOT NULL,
  `arrival_date_time` datetime(6) NOT NULL,
  `departure_airport_id` bigint NOT NULL,
  `departure_date_time` datetime(6) NOT NULL,
  `flight_id` bigint NOT NULL,
  `schedule_id` bigint NOT NULL,
  `version` bigint DEFAULT NULL,
  `gate` varchar(255) DEFAULT NULL,
  `terminal` varchar(255) DEFAULT NULL,
  `status` enum('ARRIVED','BOARDING','CANCELLED','COMPLETED','DELAYED','DEPARTED','DIVERTED','IN_AIR','LANDED','SCHEDULED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKg3ahnnurafdu0kf1cw4uo3j9c` (`flight_id`,`departure_date_time`),
  CONSTRAINT `FKldqkieut2gt9w3obug9tb5yp4` FOREIGN KEY (`flight_id`) REFERENCES `flights` (`id`)
);

CREATE TABLE `flight_schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `arrival_time` time NOT NULL,
  `departure_time` time NOT NULL,
  `end_date` date NOT NULL,
  `is_active` bit(1) NOT NULL,
  `start_date` date NOT NULL,
  `arrival_airport_id` bigint NOT NULL,
  `departure_airport_id` bigint NOT NULL,
  `flight_id` bigint NOT NULL,
  `version` bigint DEFAULT NULL,
  `recurrence_type` enum('CUSTOM','DAILY','NONE','WEEKLY') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKsefxfswv1twi0kkqi0gjyd6fi` (`flight_id`),
  CONSTRAINT `FKsefxfswv1twi0kkqi0gjyd6fi` FOREIGN KEY (`flight_id`) REFERENCES `flights` (`id`)
);

CREATE TABLE `schedule_operating_days` (
  `schedule_id` bigint NOT NULL,
  `day_of_week` enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') DEFAULT NULL,
  KEY `FK4tsfx7j14i09nfkuquqhw6no9` (`schedule_id`),
  CONSTRAINT `FK4tsfx7j14i09nfkuquqhw6no9` FOREIGN KEY (`schedule_id`) REFERENCES `flight_schedules` (`id`)
);

-- ============ seat-service (airline_seat_db) ============

CREATE TABLE `cabin_classes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `display_order` int NOT NULL,
  `is_active` bit(1) NOT NULL,
  `is_bookable` bit(1) NOT NULL,
  `typical_seat_pitch` int DEFAULT NULL,
  `typical_seat_width` int DEFAULT NULL,
  `code` varchar(5) NOT NULL,
  `aircraft_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `seat_type` varchar(255) DEFAULT NULL,
  `name` enum('BUSINESS','ECONOMY','FIRST','PREMIUM_ECONOMY') NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `flight_instance_cabins` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booked_seats` int DEFAULT NULL,
  `total_seats` int NOT NULL,
  `cabin_class_id` bigint NOT NULL,
  `flight_instance_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKdia8gbpr7pkyi7ki65trsisyw` (`cabin_class_id`),
  CONSTRAINT `FKdia8gbpr7pkyi7ki65trsisyw` FOREIGN KEY (`cabin_class_id`) REFERENCES `cabin_classes` (`id`)
);

CREATE TABLE `seat_maps` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `left_seats_per_row` int NOT NULL,
  `right_seats_per_row` int NOT NULL,
  `total_rows` int NOT NULL,
  `airline_id` bigint NOT NULL,
  `cabin_class_id` bigint DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK701xipgrjfahnib4k54r982kt` (`cabin_class_id`),
  CONSTRAINT `FKh63a3y4o6ra7c02t42mqo5e6t` FOREIGN KEY (`cabin_class_id`) REFERENCES `cabin_classes` (`id`)
);

CREATE TABLE `seats` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `base_price` double DEFAULT NULL,
  `column_letter` varchar(1) DEFAULT NULL,
  `has_bassinet` bit(1) NOT NULL,
  `has_extra_legroom` bit(1) NOT NULL,
  `has_extra_width` bit(1) NOT NULL,
  `has_power_outlet` bit(1) NOT NULL,
  `has_tv_screen` bit(1) NOT NULL,
  `is_active` bit(1) NOT NULL,
  `is_available` bit(1) NOT NULL,
  `is_blocked` bit(1) NOT NULL,
  `is_emergency_exit` bit(1) NOT NULL,
  `is_near_galley` bit(1) NOT NULL,
  `is_near_lavatory` bit(1) NOT NULL,
  `is_wheelchair_accessible` bit(1) NOT NULL,
  `premium_surcharge` double DEFAULT NULL,
  `recline_angle` int DEFAULT NULL,
  `seat_pitch` int DEFAULT NULL,
  `seat_row` int NOT NULL,
  `seat_width` int DEFAULT NULL,
  `cabin_class_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `seat_map_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `seat_number` varchar(10) NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `seat_type` enum('AISLE','EXTRA_LEGROOM','MIDDLE','WINDOW') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_seat_cabinclass` (`cabin_class_id`),
  KEY `FKn0lcnvvcrbb3wn5s6ksyc1v9q` (`seat_map_id`),
  CONSTRAINT `fk_seat_cabinclass` FOREIGN KEY (`cabin_class_id`) REFERENCES `cabin_classes` (`id`),
  CONSTRAINT `FKn0lcnvvcrbb3wn5s6ksyc1v9q` FOREIGN KEY (`seat_map_id`) REFERENCES `seat_maps` (`id`)
);

CREATE TABLE `seat_instances` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fare` double DEFAULT NULL,
  `is_available` bit(1) NOT NULL,
  `is_booked` bit(1) NOT NULL,
  `premium_surcharge` double DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `flight_id` bigint NOT NULL,
  `flight_instance_cabin_id` bigint DEFAULT NULL,
  `flight_instance_id` bigint DEFAULT NULL,
  `flight_schedule_id` bigint DEFAULT NULL,
  `seat_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `meal_preference` varchar(255) DEFAULT NULL,
  `status` enum('AVAILABLE','BLOCKED','BOOKED','OCCUPIED') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK6alfubsuxuf390cxcnpcq329x` (`flight_instance_cabin_id`),
  KEY `FK1edor3revukrkm6y8m898c3in` (`seat_id`),
  CONSTRAINT `FK1edor3revukrkm6y8m898c3in` FOREIGN KEY (`seat_id`) REFERENCES `seats` (`id`),
  CONSTRAINT `FK6alfubsuxuf390cxcnpcq329x` FOREIGN KEY (`flight_instance_cabin_id`) REFERENCES `flight_instance_cabins` (`id`)
);

-- ============ pricing-service (airline_pricing_db) ============

CREATE TABLE `fares` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `advance_seat_selection` bit(1) NOT NULL,
  `airline_fees` double DEFAULT NULL,
  `airport_transfer` bit(1) NOT NULL,
  `base_fare` double NOT NULL,
  `complimentary_beverages` bit(1) NOT NULL,
  `complimentary_meals` bit(1) NOT NULL,
  `current_price` double NOT NULL,
  `extra_seat_space` bit(1) NOT NULL,
  `fast_track_security` bit(1) NOT NULL,
  `free_date_change` bit(1) NOT NULL,
  `full_refund` bit(1) NOT NULL,
  `guaranteed_seat_together` bit(1) NOT NULL,
  `in_flight_entertainment` bit(1) NOT NULL,
  `in_flight_internet` bit(1) NOT NULL,
  `lounge_access` bit(1) NOT NULL,
  `partial_refund` bit(1) NOT NULL,
  `preferred_seat_choice` bit(1) NOT NULL,
  `premium_meal_choice` bit(1) NOT NULL,
  `priority_boarding` bit(1) NOT NULL,
  `priority_checkin` bit(1) NOT NULL,
  `rbd_code` varchar(1) NOT NULL,
  `taxes_and_fees` double DEFAULT NULL,
  `cabin_class_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `flight_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `fare_label` varchar(100) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `cabin_class` enum('BUSINESS','ECONOMY','FIRST','PREMIUM_ECONOMY') DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `baggage_policies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cabin_baggage_max_dimension` double DEFAULT NULL,
  `cabin_baggage_max_weight` double DEFAULT NULL,
  `cabin_baggage_pieces` int DEFAULT NULL,
  `cabin_baggage_weight_per_piece` double DEFAULT NULL,
  `check_in_baggage_max_weight` double DEFAULT NULL,
  `check_in_baggage_pieces` int DEFAULT NULL,
  `check_in_baggage_weight_per_piece` double DEFAULT NULL,
  `extra_baggage_allowance` bit(1) NOT NULL,
  `free_checked_bags_allowance` int DEFAULT NULL,
  `priority_baggage` bit(1) NOT NULL,
  `airline_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `fare_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1lbpkmfrho2eb36xo0ouywl1w` (`fare_id`),
  CONSTRAINT `FKhqu96eusm22uksffyfo50lsc3` FOREIGN KEY (`fare_id`) REFERENCES `fares` (`id`)
);

CREATE TABLE `fare_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cancellation_fee` double DEFAULT NULL,
  `change_deadline_hours` int DEFAULT NULL,
  `change_fee` double DEFAULT NULL,
  `is_changeable` bit(1) DEFAULT NULL,
  `is_refundable` bit(1) DEFAULT NULL,
  `refund_deadline_days` int DEFAULT NULL,
  `airline_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `fare_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `rule_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKdo45693odksp1byrcdc759uah` (`fare_id`),
  CONSTRAINT `FKhoxt2ycj9ctheeqmg7aqw3y0l` FOREIGN KEY (`fare_id`) REFERENCES `fares` (`id`)
);

-- ============ ancillary-service (airline_ancillary_db) ============

CREATE TABLE `ancillaries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `display_order` int DEFAULT NULL,
  `airline_id` bigint NOT NULL,
  `rfisc` varchar(10) DEFAULT NULL,
  `sub_type` varchar(100) DEFAULT NULL,
  `name` varchar(200) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `metadata` text,
  `type` enum('BAGGAGE','TRAVEL_PROTECTION') NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `flight_cabin_ancillaries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `available` bit(1) NOT NULL,
  `included_in_fare` bit(1) NOT NULL,
  `max_quantity` int DEFAULT NULL,
  `price` double DEFAULT NULL,
  `ancillary_id` bigint NOT NULL,
  `cabin_class_id` bigint NOT NULL,
  `flight_id` bigint NOT NULL,
  `currency` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqwp6a6f49r6j9pa3no11h37ws` (`ancillary_id`),
  CONSTRAINT `FKqwp6a6f49r6j9pa3no11h37ws` FOREIGN KEY (`ancillary_id`) REFERENCES `ancillaries` (`id`)
);

CREATE TABLE `meals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `advance_booking_hours` int DEFAULT NULL,
  `available` bit(1) NOT NULL,
  `display_order` int DEFAULT NULL,
  `requires_advance_booking` bit(1) NOT NULL,
  `airline_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `code` varchar(10) NOT NULL,
  `meal_type` varchar(50) NOT NULL,
  `dietary_restriction` varchar(100) DEFAULT NULL,
  `name` varchar(200) NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `ingredients` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKe8bdahqmtgy5ekeo90sydyufx` (`code`),
  KEY `idx_meal_airline` (`airline_id`),
  KEY `idx_meal_code` (`code`)
);

CREATE TABLE `flight_meals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `available` bit(1) NOT NULL,
  `display_order` int DEFAULT NULL,
  `price` double DEFAULT NULL,
  `flight_id` bigint NOT NULL,
  `meal_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flight_meal` (`flight_id`,`meal_id`),
  KEY `idx_flight_meal_flight` (`flight_id`),
  KEY `idx_flight_meal_meal` (`meal_id`),
  CONSTRAINT `FKkeiil3l6qa6vmsmjvl2q81iw5` FOREIGN KEY (`meal_id`) REFERENCES `meals` (`id`)
);

CREATE TABLE `insurance_coverages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `coverage_amount` double NOT NULL,
  `currency` varchar(3) DEFAULT NULL,
  `display_order` int DEFAULT NULL,
  `is_flat` bit(1) NOT NULL,
  `ancillary_id` bigint NOT NULL,
  `emergency_contact` varchar(100) DEFAULT NULL,
  `name` varchar(200) NOT NULL,
  `claim_condition` varchar(500) DEFAULT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `coverage_type` enum('BAGGAGE_ASSISTANCE','BAGGAGE_DELAY','BAGGAGE_LOSS','DIVERTED_FLIGHT','EMERGENCY_ASSISTANCE','FREE_DATE_CHANGE','MEDICAL_EMERGENCY','MISSED_CONNECTION','PERSONAL_ACCIDENT','TRAVEL_DOCUMENT_LOSS','TRIP_CANCELLATION','TRIP_DELAY','ZERO_CANCELLATION') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKnuihpe5dp0vsqpox1i44j18gh` (`ancillary_id`),
  CONSTRAINT `FKnuihpe5dp0vsqpox1i44j18gh` FOREIGN KEY (`ancillary_id`) REFERENCES `ancillaries` (`id`)
);

-- ============ booking-service (airline_booking_db) ============

CREATE TABLE `bookings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `flexible_ticket` bit(1) NOT NULL,
  `ticket_issued` bit(1) NOT NULL,
  `trip_type` tinyint DEFAULT NULL,
  `airline_id` bigint NOT NULL,
  `booking_date` datetime(6) DEFAULT NULL,
  `fare_id` bigint DEFAULT NULL,
  `flight_id` bigint DEFAULT NULL,
  `flight_instance_id` bigint DEFAULT NULL,
  `last_modified` datetime(6) DEFAULT NULL,
  `payment_id` bigint DEFAULT NULL,
  `ticket_time_limit` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `booking_reference` varchar(255) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `cabin_class` enum('BUSINESS','ECONOMY','FIRST','PREMIUM_ECONOMY') DEFAULT NULL,
  `status` enum('CANCELLED','COMPLETED','CONFIRMED','PENDING') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKe92mgyq35mdeo8gc1un2o6uk0` (`booking_reference`)
);

CREATE TABLE `booking_ancillaries` (
  `ancillary_id` bigint DEFAULT NULL,
  `booking_id` bigint NOT NULL,
  KEY `FKeo55cvu13i273hp3yrqirnmkw` (`booking_id`),
  CONSTRAINT `FKeo55cvu13i273hp3yrqirnmkw` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
);

CREATE TABLE `booking_meals` (
  `booking_id` bigint NOT NULL,
  `meal_id` bigint DEFAULT NULL,
  KEY `FK3nde1vk7trgnwpsq9mk7gi2qm` (`booking_id`),
  CONSTRAINT `FK3nde1vk7trgnwpsq9mk7gi2qm` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
);

CREATE TABLE `booking_seat_instances` (
  `booking_id` bigint NOT NULL,
  `seat_instance_id` bigint DEFAULT NULL,
  KEY `FK81nx3su4io1vws0ggk52w148h` (`booking_id`),
  CONSTRAINT `FK81nx3su4io1vws0ggk52w148h` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
);

CREATE TABLE `passengers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_of_birth` date NOT NULL,
  `is_active` bit(1) NOT NULL,
  `requires_wheelchair_assistance` bit(1) DEFAULT NULL,
  `booking_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `primary_user_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `dietary_preferences` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `first_name` varchar(255) NOT NULL,
  `frequent_flyer_number` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) NOT NULL,
  `medical_conditions` varchar(255) DEFAULT NULL,
  `nationality` varchar(255) DEFAULT NULL,
  `passport_number` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `gender` enum('FEMALE','MALE','OTHER') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKejvp8b3th7f5uqyn8gyckhbqi` (`passport_number`),
  KEY `FKgc7vcfrut3vamougerwse2m2u` (`booking_id`),
  CONSTRAINT `FKgc7vcfrut3vamougerwse2m2u` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
);

CREATE TABLE `tickets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booking_id` bigint DEFAULT NULL,
  `issued_at` datetime(6) DEFAULT NULL,
  `passenger_id` bigint DEFAULT NULL,
  `ticket_number` varchar(255) NOT NULL,
  `status` enum('BOOKED','CANCELLED','EXPIRED','REFUNDED','USED') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4ks48wgrew48dpkh0wd1rbe2b` (`ticket_number`),
  KEY `FKefja4avuu7g29t78mxifrsynb` (`booking_id`),
  KEY `FK1ds262xq345nkvs5o9ptnftwr` (`passenger_id`),
  CONSTRAINT `FK1ds262xq345nkvs5o9ptnftwr` FOREIGN KEY (`passenger_id`) REFERENCES `passengers` (`id`),
  CONSTRAINT `FKefja4avuu7g29t78mxifrsynb` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
);

-- ============ payment-service (airline_payment_db) ============

CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` double DEFAULT NULL,
  `booking_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `failure_reason` varchar(255) DEFAULT NULL,
  `method` varchar(255) DEFAULT NULL,
  `provider_payment_id` varchar(255) DEFAULT NULL,
  `refund_id` varchar(255) DEFAULT NULL,
  `transaction_id` varchar(255) DEFAULT NULL,
  `provider` enum('RAZORPAY','STRIPE') DEFAULT NULL,
  `status` enum('CANCELLED','FAILED','PENDING','PROCESSING','REFUNDED','SUCCESS') DEFAULT NULL,
  PRIMARY KEY (`id`)
);
