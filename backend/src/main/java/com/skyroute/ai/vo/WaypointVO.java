package com.skyroute.ai.vo;

import java.math.BigDecimal;

public record WaypointVO(
        Long id,
        Integer sequenceNo,
        BigDecimal longitude,
        BigDecimal latitude,
        BigDecimal altitude,
        String actionType
) {
}
