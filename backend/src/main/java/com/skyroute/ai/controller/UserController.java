package com.skyroute.ai.controller;

import com.skyroute.ai.common.Result;
import com.skyroute.ai.service.AuthService;
import com.skyroute.ai.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户中心")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "获取当前用户资料")
    @GetMapping("/profile")
    public Result<UserProfileVO> profile() {
        return Result.success(authService.getCurrentUserProfile());
    }
}
