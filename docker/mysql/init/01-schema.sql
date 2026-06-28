CREATE DATABASE IF NOT EXISTS `skyroute_ai`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `skyroute_ai`;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `nickname` VARCHAR(64) NOT NULL,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `mission` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `mission_name` VARCHAR(128) NOT NULL,
  `flight_altitude` DECIMAL(10,2) NOT NULL,
  `flight_speed` DECIMAL(8,2) NOT NULL,
  `heading_overlap_rate` DECIMAL(5,2) NOT NULL,
  `side_overlap_rate` DECIMAL(5,2) NOT NULL,
  `total_distance` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `estimated_duration_sec` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `waypoint_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_mission_user_id` (`user_id`),
  KEY `idx_mission_user_status` (`user_id`, `status`),
  CONSTRAINT `fk_mission_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `chk_mission_altitude` CHECK (`flight_altitude` > 0),
  CONSTRAINT `chk_mission_speed` CHECK (`flight_speed` > 0),
  CONSTRAINT `chk_mission_heading_overlap` CHECK (`heading_overlap_rate` >= 0 AND `heading_overlap_rate` <= 100),
  CONSTRAINT `chk_mission_side_overlap` CHECK (`side_overlap_rate` >= 0 AND `side_overlap_rate` <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `mission_area` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `mission_id` BIGINT UNSIGNED NOT NULL,
  `area_geojson` JSON NOT NULL,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mission_area_mission_id` (`mission_id`),
  CONSTRAINT `fk_mission_area_mission` FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `waypoint` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `mission_id` BIGINT UNSIGNED NOT NULL,
  `sequence_no` INT UNSIGNED NOT NULL,
  `longitude` DECIMAL(10,7) NOT NULL,
  `latitude` DECIMAL(10,7) NOT NULL,
  `altitude` DECIMAL(10,2) NOT NULL,
  `action_type` VARCHAR(32) NOT NULL DEFAULT 'FLY',
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_waypoint_mission_sequence` (`mission_id`, `sequence_no`),
  KEY `idx_waypoint_mission_id` (`mission_id`),
  CONSTRAINT `fk_waypoint_mission` FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `chk_waypoint_sequence` CHECK (`sequence_no` > 0),
  CONSTRAINT `chk_waypoint_longitude` CHECK (`longitude` >= -180 AND `longitude` <= 180),
  CONSTRAINT `chk_waypoint_latitude` CHECK (`latitude` >= -90 AND `latitude` <= 90),
  CONSTRAINT `chk_waypoint_altitude` CHECK (`altitude` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;