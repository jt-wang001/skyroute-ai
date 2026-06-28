package com.skyroute.ai.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

@TableName("mission")
public class MissionEntity extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("mission_name")
    private String missionName;

    @TableField("flight_altitude")
    private BigDecimal flightAltitude;

    @TableField("flight_speed")
    private BigDecimal flightSpeed;

    @TableField("heading_overlap_rate")
    private BigDecimal headingOverlapRate;

    @TableField("side_overlap_rate")
    private BigDecimal sideOverlapRate;

    @TableField("total_distance")
    private BigDecimal totalDistance;

    @TableField("estimated_duration_sec")
    private Long estimatedDurationSec;

    @TableField("waypoint_count")
    private Integer waypointCount;

    private String status;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMissionName() {
        return missionName;
    }

    public void setMissionName(String missionName) {
        this.missionName = missionName;
    }

    public BigDecimal getFlightAltitude() {
        return flightAltitude;
    }

    public void setFlightAltitude(BigDecimal flightAltitude) {
        this.flightAltitude = flightAltitude;
    }

    public BigDecimal getFlightSpeed() {
        return flightSpeed;
    }

    public void setFlightSpeed(BigDecimal flightSpeed) {
        this.flightSpeed = flightSpeed;
    }

    public BigDecimal getHeadingOverlapRate() {
        return headingOverlapRate;
    }

    public void setHeadingOverlapRate(BigDecimal headingOverlapRate) {
        this.headingOverlapRate = headingOverlapRate;
    }

    public BigDecimal getSideOverlapRate() {
        return sideOverlapRate;
    }

    public void setSideOverlapRate(BigDecimal sideOverlapRate) {
        this.sideOverlapRate = sideOverlapRate;
    }

    public BigDecimal getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(BigDecimal totalDistance) {
        this.totalDistance = totalDistance;
    }

    public Long getEstimatedDurationSec() {
        return estimatedDurationSec;
    }

    public void setEstimatedDurationSec(Long estimatedDurationSec) {
        this.estimatedDurationSec = estimatedDurationSec;
    }

    public Integer getWaypointCount() {
        return waypointCount;
    }

    public void setWaypointCount(Integer waypointCount) {
        this.waypointCount = waypointCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
