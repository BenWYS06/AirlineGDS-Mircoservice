-- Baseline schema for flight-ops-service, generated from the JPA entities (Hibernate ddl-auto=create + mysqldump --no-data).
-- Never edit this file after it has run: add V2__..., V3__... for later changes.

-- Tables are listed alphabetically, so foreign keys may point to tables created further down.
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `flight_instances` (
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
  `id` bigint NOT NULL AUTO_INCREMENT,
  `schedule_id` bigint NOT NULL,
  `version` bigint DEFAULT NULL,
  `gate` varchar(255) DEFAULT NULL,
  `terminal` varchar(255) DEFAULT NULL,
  `status` enum('ARRIVED','BOARDING','CANCELLED','COMPLETED','DELAYED','DEPARTED','DIVERTED','IN_AIR','LANDED','SCHEDULED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKg3ahnnurafdu0kf1cw4uo3j9c` (`flight_id`,`departure_date_time`),
  CONSTRAINT `FKldqkieut2gt9w3obug9tb5yp4` FOREIGN KEY (`flight_id`) REFERENCES `flights` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `flight_schedules` (
  `arrival_time` time NOT NULL,
  `departure_time` time NOT NULL,
  `end_date` date NOT NULL,
  `is_active` bit(1) NOT NULL,
  `start_date` date NOT NULL,
  `arrival_airport_id` bigint NOT NULL,
  `departure_airport_id` bigint NOT NULL,
  `flight_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` bigint DEFAULT NULL,
  `recurrence_type` enum('CUSTOM','DAILY','NONE','WEEKLY') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKsefxfswv1twi0kkqi0gjyd6fi` (`flight_id`),
  CONSTRAINT `FKsefxfswv1twi0kkqi0gjyd6fi` FOREIGN KEY (`flight_id`) REFERENCES `flights` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `flights` (
  `aircraft_id` bigint NOT NULL,
  `airline_id` bigint NOT NULL,
  `arrival_airport_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `departure_airport_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `flight_number` varchar(10) NOT NULL,
  `status` enum('ARRIVED','BOARDING','CANCELLED','COMPLETED','DELAYED','DEPARTED','DIVERTED','IN_AIR','LANDED','SCHEDULED') DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `schedule_operating_days` (
  `schedule_id` bigint NOT NULL,
  `day_of_week` enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') DEFAULT NULL,
  KEY `FK4tsfx7j14i09nfkuquqhw6no9` (`schedule_id`),
  CONSTRAINT `FK4tsfx7j14i09nfkuquqhw6no9` FOREIGN KEY (`schedule_id`) REFERENCES `flight_schedules` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
