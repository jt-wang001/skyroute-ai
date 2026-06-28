package com.skyroute.ai.vo;

import com.skyroute.ai.common.RiskLevel;

import java.util.List;

public record RiskAssessmentVO(
        RiskLevel riskLevel,
        List<String> riskMessages
) {
}
