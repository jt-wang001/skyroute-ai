package com.skyroute.ai.service;

import com.skyroute.ai.entity.MissionEntity;
import com.skyroute.ai.vo.RiskAssessmentVO;

public interface RiskAssessmentService {

    RiskAssessmentVO assess(MissionEntity mission);
}
