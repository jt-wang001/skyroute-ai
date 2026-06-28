package com.skyroute.ai.client.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record AlgorithmGenerateRouteRequest(
        JsonNode areaGeojson,
        BigDecimal flightHeight,
        BigDecimal flightSpeed,
        BigDecimal sideOverlap
) {
}
