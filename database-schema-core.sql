-- Ben Airline - CORE booking workflow tables (MySQL) for DrawSQL import
-- Setup: airline -> aircraft -> cabin_classes -> seat_maps -> seats (templates)
-- Flow:  flight -> flight_instance (date) -> flight_instance_cabins + seat_instances (copied from seats)
--        -> user picks fare + seats -> booking -> passengers -> tickets -> payment
-- Links marked [cross-service] are logical: in the real system they are plain IDs across
-- separate microservice databases, added here only so the diagram shows the connections.

-- users (user-service): people who log in (linked to Keycloak)
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

-- airlines (airline-core-service): airline companies
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

-- airports (location-service): airports flights depart from / arrive at
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
  UNIQUE KEY `UKrck10qn096aw10ds8rjqf35ah` (`iata_code`)
);

-- aircrafts (airline-core-service): planes an airline owns (gives total seat count)
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
  CONSTRAINT `fk_aircrafts_airline_id` FOREIGN KEY (`airline_id`) REFERENCES `airlines` (`id`)
);

-- cabin_classes (seat-service): cabins of an aircraft: ECONOMY, BUSINESS...
--   cross-service links: aircraft_id -> aircrafts
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
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_cabin_classes_aircraft_id` FOREIGN KEY (`aircraft_id`) REFERENCES `aircrafts` (`id`)
);

-- seat_maps (seat-service): seat layout of a cabin
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
  CONSTRAINT `fk_seat_maps_cabin_class_id` FOREIGN KEY (`cabin_class_id`) REFERENCES `cabin_classes` (`id`)
);

-- seats (seat-service): physical seat template, e.g. 12A (not tied to a date)
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
  `created_at` datetime(6) NOT NULL,
  `seat_map_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `seat_number` varchar(10) NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `seat_type` enum('AISLE','EXTRA_LEGROOM','MIDDLE','WINDOW') NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_seats_seat_map_id` FOREIGN KEY (`seat_map_id`) REFERENCES `seat_maps` (`id`)
);

-- flights (flight-ops-service): a route, e.g. VN123 SGN -> HAN
--   cross-service links: aircraft_id -> aircrafts, airline_id -> airlines, departure_airport_id -> airports, arrival_airport_id -> airports
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
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_flights_aircraft_id` FOREIGN KEY (`aircraft_id`) REFERENCES `aircrafts` (`id`),
  CONSTRAINT `fk_flights_airline_id` FOREIGN KEY (`airline_id`) REFERENCES `airlines` (`id`),
  CONSTRAINT `fk_flights_departure_airport_id` FOREIGN KEY (`departure_airport_id`) REFERENCES `airports` (`id`),
  CONSTRAINT `fk_flights_arrival_airport_id` FOREIGN KEY (`arrival_airport_id`) REFERENCES `airports` (`id`)
);

-- flight_instances (flight-ops-service): one flight on one specific date
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
  CONSTRAINT `fk_flight_instances_flight_id` FOREIGN KEY (`flight_id`) REFERENCES `flights` (`id`)
);

-- flight_instance_cabins (seat-service): one cabin on one dated flight (seat counts per cabin)
--   cross-service links: flight_instance_id -> flight_instances
CREATE TABLE `flight_instance_cabins` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booked_seats` int DEFAULT NULL,
  `total_seats` int NOT NULL,
  `cabin_class_id` bigint NOT NULL,
  `flight_instance_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_flight_instance_cabins_flight_instance_id` FOREIGN KEY (`flight_instance_id`) REFERENCES `flight_instances` (`id`),
  CONSTRAINT `fk_flight_instance_cabins_cabin_class_id` FOREIGN KEY (`cabin_class_id`) REFERENCES `cabin_classes` (`id`)
);

-- fares (pricing-service): ticket price per flight + cabin class
--   cross-service links: cabin_class_id -> cabin_classes, flight_id -> flights
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
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_fares_cabin_class_id` FOREIGN KEY (`cabin_class_id`) REFERENCES `cabin_classes` (`id`),
  CONSTRAINT `fk_fares_flight_id` FOREIGN KEY (`flight_id`) REFERENCES `flights` (`id`)
);

-- seat_instances (seat-service): one seat on one dated flight (available / booked)
CREATE TABLE `seat_instances` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fare` double DEFAULT NULL,
  `is_available` bit(1) NOT NULL,
  `is_booked` bit(1) NOT NULL,
  `premium_surcharge` double DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `flight_instance_cabin_id` bigint NOT NULL,
  `seat_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `meal_preference` varchar(255) DEFAULT NULL,
  `status` enum('AVAILABLE','BLOCKED','BOOKED','OCCUPIED') NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_seat_instances_seat_id` FOREIGN KEY (`seat_id`) REFERENCES `seats` (`id`),
  CONSTRAINT `fk_seat_instances_flight_instance_cabin_id` FOREIGN KEY (`flight_instance_cabin_id`) REFERENCES `flight_instance_cabins` (`id`)
);

-- bookings (booking-service): a reservation made by a user
--   cross-service links: user_id -> users, flight_instance_id -> flight_instances, fare_id -> fares
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
  UNIQUE KEY `UKe92mgyq35mdeo8gc1un2o6uk0` (`booking_reference`),
  CONSTRAINT `fk_bookings_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_bookings_flight_instance_id` FOREIGN KEY (`flight_instance_id`) REFERENCES `flight_instances` (`id`),
  CONSTRAINT `fk_bookings_fare_id` FOREIGN KEY (`fare_id`) REFERENCES `fares` (`id`)
);

-- booking_seat_instances (booking-service): which seats a booking holds
--   cross-service links: seat_instance_id -> seat_instances
CREATE TABLE `booking_seat_instances` (
  `booking_id` bigint NOT NULL,
  `seat_instance_id` bigint DEFAULT NULL,
  CONSTRAINT `fk_booking_seat_instances_booking_id` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `fk_booking_seat_instances_seat_instance_id` FOREIGN KEY (`seat_instance_id`) REFERENCES `seat_instances` (`id`)
);

-- passengers (booking-service): people travelling on a booking
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
  CONSTRAINT `fk_passengers_booking_id` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
);

-- tickets (booking-service): e-ticket issued per passenger
CREATE TABLE `tickets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booking_id` bigint DEFAULT NULL,
  `issued_at` datetime(6) DEFAULT NULL,
  `passenger_id` bigint DEFAULT NULL,
  `ticket_number` varchar(255) NOT NULL,
  `status` enum('BOOKED','CANCELLED','EXPIRED','REFUNDED','USED') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4ks48wgrew48dpkh0wd1rbe2b` (`ticket_number`),
  CONSTRAINT `fk_tickets_booking_id` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `fk_tickets_passenger_id` FOREIGN KEY (`passenger_id`) REFERENCES `passengers` (`id`)
);

-- payments (payment-service): payment for a booking
--   cross-service links: booking_id -> bookings, user_id -> users
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
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_payments_booking_id` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `fk_payments_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
);
