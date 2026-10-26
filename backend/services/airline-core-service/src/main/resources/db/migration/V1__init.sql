-- Baseline schema for airline-core-service, generated from the JPA entities (Hibernate ddl-auto=create + mysqldump --no-data).
-- Never edit this file after it has run: add V2__..., V3__... for later changes.

-- Tables are listed alphabetically, so foreign keys may point to tables created further down.
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `aircrafts` (
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
  `id` bigint NOT NULL AUTO_INCREMENT,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `airlines` (
  `iata_code` varchar(2) NOT NULL,
  `icao_code` varchar(3) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `headquarters_city_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
