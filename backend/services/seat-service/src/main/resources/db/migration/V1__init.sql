-- Baseline schema for seat-service, generated from the JPA entities (Hibernate ddl-auto=create + mysqldump --no-data).
-- Never edit this file after it has run: add V2__..., V3__... for later changes.

-- Tables are listed alphabetically, so foreign keys may point to tables created further down.
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `cabin_classes` (
  `display_order` int NOT NULL,
  `is_active` bit(1) NOT NULL,
  `is_bookable` bit(1) NOT NULL,
  `typical_seat_pitch` int DEFAULT NULL,
  `typical_seat_width` int DEFAULT NULL,
  `code` varchar(5) NOT NULL,
  `aircraft_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `seat_type` varchar(255) DEFAULT NULL,
  `name` enum('BUSINESS','ECONOMY','FIRST','PREMIUM_ECONOMY') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `flight_instance_cabins` (
  `booked_seats` int DEFAULT NULL,
  `total_seats` int NOT NULL,
  `cabin_class_id` bigint NOT NULL,
  `flight_instance_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  KEY `FKdia8gbpr7pkyi7ki65trsisyw` (`cabin_class_id`),
  CONSTRAINT `FKdia8gbpr7pkyi7ki65trsisyw` FOREIGN KEY (`cabin_class_id`) REFERENCES `cabin_classes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `seat_instances` (
  `fare` double DEFAULT NULL,
  `premium_surcharge` double DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `flight_instance_cabin_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `seat_maps` (
  `left_seats_per_row` int NOT NULL,
  `right_seats_per_row` int NOT NULL,
  `total_rows` int NOT NULL,
  `airline_id` bigint NOT NULL,
  `cabin_class_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK701xipgrjfahnib4k54r982kt` (`cabin_class_id`),
  CONSTRAINT `FKh63a3y4o6ra7c02t42mqo5e6t` FOREIGN KEY (`cabin_class_id`) REFERENCES `cabin_classes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `seats` (
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
  `id` bigint NOT NULL AUTO_INCREMENT,
  `seat_map_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `seat_number` varchar(10) NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `seat_type` enum('AISLE','EXTRA_LEGROOM','MIDDLE','WINDOW') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKn0lcnvvcrbb3wn5s6ksyc1v9q` (`seat_map_id`),
  CONSTRAINT `FKn0lcnvvcrbb3wn5s6ksyc1v9q` FOREIGN KEY (`seat_map_id`) REFERENCES `seat_maps` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
