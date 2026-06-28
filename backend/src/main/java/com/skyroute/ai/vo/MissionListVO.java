package com.skyroute.ai.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MissionListVO(
        Long id,
        String missionName,
        BigDecimal flightAltitude,
        BigDecimal flightSpeed,
        BigDecimal headingOverlapRate,
        BigDecimal sideOverlapRate,
        BigDecimal totalDistance,
        Long estimatedDurationSec,
        Integer waypointCount,
        String status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
