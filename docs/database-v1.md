# SkyRoute AI 数据库设计 V1

数据库：MySQL 8.x  
字符集：`utf8mb4`  
排序规则：`utf8mb4_0900_ai_ci`

完整建表脚本位于：

```text
backend/src/main/resources/db/schema-v1.sql
```

## 表关系

```text
user 1 ────── N mission
                  │
                  ├──── 1 mission_area
                  │
                  └──── N waypoint
```

- 一个用户可以创建多个飞行任务。
- 一个任务最多保存一条巡检区域记录，由 `mission_area.mission_id` 唯一索引保证。
- 一个任务可以包含多个航点。
- 同一任务内航点序号不能重复，由 `(mission_id, sequence_no)` 唯一索引保证。
- 删除任务时级联删除对应区域和航点。
- 存在任务的用户不能直接删除，避免任务失去所属用户。

## user

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT UNSIGNED | 用户主键 |
| `username` | VARCHAR(64) | 登录用户名，全局唯一 |
| `password` | VARCHAR(255) | 密码哈希，禁止存储明文 |
| `nickname` | VARCHAR(64) | 用户昵称 |
| `create_time` | DATETIME(3) | 创建时间 |
| `update_time` | DATETIME(3) | 更新时间 |

## mission

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT UNSIGNED | 任务主键 |
| `user_id` | BIGINT UNSIGNED | 所属用户 ID |
| `mission_name` | VARCHAR(128) | 任务名称 |
| `flight_altitude` | DECIMAL(10,2) | 飞行高度，米 |
| `flight_speed` | DECIMAL(8,2) | 飞行速度，米/秒 |
| `heading_overlap_rate` | DECIMAL(5,2) | 航向重叠率，0–100 |
| `side_overlap_rate` | DECIMAL(5,2) | 旁向重叠率，0–100 |
| `total_distance` | DECIMAL(12,2) | 总航程，米 |
| `estimated_duration_sec` | BIGINT UNSIGNED | 预计飞行时长，秒 |
| `waypoint_count` | INT UNSIGNED | 航点数量 |
| `status` | VARCHAR(32) | 任务状态 |
| `create_time` | DATETIME(3) | 创建时间 |
| `update_time` | DATETIME(3) | 更新时间 |

任务状态建议：

- `DRAFT`：草稿
- `PLANNED`：航线已生成
- `EXECUTING`：执行中
- `COMPLETED`：已完成
- `CANCELLED`：已取消
- `FAILED`：执行失败

## mission_area

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT UNSIGNED | 区域主键 |
| `mission_id` | BIGINT UNSIGNED | 关联任务 ID，一对一 |
| `area_geojson` | JSON | Polygon 或 MultiPolygon GeoJSON |
| `create_time` | DATETIME(3) | 创建时间 |
| `update_time` | DATETIME(3) | 更新时间 |

V1 使用 GeoJSON 原文保存区域，方便前端地图回显和算法服务调用。后续需要数据库空间查询时，可增加 MySQL `POLYGON` 字段和空间索引。

## waypoint

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT UNSIGNED | 航点主键 |
| `mission_id` | BIGINT UNSIGNED | 关联任务 ID |
| `sequence_no` | INT UNSIGNED | 航点序号，从 1 开始 |
| `longitude` | DECIMAL(10,7) | 经度 |
| `latitude` | DECIMAL(10,7) | 纬度 |
| `altitude` | DECIMAL(10,2) | 航点高度，米 |
| `action_type` | VARCHAR(32) | 航点动作类型 |
| `create_time` | DATETIME(3) | 创建时间 |
| `update_time` | DATETIME(3) | 更新时间 |

动作类型建议：

- `TAKEOFF`：起飞
- `FLY`：普通飞行
- `PHOTO`：拍照
- `HOVER`：悬停
- `LAND`：降落

## 设计约定

- Java 金额式精度字段使用 `BigDecimal`，不使用 `double`。
- 密码字段只保存 BCrypt 或 Argon2 等哈希值。
- SQL 外键用于保证 V1 数据一致性；批量导入航点时应使用事务。
- `mission.waypoint_count` 是冗余统计字段，保存航点时必须与实际数量同步更新。
- 时间字段由 MySQL 默认值和 `ON UPDATE` 维护。
