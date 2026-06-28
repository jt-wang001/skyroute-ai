package com.skyroute.ai.controller;

import com.skyroute.ai.common.Result;
import com.skyroute.ai.dto.MissionCreateDTO;
import com.skyroute.ai.service.MissionService;
import com.skyroute.ai.vo.MissionDetailVO;
import com.skyroute.ai.vo.MissionListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "Mission Management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @Operation(summary = "Create mission")
    @PostMapping
    public Result<Long> createMission(@Valid @RequestBody MissionCreateDTO createDTO) {
        return Result.success("Mission created successfully", missionService.createMission(createDTO));
    }

    @Operation(summary = "List current user missions")
    @GetMapping
    public Result<List<MissionListVO>> listMissions() {
        return Result.success(missionService.listCurrentUserMissions());
    }

    @Operation(summary = "Get current user mission detail")
    @GetMapping("/{id}")
    public Result<MissionDetailVO> getMission(@PathVariable Long id) {
        return Result.success(missionService.getCurrentUserMission(id));
    }

    @Operation(summary = "Export mission as GeoJSON")
    @GetMapping("/{id}/export/geojson")
    public ResponseEntity<byte[]> exportMissionGeoJson(@PathVariable Long id) {
        String geoJson = missionService.exportCurrentUserMissionGeoJson(id);
        byte[] body = geoJson.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/geo+json;charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("mission-" + id + ".geojson", StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(body.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(body);
    }

    @Operation(summary = "Delete current user mission")
    @DeleteMapping("/{id}")
    public Result<Void> deleteMission(@PathVariable Long id) {
        missionService.deleteCurrentUserMission(id);
        return Result.success("Mission deleted successfully", null);
    }
}
