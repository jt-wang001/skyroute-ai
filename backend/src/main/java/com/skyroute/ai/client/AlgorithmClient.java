package com.skyroute.ai.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyroute.ai.client.dto.AlgorithmGenerateRouteRequest;
import com.skyroute.ai.client.dto.AlgorithmGenerateRouteResponse;
import com.skyroute.ai.common.exception.BusinessException;
import com.skyroute.ai.config.AlgorithmProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class AlgorithmClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String generateRouteUrl;

    public AlgorithmClient(AlgorithmProperties properties, ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
        this.generateRouteUrl = normalizeBaseUrl(properties.getBaseUrl()) + "/generate-route";
    }

    public AlgorithmGenerateRouteResponse generateRoute(AlgorithmGenerateRouteRequest request) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .uri(URI.create(generateRouteUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                throw new BusinessException(
                        503,
                        buildAlgorithmErrorMessage(httpResponse.body()));
            }

            AlgorithmGenerateRouteResponse response = objectMapper.readValue(
                    httpResponse.body(),
                    AlgorithmGenerateRouteResponse.class);

            if (response == null
                    || !Boolean.TRUE.equals(response.success())
                    || response.waypoints() == null
                    || response.waypoints().isEmpty()) {
                throw new BusinessException(503, "Algorithm service did not return valid waypoints");
            }
            return response;
        } catch (BusinessException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            throw new BusinessException(503, "Failed to serialize or parse algorithm request");
        } catch (IOException exception) {
            throw new BusinessException(
                    503,
                    "Algorithm service is unavailable. Please start Python FastAPI on port 8000");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(503, "Algorithm service call was interrupted");
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(503, "Algorithm service url is invalid");
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8000";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static String buildAlgorithmErrorMessage(String errorBody) {
        if (errorBody == null || errorBody.isBlank()) {
            return "Algorithm service failed. Please check mission area and flight parameters";
        }
        String compactBody = errorBody.replaceAll("\\s+", " ").trim();
        if (compactBody.length() > 500) {
            compactBody = compactBody.substring(0, 500) + "...";
        }
        return "Algorithm service failed: " + compactBody;
    }
}