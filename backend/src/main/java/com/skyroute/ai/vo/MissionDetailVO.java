package com.skyroute.ai.vo;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MissionDetailVO(
        Long id,
        Long userId,
        String missionName,
        BigDecimal flightAltitude,
        BigDecimal flightSpeed,
        BigDecimal headingOverlapRate,
        BigDecimal sideOverlapRate,
        BigDecimal totalDistance,
        Long estimatedDurationSec,
        Integer waypointCount,
        String status,
        JsonNode areaGeojson,
        List<WaypointVO> waypoints,
        RiskAssessmentVO riskAssessment,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
