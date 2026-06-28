package com.skyroute.ai.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "创建飞行任务请求")
public record MissionCreateDTO(
        @NotBlank(message = "任务名称不能为空")
        @Size(max = 128, message = "任务名称不能超过 128 个字符")
        String missionName,

        @NotNull(message = "飞行高度不能为空")
        @DecimalMin(value = "0.01", message = "飞行高度必须大于 0")
        BigDecimal flightAltitude,

        @NotNull(message = "飞行速度不能为空")
        @DecimalMin(value = "0.01", message = "飞行速度必须大于 0")
        BigDecimal flightSpeed,

        @NotNull(message = "航向重叠率不能为空")
        @DecimalMin(value = "0", message = "航向重叠率不能小于 0")
        @DecimalMax(value = "100", message = "航向重叠率不能大于 100")
        BigDecimal headingOverlapRate,

        @NotNull(message = "旁向重叠率不能为空")
        @DecimalMin(value = "0", message = "旁向重叠率不能小于 0")
        @DecimalMax(value = "100", message = "旁向重叠率不能大于 100")
        BigDecimal sideOverlapRate,

        @NotNull(message = "巡检区域不能为空")
        @Schema(description = "标准 GeoJSON Polygon")
        JsonNode areaGeojson
) {
}
