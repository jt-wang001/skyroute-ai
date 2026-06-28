package com.skyroute.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "服务健康状态")
public record HealthVO(
        @Schema(description = "状态", example = "UP") String status,
        @Schema(description = "服务名称", example = "skyroute-backend") String service,
        @Schema(description = "服务器时间") Instant time
) {
}
