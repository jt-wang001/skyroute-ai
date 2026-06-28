package com.skyroute.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skyroute.ai.client.AlgorithmClient;
import com.skyroute.ai.client.dto.AlgorithmGenerateRouteRequest;
import com.skyroute.ai.client.dto.AlgorithmGenerateRouteResponse;
import com.skyroute.ai.client.dto.AlgorithmWaypoint;
import com.skyroute.ai.common.UserContext;
import com.skyroute.ai.common.exception.BusinessException;
import com.skyroute.ai.dto.MissionCreateDTO;
import com.skyroute.ai.entity.MissionAreaEntity;
import com.skyroute.ai.entity.MissionEntity;
import com.skyroute.ai.entity.WaypointEntity;
import com.skyroute.ai.mapper.MissionAreaMapper;
import com.skyroute.ai.mapper.MissionMapper;
import com.skyroute.ai.mapper.WaypointMapper;
import com.skyroute.ai.service.MissionService;
import com.skyroute.ai.service.RiskAssessmentService;
import com.skyroute.ai.vo.MissionDetailVO;
import com.skyroute.ai.vo.MissionListVO;
import com.skyroute.ai.vo.WaypointVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class MissionServiceImpl implements MissionService {

    private static final String INITIAL_STATUS = "DRAFT";
    private static final String PLANNED_STATUS = "PLANNED";
    private static final double EARTH_RADIUS_METERS = 6_371_000D;

    private final MissionMapper missionMapper;
    private final MissionAreaMapper missionAreaMapper;
    private final WaypointMapper waypointMapper;
    private final AlgorithmClient algorithmClient;
    private final RiskAssessmentService riskAssessmentService;
    private final ObjectMapper objectMapper;

    public MissionServiceImpl(
            MissionMapper missionMapper,
            MissionAreaMapper missionAreaMapper,
            WaypointMapper waypointMapper,
            AlgorithmClient algorithmClient,
            RiskAssessmentService riskAssessmentService,
            ObjectMapper objectMapper) {
        this.missionMapper = missionMapper;
        this.missionAreaMapper = missionAreaMapper;
        this.waypointMapper = waypointMapper;
        this.algorithmClient = algorithmClient;
        this.riskAssessmentService = riskAssessmentService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Long createMission(MissionCreateDTO createDTO) {
        validateGeoJsonPolygon(createDTO.areaGeojson());
        Long userId = UserContext.requireUserId();

        MissionEntity mission = new MissionEntity();
        mission.setUserId(userId);
        mission.setMissionName(createDTO.missionName().trim());
        mission.setFlightAltitude(createDTO.flightAltitude());
        mission.setFlightSpeed(createDTO.flightSpeed());
        mission.setHeadingOverlapRate(createDTO.headingOverlapRate());
        mission.setSideOverlapRate(createDTO.sideOverlapRate());
        mission.setTotalDistance(BigDecimal.ZERO);
        mission.setEstimatedDurationSec(0L);
        mission.setWaypointCount(0);
        mission.setStatus(INITIAL_STATUS);
        missionMapper.insert(mission);

        MissionAreaEntity missionArea = new MissionAreaEntity();
        missionArea.setMissionId(mission.getId());
        missionArea.setAreaGeojson(writeJson(createDTO.areaGeojson()));
        missionAreaMapper.insert(missionArea);

        AlgorithmGenerateRouteResponse routeResponse = algorithmClient.generateRoute(
                new AlgorithmGenerateRouteRequest(
                        createDTO.areaGeojson(),
                        createDTO.flightAltitude(),
                        createDTO.flightSpeed(),
                        createDTO.sideOverlapRate()));
        saveWaypoints(mission.getId(), routeResponse.waypoints());

        BigDecimal totalDistance = calculateTotalDistance(routeResponse.waypoints());
        mission.setTotalDistance(totalDistance);
        mission.setEstimatedDurationSec(calculateEstimatedDuration(totalDistance, mission.getFlightSpeed()));
        mission.setWaypointCount(routeResponse.waypoints().size());
        mission.setStatus(PLANNED_STATUS);
        missionMapper.updateById(mission);

        return mission.getId();
    }

    @Override
    public List<MissionListVO> listCurrentUserMissions() {
        Long userId = UserContext.requireUserId();
        return missionMapper.selectList(Wrappers.<MissionEntity>lambdaQuery()
                        .eq(MissionEntity::getUserId, userId)
                        .orderByDesc(MissionEntity::getCreateTime))
                .stream()
                .map(this::toListVO)
                .toList();
    }

    @Override
    public MissionDetailVO getCurrentUserMission(Long missionId) {
        MissionEntity mission = requireOwnedMission(missionId);
        MissionAreaEntity area = missionAreaMapper.selectOne(Wrappers.<MissionAreaEntity>lambdaQuery()
                .eq(MissionAreaEntity::getMissionId, missionId));
        if (area == null) {
            throw new BusinessException(500, "Mission area data does not exist");
        }

        List<WaypointVO> waypoints = waypointMapper.selectList(
                        Wrappers.<WaypointEntity>lambdaQuery()
                                .eq(WaypointEntity::getMissionId, missionId)
                                .orderByAsc(WaypointEntity::getSequenceNo))
                .stream()
                .map(this::toWaypointVO)
                .toList();
        return toDetailVO(mission, readJson(area.getAreaGeojson()), waypoints);
    }


    @Override
    public String exportCurrentUserMissionGeoJson(Long missionId) {
        MissionEntity mission = requireOwnedMission(missionId);
        MissionAreaEntity area = missionAreaMapper.selectOne(Wrappers.<MissionAreaEntity>lambdaQuery()
                .eq(MissionAreaEntity::getMissionId, missionId));
        if (area == null) {
            throw new BusinessException(500, "Mission area data does not exist");
        }

        List<WaypointEntity> waypoints = waypointMapper.selectList(
                Wrappers.<WaypointEntity>lambdaQuery()
                        .eq(WaypointEntity::getMissionId, missionId)
                        .orderByAsc(WaypointEntity::getSequenceNo));
        if (waypoints.size() < 2) {
            throw new BusinessException(500, "Mission waypoint data is insufficient for route export");
        }

        ObjectNode featureCollection = objectMapper.createObjectNode();
        featureCollection.put("type", "FeatureCollection");

        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("missionId", mission.getId());
        metadata.put("missionName", mission.getMissionName());
        metadata.put("status", mission.getStatus());
        metadata.put("flightAltitude", mission.getFlightAltitude());
        metadata.put("flightSpeed", mission.getFlightSpeed());
        metadata.put("headingOverlapRate", mission.getHeadingOverlapRate());
        metadata.put("sideOverlapRate", mission.getSideOverlapRate());
        metadata.put("totalDistance", mission.getTotalDistance());
        metadata.put("estimatedDurationSec", mission.getEstimatedDurationSec());
        metadata.put("waypointCount", mission.getWaypointCount());
        featureCollection.set("metadata", metadata);

        ArrayNode features = objectMapper.createArrayNode();
        features.add(buildAreaFeature(mission, readJson(area.getAreaGeojson())));
        features.add(buildRouteFeature(mission, waypoints));
        waypoints.forEach(waypoint -> features.add(buildWaypointFeature(mission, waypoint)));
        featureCollection.set("features", features);

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(featureCollection);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "Mission GeoJSON export failed");
        }
    }
    @Override
    @Transactional
    public void deleteCurrentUserMission(Long missionId) {
        MissionEntity mission = requireOwnedMission(missionId);
        waypointMapper.delete(Wrappers.<WaypointEntity>lambdaQuery()
                .eq(WaypointEntity::getMissionId, mission.getId()));
        missionAreaMapper.delete(Wrappers.<MissionAreaEntity>lambdaQuery()
                .eq(MissionAreaEntity::getMissionId, mission.getId()));
        missionMapper.deleteById(mission.getId());
    }


    private ObjectNode buildAreaFeature(MissionEntity mission, JsonNode areaGeojson) {
        ObjectNode feature = objectMapper.createObjectNode();
        feature.put("type", "Feature");
        feature.set("geometry", areaGeojson);

        ObjectNode properties = objectMapper.createObjectNode();
        properties.put("featureType", "mission_area");
        properties.put("missionId", mission.getId());
        properties.put("missionName", mission.getMissionName());
        feature.set("properties", properties);
        return feature;
    }

    private ObjectNode buildRouteFeature(MissionEntity mission, List<WaypointEntity> waypoints) {
        ObjectNode feature = objectMapper.createObjectNode();
        feature.put("type", "Feature");

        ObjectNode geometry = objectMapper.createObjectNode();
        geometry.put("type", "LineString");
        ArrayNode coordinates = objectMapper.createArrayNode();
        waypoints.forEach(waypoint -> coordinates.add(toPosition(waypoint)));
        geometry.set("coordinates", coordinates);
        feature.set("geometry", geometry);

        ObjectNode properties = objectMapper.createObjectNode();
        properties.put("featureType", "route_line");
        properties.put("missionId", mission.getId());
        properties.put("missionName", mission.getMissionName());
        properties.put("distanceMeters", mission.getTotalDistance());
        properties.put("estimatedTimeSeconds", mission.getEstimatedDurationSec());
        properties.put("waypointCount", waypoints.size());
        feature.set("properties", properties);
        return feature;
    }

    private ObjectNode buildWaypointFeature(MissionEntity mission, WaypointEntity waypoint) {
        ObjectNode feature = objectMapper.createObjectNode();
        feature.put("type", "Feature");

        ObjectNode geometry = objectMapper.createObjectNode();
        geometry.put("type", "Point");
        geometry.set("coordinates", toPosition(waypoint));
        feature.set("geometry", geometry);

        ObjectNode properties = objectMapper.createObjectNode();
        properties.put("featureType", "waypoint");
        properties.put("missionId", mission.getId());
        properties.put("missionName", mission.getMissionName());
        properties.put("waypointId", waypoint.getId());
        properties.put("sequenceNo", waypoint.getSequenceNo());
        properties.put("altitude", waypoint.getAltitude());
        properties.put("actionType", waypoint.getActionType());
        feature.set("properties", properties);
        return feature;
    }

    private ArrayNode toPosition(WaypointEntity waypoint) {
        ArrayNode position = objectMapper.createArrayNode();
        position.add(waypoint.getLongitude());
        position.add(waypoint.getLatitude());
        position.add(waypoint.getAltitude());
        return position;
    }
    private void saveWaypoints(Long missionId, List<AlgorithmWaypoint> waypoints) {
        for (AlgorithmWaypoint waypoint : waypoints) {
            validateAlgorithmWaypoint(waypoint);
            WaypointEntity entity = new WaypointEntity();
            entity.setMissionId(missionId);
            entity.setSequenceNo(waypoint.sequence());
            entity.setLongitude(waypoint.longitude());
            entity.setLatitude(waypoint.latitude());
            entity.setAltitude(waypoint.altitude());
            entity.setActionType(waypoint.actionType());
            waypointMapper.insert(entity);
        }
    }

    private void validateAlgorithmWaypoint(AlgorithmWaypoint waypoint) {
        if (waypoint == null
                || waypoint.sequence() == null
                || waypoint.sequence() <= 0
                || waypoint.longitude() == null
                || waypoint.latitude() == null
                || waypoint.altitude() == null
                || waypoint.actionType() == null
                || waypoint.actionType().isBlank()) {
            throw new BusinessException(503, "Algorithm service returned incomplete waypoint data");
        }
        if (waypoint.longitude().compareTo(BigDecimal.valueOf(-180)) < 0
                || waypoint.longitude().compareTo(BigDecimal.valueOf(180)) > 0
                || waypoint.latitude().compareTo(BigDecimal.valueOf(-90)) < 0
                || waypoint.latitude().compareTo(BigDecimal.valueOf(90)) > 0
                || waypoint.altitude().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(503, "Algorithm service returned waypoint data out of valid range");
        }
    }

    private BigDecimal calculateTotalDistance(List<AlgorithmWaypoint> waypoints) {
        double totalMeters = 0D;
        for (int index = 1; index < waypoints.size(); index++) {
            AlgorithmWaypoint previous = waypoints.get(index - 1);
            AlgorithmWaypoint current = waypoints.get(index);
            totalMeters += haversineDistance(
                    previous.latitude().doubleValue(),
                    previous.longitude().doubleValue(),
                    current.latitude().doubleValue(),
                    current.longitude().doubleValue());
        }
        return BigDecimal.valueOf(totalMeters).setScale(2, RoundingMode.HALF_UP);
    }

    private double haversineDistance(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2) {
        double latitudeDelta = Math.toRadians(latitude2 - latitude1);
        double longitudeDelta = Math.toRadians(longitude2 - longitude1);
        double startLatitude = Math.toRadians(latitude1);
        double endLatitude = Math.toRadians(latitude2);
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(startLatitude) * Math.cos(endLatitude)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private long calculateEstimatedDuration(BigDecimal distance, BigDecimal speed) {
        return distance.divide(speed, 0, RoundingMode.CEILING).longValue();
    }

    private MissionEntity requireOwnedMission(Long missionId) {
        MissionEntity mission = missionMapper.selectOne(Wrappers.<MissionEntity>lambdaQuery()
                .eq(MissionEntity::getId, missionId)
                .eq(MissionEntity::getUserId, UserContext.requireUserId()));
        if (mission == null) {
            throw new BusinessException(404, "Mission does not exist");
        }
        return mission;
    }

    private void validateGeoJsonPolygon(JsonNode geoJson) {
        if (!geoJson.isObject() || !"Polygon".equals(geoJson.path("type").asText())) {
            throw new BusinessException(400, "Mission area must be a GeoJSON Polygon");
        }

        JsonNode coordinates = geoJson.path("coordinates");
        if (!coordinates.isArray() || coordinates.isEmpty() || !coordinates.get(0).isArray()) {
            throw new BusinessException(400, "GeoJSON Polygon coordinates format is invalid");
        }

        JsonNode ring = coordinates.get(0);
        if (ring.size() < 4) {
            throw new BusinessException(400, "GeoJSON Polygon needs at least 3 vertices and must be closed");
        }

        JsonNode first = ring.get(0);
        JsonNode last = ring.get(ring.size() - 1);
        if (!isCoordinate(first) || !isCoordinate(last)
                || first.get(0).asDouble() != last.get(0).asDouble()
                || first.get(1).asDouble() != last.get(1).asDouble()) {
            throw new BusinessException(400, "GeoJSON Polygon first and last coordinates must be equal");
        }

        for (JsonNode coordinate : ring) {
            if (!isCoordinate(coordinate)) {
                throw new BusinessException(400, "GeoJSON Polygon coordinate format is invalid");
            }
            double longitude = coordinate.get(0).asDouble();
            double latitude = coordinate.get(1).asDouble();
            if (longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
                throw new BusinessException(400, "GeoJSON Polygon longitude or latitude is out of valid range");
            }
        }
    }

    private boolean isCoordinate(JsonNode coordinate) {
        return coordinate.isArray()
                && coordinate.size() >= 2
                && coordinate.get(0).isNumber()
                && coordinate.get(1).isNumber();
    }

    private String writeJson(JsonNode jsonNode) {
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(400, "GeoJSON serialization failed");
        }
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "Mission area data parse failed");
        }
    }

    private MissionListVO toListVO(MissionEntity mission) {
        return new MissionListVO(
                mission.getId(),
                mission.getMissionName(),
                mission.getFlightAltitude(),
                mission.getFlightSpeed(),
                mission.getHeadingOverlapRate(),
                mission.getSideOverlapRate(),
                mission.getTotalDistance(),
                mission.getEstimatedDurationSec(),
                mission.getWaypointCount(),
                mission.getStatus(),
                mission.getCreateTime(),
                mission.getUpdateTime());
    }

    private MissionDetailVO toDetailVO(
            MissionEntity mission,
            JsonNode areaGeojson,
            List<WaypointVO> waypoints) {
        return new MissionDetailVO(
                mission.getId(),
                mission.getUserId(),
                mission.getMissionName(),
                mission.getFlightAltitude(),
                mission.getFlightSpeed(),
                mission.getHeadingOverlapRate(),
                mission.getSideOverlapRate(),
                mission.getTotalDistance(),
                mission.getEstimatedDurationSec(),
                mission.getWaypointCount(),
                mission.getStatus(),
                areaGeojson,
                waypoints,
                riskAssessmentService.assess(mission),
                mission.getCreateTime(),
                mission.getUpdateTime());
    }

    private WaypointVO toWaypointVO(WaypointEntity waypoint) {
        return new WaypointVO(
                waypoint.getId(),
                waypoint.getSequenceNo(),
                waypoint.getLongitude(),
                waypoint.getLatitude(),
                waypoint.getAltitude(),
                waypoint.getActionType());
    }
}
