CREATE DATABASE IF NOT EXISTS `skyroute_ai`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `skyroute_ai`;

CREATE TABLE IF NOT EXISTS `user`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    `username`    VARCHAR(64)     NOT NULL COMMENT '登录用户名',
    `password`    VARCHAR(255)    NOT NULL COMMENT '密码哈希，禁止保存明文密码',
    `nickname`    VARCHAR(64)     NOT NULL COMMENT '用户昵称',
    `create_time` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `update_time` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户表';

CREATE TABLE IF NOT EXISTS `mission`
(
    `id`                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务主键',
    `user_id`                  BIGINT UNSIGNED NOT NULL COMMENT '任务所属用户 ID',
    `mission_name`             VARCHAR(128)    NOT NULL COMMENT '任务名称',
    `flight_altitude`          DECIMAL(10, 2)  NOT NULL COMMENT '飞行高度，单位：米',
    `flight_speed`             DECIMAL(8, 2)   NOT NULL COMMENT '飞行速度，单位：米/秒',
    `heading_overlap_rate`     DECIMAL(5, 2)   NOT NULL COMMENT '航向重叠率，单位：百分比',
    `side_overlap_rate`        DECIMAL(5, 2)   NOT NULL COMMENT '旁向重叠率，单位：百分比',
    `total_distance`           DECIMAL(12, 2)  NOT NULL DEFAULT 0 COMMENT '总航程，单位：米',
    `estimated_duration_sec`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '预计飞行时长，单位：秒',
    `waypoint_count`           INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '航点数量',
    `status`                   VARCHAR(32)     NOT NULL DEFAULT 'DRAFT'
        COMMENT '任务状态：DRAFT/PLANNED/EXECUTING/COMPLETED/CANCELLED/FAILED',
    `create_time`              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `update_time`              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_mission_user_id` (`user_id`),
    KEY `idx_mission_user_status` (`user_id`, `status`),
    CONSTRAINT `fk_mission_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_mission_altitude` CHECK (`flight_altitude` > 0),
    CONSTRAINT `chk_mission_speed` CHECK (`flight_speed` > 0),
    CONSTRAINT `chk_mission_heading_overlap`
        CHECK (`heading_overlap_rate` >= 0 AND `heading_overlap_rate` <= 100),
    CONSTRAINT `chk_mission_side_overlap`
        CHECK (`side_overlap_rate` >= 0 AND `side_overlap_rate` <= 100)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '飞行任务表';

CREATE TABLE IF NOT EXISTS `mission_area`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '巡检区域主键',
    `mission_id`   BIGINT UNSIGNED NOT NULL COMMENT '关联任务 ID',
    `area_geojson` JSON            NOT NULL COMMENT '巡检区域 GeoJSON，通常为 Polygon 或 MultiPolygon',
    `create_time`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `update_time`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mission_area_mission_id` (`mission_id`),
    CONSTRAINT `fk_mission_area_mission`
        FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`)
            ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '任务巡检区域表';

CREATE TABLE IF NOT EXISTS `waypoint`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '航点主键',
    `mission_id`     BIGINT UNSIGNED NOT NULL COMMENT '关联任务 ID',
    `sequence_no`    INT UNSIGNED    NOT NULL COMMENT '航点序号，从 1 开始',
    `longitude`      DECIMAL(10, 7)  NOT NULL COMMENT '经度，范围 -180 至 180',
    `latitude`       DECIMAL(10, 7)  NOT NULL COMMENT '纬度，范围 -90 至 90',
    `altitude`       DECIMAL(10, 2)  NOT NULL COMMENT '航点高度，单位：米',
    `action_type`    VARCHAR(32)     NOT NULL DEFAULT 'FLY'
        COMMENT '动作类型：TAKEOFF/FLY/PHOTO/HOVER/LAND',
    `create_time`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `update_time`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_waypoint_mission_sequence` (`mission_id`, `sequence_no`),
    KEY `idx_waypoint_mission_id` (`mission_id`),
    CONSTRAINT `fk_waypoint_mission`
        FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`)
            ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `chk_waypoint_sequence` CHECK (`sequence_no` > 0),
    CONSTRAINT `chk_waypoint_longitude` CHECK (`longitude` >= -180 AND `longitude` <= 180),
    CONSTRAINT `chk_waypoint_latitude` CHECK (`latitude` >= -90 AND `latitude` <= 90),
    CONSTRAINT `chk_waypoint_altitude` CHECK (`altitude` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '任务航点表';
