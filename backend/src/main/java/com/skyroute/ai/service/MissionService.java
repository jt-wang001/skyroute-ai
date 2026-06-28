package com.skyroute.ai.service;

import com.skyroute.ai.dto.MissionCreateDTO;
import com.skyroute.ai.vo.MissionDetailVO;
import com.skyroute.ai.vo.MissionListVO;

import java.util.List;

public interface MissionService {

    Long createMission(MissionCreateDTO createDTO);

    List<MissionListVO> listCurrentUserMissions();

    MissionDetailVO getCurrentUserMission(Long missionId);

    String exportCurrentUserMissionGeoJson(Long missionId);

    void deleteCurrentUserMission(Long missionId);
}
