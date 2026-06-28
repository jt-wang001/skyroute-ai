package com.skyroute.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录结果")
public record AuthVO(
        @Schema(description = "JWT 访问令牌") String token,
        @Schema(description = "用户 ID", example = "1") Long userId,
        @Schema(description = "用户昵称", example = "巡检员") String nickname
) {
}
