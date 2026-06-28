package com.skyroute.ai.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyroute.ai.client.AlgorithmClient;
import com.skyroute.ai.client.dto.AlgorithmGenerateRouteResponse;
import com.skyroute.ai.client.dto.AlgorithmWaypoint;
import com.skyroute.ai.common.exception.BusinessException;
import com.skyroute.ai.entity.MissionAreaEntity;
import com.skyroute.ai.entity.MissionEntity;
import com.skyroute.ai.entity.UserEntity;
import com.skyroute.ai.entity.WaypointEntity;
import com.skyroute.ai.mapper.MissionAreaMapper;
import com.skyroute.ai.mapper.MissionMapper;
import com.skyroute.ai.mapper.UserMapper;
import com.skyroute.ai.mapper.WaypointMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MissionIntegrationTest {

    private final String suffix = String.valueOf(System.nanoTime());
    private final String userA = "mission_a_" + suffix;
    private final String userB = "mission_b_" + suffix;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MissionMapper missionMapper;

    @Autowired
    private MissionAreaMapper missionAreaMapper;

    @Autowired
    private WaypointMapper waypointMapper;

    @MockitoBean
    private AlgorithmClient algorithmClient;

    @BeforeEach
    void mockAlgorithmService() {
        when(algorithmClient.generateRoute(any())).thenReturn(new AlgorithmGenerateRouteResponse(
                true,
                "模拟航线生成成功",
                4,
                List.of(
                        waypoint(1, "121.9000000", "30.8900000", "TAKEOFF"),
                        waypoint(2, "121.9100000", "30.8900000", "FLY"),
                        waypoint(3, "121.9100000", "30.9000000", "FLY"),
                        waypoint(4, "121.9000000", "30.8900000", "LAND"))));
    }

    @AfterEach
    void cleanUp() {
        for (String username : new String[]{userA, userB}) {
            UserEntity user = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                    .eq(UserEntity::getUsername, username));
            if (user != null) {
                for (MissionEntity mission : missionMapper.selectList(Wrappers.<MissionEntity>lambdaQuery()
                        .eq(MissionEntity::getUserId, user.getId()))) {
                    waypointMapper.delete(Wrappers.<WaypointEntity>lambdaQuery()
                            .eq(WaypointEntity::getMissionId, mission.getId()));
                    missionAreaMapper.delete(Wrappers.<MissionAreaEntity>lambdaQuery()
                            .eq(MissionAreaEntity::getMissionId, mission.getId()));
                    missionMapper.deleteById(mission.getId());
                }
                userMapper.deleteById(user.getId());
            }
        }
    }

    @Test
    void shouldCreateListGetAndDeleteOwnedMissionOnly() throws Exception {
        register(userA, "用户A");
        register(userB, "用户B");
        String tokenA = login(userA);
        String tokenB = login(userB);

        HttpHeaders headersA = bearerHeaders(tokenA);
        Map<String, Object> request = Map.of(
                "missionName", "集成测试巡检任务",
                "flightAltitude", 80,
                "flightSpeed", 8,
                "headingOverlapRate", 75,
                "sideOverlapRate", 65,
                "areaGeojson", Map.of(
                        "type", "Polygon",
                        "coordinates", new double[][][]{
                                {
                                        {121.90, 30.89},
                                        {121.91, 30.89},
                                        {121.91, 30.90},
                                        {121.90, 30.89}
                                }
                        }));

        ResponseEntity<String> createResponse = restTemplate.postForEntity(
                "/api/missions",
                new HttpEntity<>(request, headersA),
                String.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode createJson = objectMapper.readTree(createResponse.getBody());
        long missionId = createJson.path("data").asLong();
        assertThat(missionId).isPositive();
        MissionEntity createdMission = missionMapper.selectById(missionId);
        assertThat(createdMission).isNotNull();
        assertThat(createdMission.getStatus()).isEqualTo("PLANNED");
        assertThat(createdMission.getWaypointCount()).isEqualTo(4);
        assertThat(missionAreaMapper.selectOne(Wrappers.<MissionAreaEntity>lambdaQuery()
                .eq(MissionAreaEntity::getMissionId, missionId))).isNotNull();
        assertThat(waypointMapper.selectCount(Wrappers.<WaypointEntity>lambdaQuery()
                .eq(WaypointEntity::getMissionId, missionId))).isEqualTo(4);

        ResponseEntity<String> listResponse = restTemplate.exchange(
                "/api/missions", HttpMethod.GET, new HttpEntity<>(headersA), String.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode listJson = objectMapper.readTree(listResponse.getBody());
        assertThat(listJson.path("data").toString()).contains("集成测试巡检任务");

        ResponseEntity<String> detailResponse = restTemplate.exchange(
                "/api/missions/" + missionId,
                HttpMethod.GET,
                new HttpEntity<>(headersA),
                String.class);
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode detailJson = objectMapper.readTree(detailResponse.getBody());
        JsonNode ring = detailJson.path("data").path("areaGeojson").path("coordinates").get(0);
        assertThat(ring.get(0)).isEqualTo(ring.get(ring.size() - 1));
        assertThat(detailJson.path("data").path("waypoints")).hasSize(4);
        assertThat(detailJson.path("data").path("waypoints").get(0).path("actionType").asText())
                .isEqualTo("TAKEOFF");

        HttpHeaders headersB = bearerHeaders(tokenB);
        ResponseEntity<String> forbiddenDetail = restTemplate.exchange(
                "/api/missions/" + missionId,
                HttpMethod.GET,
                new HttpEntity<>(headersB),
                String.class);
        assertThat(forbiddenDetail.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> forbiddenDelete = restTemplate.exchange(
                "/api/missions/" + missionId,
                HttpMethod.DELETE,
                new HttpEntity<>(headersB),
                String.class);
        assertThat(forbiddenDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/api/missions/" + missionId,
                HttpMethod.DELETE,
                new HttpEntity<>(headersA),
                String.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(missionMapper.selectById(missionId)).isNull();
        assertThat(missionAreaMapper.selectOne(Wrappers.<MissionAreaEntity>lambdaQuery()
                .eq(MissionAreaEntity::getMissionId, missionId))).isNull();
        assertThat(waypointMapper.selectCount(Wrappers.<WaypointEntity>lambdaQuery()
                .eq(WaypointEntity::getMissionId, missionId))).isZero();
    }

    @Test
    void shouldRollbackMissionWhenAlgorithmServiceFails() throws Exception {
        register(userA, "用户A");
        String token = login(userA);
        UserEntity user = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, userA));
        when(algorithmClient.generateRoute(any()))
                .thenThrow(new BusinessException(503, "航线规划服务暂不可用"));

        Map<String, Object> request = Map.of(
                "missionName", "算法异常回滚任务",
                "flightAltitude", 80,
                "flightSpeed", 8,
                "headingOverlapRate", 75,
                "sideOverlapRate", 65,
                "areaGeojson", Map.of(
                        "type", "Polygon",
                        "coordinates", new double[][][]{
                                {
                                        {121.90, 30.89},
                                        {121.91, 30.89},
                                        {121.91, 30.90},
                                        {121.90, 30.89}
                                }
                        }));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/missions",
                new HttpEntity<>(request, bearerHeaders(token)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(objectMapper.readTree(response.getBody()).path("message").asText())
                .isEqualTo("航线规划服务暂不可用");
        assertThat(missionMapper.selectCount(Wrappers.<MissionEntity>lambdaQuery()
                .eq(MissionEntity::getUserId, user.getId()))).isZero();
    }
    private AlgorithmWaypoint waypoint(
            int sequence,
            String longitude,
            String latitude,
            String actionType) {
        return new AlgorithmWaypoint(
                sequence,
                new BigDecimal(longitude),
                new BigDecimal(latitude),
                new BigDecimal("80.00"),
                actionType);
    }

    private void register(String username, String nickname) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register",
                new HttpEntity<>(Map.of(
                        "username", username,
                        "password", "TestPass@123",
                        "nickname", nickname), headers),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String login(String username) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(Map.of(
                        "username", username,
                        "password", "TestPass@123"), headers),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody()).path("data").path("token").asText();
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}