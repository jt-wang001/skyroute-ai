package com.skyroute.ai.client.dto;

import java.util.List;

public record AlgorithmGenerateRouteResponse(
        Boolean success,
        String message,
        Integer waypointCount,
        List<AlgorithmWaypoint> waypoints
) {
}
