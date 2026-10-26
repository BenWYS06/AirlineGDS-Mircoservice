-- Baseline schema for pricing-service, generated from the JPA entities (Hibernate ddl-auto=create + mysqldump --no-data).
-- Never edit this file after it has run: add V2__..., V3__... for later changes.

-- Tables are listed alphabetically, so foreign keys may point to tables created further down.
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `baggage_policies` (
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
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1lbpkmfrho2eb36xo0ouywl1w` (`fare_id`),
  CONSTRAINT `FKhqu96eusm22uksffyfo50lsc3` FOREIGN KEY (`fare_id`) REFERENCES `fares` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `fare_rules` (
  `cancellation_fee` double DEFAULT NULL,
  `change_deadline_hours` int DEFAULT NULL,
  `change_fee` double DEFAULT NULL,
  `is_changeable` bit(1) DEFAULT NULL,
  `is_refundable` bit(1) DEFAULT NULL,
  `refund_deadline_days` int DEFAULT NULL,
  `airline_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `fare_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `rule_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKdo45693odksp1byrcdc759uah` (`fare_id`),
  CONSTRAINT `FKhoxt2ycj9ctheeqmg7aqw3y0l` FOREIGN KEY (`fare_id`) REFERENCES `fares` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `fares` (
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
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `fare_label` varchar(100) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `cabin_class` enum('BUSINESS','ECONOMY','FIRST','PREMIUM_ECONOMY') DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
