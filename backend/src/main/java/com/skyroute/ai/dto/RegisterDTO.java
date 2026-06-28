package com.skyroute.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "用户注册请求")
public record RegisterDTO(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 4, max = 64, message = "用户名长度必须为 4-64 个字符")
        @Schema(description = "登录用户名", example = "skyroute_user")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度必须为 6-64 个字符")
        @Schema(description = "登录密码", example = "SkyRoute@123")
        String password,

        @NotBlank(message = "昵称不能为空")
        @Size(max = 64, message = "昵称不能超过 64 个字符")
        @Schema(description = "用户昵称", example = "巡检员")
        String nickname
) {
}
