package com.skyroute.ai.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "统一接口响应")
public record Result<T>(
        @Schema(description = "业务状态码", example = "200") int code,
        @Schema(description = "响应消息", example = "操作成功") String message,
        @Schema(description = "响应数据") T data,
        @Schema(description = "响应时间") Instant timestamp
) {

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data, Instant.now());
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data, Instant.now());
    }

    public static <T> Result<T> failure(ResultCode resultCode) {
        return failure(resultCode.getCode(), resultCode.getMessage());
    }

    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null, Instant.now());
    }
}
