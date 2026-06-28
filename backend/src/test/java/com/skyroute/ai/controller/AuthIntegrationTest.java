package com.skyroute.ai.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyroute.ai.entity.UserEntity;
import com.skyroute.ai.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    private final String username = "auth_test_" + System.nanoTime();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @AfterEach
    void cleanUp() {
        userMapper.delete(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, username));
    }

    @Test
    void shouldRegisterLoginAndReadProfileWithJwt() throws Exception {
        ResponseEntity<String> unauthorized = restTemplate.getForEntity("/api/user/profile", String.class);
        assertThat(unauthorized.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> register = restTemplate.postForEntity(
                "/api/auth/register",
                new HttpEntity<>(Map.of(
                        "username", username,
                        "password", "TestPass@123",
                        "nickname", "Auth Tester"), jsonHeaders),
                String.class);
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.OK);

        UserEntity storedUser = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, username));
        assertThat(storedUser).isNotNull();
        assertThat(storedUser.getPassword()).startsWith("$2");
        assertThat(storedUser.getPassword()).isNotEqualTo("TestPass@123");

        ResponseEntity<String> badLogin = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(Map.of(
                        "username", username,
                        "password", "wrong-password"), jsonHeaders),
                String.class);
        assertThat(badLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(Map.of(
                        "username", username,
                        "password", "TestPass@123"), jsonHeaders),
                String.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode loginJson = objectMapper.readTree(login.getBody());
        String token = loginJson.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        assertThat(loginJson.path("data").path("userId").asLong()).isEqualTo(storedUser.getId());

        HttpHeaders bearerHeaders = new HttpHeaders();
        bearerHeaders.setBearerAuth(token);
        ResponseEntity<String> profile = restTemplate.exchange(
                "/api/user/profile",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders),
                String.class);

        assertThat(profile.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode profileJson = objectMapper.readTree(profile.getBody());
        assertThat(profileJson.path("data").path("id").asLong()).isEqualTo(storedUser.getId());
        assertThat(profileJson.path("data").path("nickname").asText()).isEqualTo("Auth Tester");
    }
}
