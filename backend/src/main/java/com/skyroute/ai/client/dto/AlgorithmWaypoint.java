package com.skyroute.ai.client.dto;

import java.math.BigDecimal;

public record AlgorithmWaypoint(
        Integer sequence,
        BigDecimal longitude,
        BigDecimal latitude,
        BigDecimal altitude,
        String actionType
) {
}
