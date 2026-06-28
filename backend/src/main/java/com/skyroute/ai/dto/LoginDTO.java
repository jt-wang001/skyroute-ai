package com.skyroute.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "用户登录请求")
public record LoginDTO(
        @NotBlank(message = "用户名不能为空")
        @Schema(description = "登录用户名", example = "skyroute_user")
        String username,

        @NotBlank(message = "密码不能为空")
        @Schema(description = "登录密码", example = "SkyRoute@123")
        String password
) {
}
