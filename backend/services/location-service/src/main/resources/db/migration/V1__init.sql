-- Baseline schema for location-service, generated from the JPA entities (Hibernate ddl-auto=create + mysqldump --no-data).
-- Never edit this file after it has run: add V2__..., V3__... for later changes.

-- Tables are listed alphabetically, so foreign keys may point to tables created further down.
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `airports` (
  `airlines_count` int DEFAULT NULL,
  `annual_passengers` double DEFAULT NULL,
  `destinations_count` int DEFAULT NULL,
  `iata_code` varchar(3) NOT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `on_time_performance` double DEFAULT NULL,
  `traveler_score` int DEFAULT NULL,
  `city_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `cities` (
  `country_code` varchar(5) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
