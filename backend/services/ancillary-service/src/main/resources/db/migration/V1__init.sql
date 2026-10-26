-- Baseline schema for ancillary-service, generated from the JPA entities (Hibernate ddl-auto=create + mysqldump --no-data).
-- Never edit this file after it has run: add V2__..., V3__... for later changes.

-- Tables are listed alphabetically, so foreign keys may point to tables created further down.
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `ancillaries` (
  `display_order` int DEFAULT NULL,
  `airline_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rfisc` varchar(10) DEFAULT NULL,
  `sub_type` varchar(100) DEFAULT NULL,
  `name` varchar(200) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `metadata` text,
  `type` enum('BAGGAGE','TRAVEL_PROTECTION') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `flight_cabin_ancillaries` (
  `available` bit(1) NOT NULL,
  `included_in_fare` bit(1) NOT NULL,
  `max_quantity` int DEFAULT NULL,
  `price` double DEFAULT NULL,
  `ancillary_id` bigint NOT NULL,
  `cabin_class_id` bigint NOT NULL,
  `flight_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `currency` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqwp6a6f49r6j9pa3no11h37ws` (`ancillary_id`),
  CONSTRAINT `FKqwp6a6f49r6j9pa3no11h37ws` FOREIGN KEY (`ancillary_id`) REFERENCES `ancillaries` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `flight_meals` (
  `available` bit(1) NOT NULL,
  `display_order` int DEFAULT NULL,
  `price` double DEFAULT NULL,
  `flight_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meal_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flight_meal` (`flight_id`,`meal_id`),
  KEY `idx_flight_meal_flight` (`flight_id`),
  KEY `idx_flight_meal_meal` (`meal_id`),
  CONSTRAINT `FKkeiil3l6qa6vmsmjvl2q81iw5` FOREIGN KEY (`meal_id`) REFERENCES `meals` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `insurance_coverages` (
  `active` bit(1) NOT NULL,
  `coverage_amount` double NOT NULL,
  `currency` varchar(3) DEFAULT NULL,
  `display_order` int DEFAULT NULL,
  `is_flat` bit(1) NOT NULL,
  `ancillary_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `emergency_contact` varchar(100) DEFAULT NULL,
  `name` varchar(200) NOT NULL,
  `claim_condition` varchar(500) DEFAULT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `coverage_type` enum('BAGGAGE_ASSISTANCE','BAGGAGE_DELAY','BAGGAGE_LOSS','DIVERTED_FLIGHT','EMERGENCY_ASSISTANCE','FREE_DATE_CHANGE','MEDICAL_EMERGENCY','MISSED_CONNECTION','PERSONAL_ACCIDENT','TRAVEL_DOCUMENT_LOSS','TRIP_CANCELLATION','TRIP_DELAY','ZERO_CANCELLATION') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKnuihpe5dp0vsqpox1i44j18gh` (`ancillary_id`),
  CONSTRAINT `FKnuihpe5dp0vsqpox1i44j18gh` FOREIGN KEY (`ancillary_id`) REFERENCES `ancillaries` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `meals` (
  `advance_booking_hours` int DEFAULT NULL,
  `available` bit(1) NOT NULL,
  `display_order` int DEFAULT NULL,
  `requires_advance_booking` bit(1) NOT NULL,
  `airline_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
