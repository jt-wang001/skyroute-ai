package com.skyroute.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "当前用户资料")
public record UserProfileVO(
        @Schema(description = "用户 ID", example = "1") Long id,
        @Schema(description = "用户名", example = "skyroute_user") String username,
        @Schema(description = "昵称", example = "巡检员") String nickname,
        @Schema(description = "注册时间") LocalDateTime createTime
) {
}
