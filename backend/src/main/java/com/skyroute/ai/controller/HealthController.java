package com.skyroute.ai.controller;

import com.skyroute.ai.common.Result;
import com.skyroute.ai.vo.HealthVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@Tag(name = "系统健康检查")
@RestController
@RequestMapping("/api")
public class HealthController {

    @Operation(summary = "检查后端服务是否正常运行")
    @GetMapping("/health")
    public Result<HealthVO> health() {
        return Result.success(new HealthVO("UP", "skyroute-backend", Instant.now()));
    }
}
