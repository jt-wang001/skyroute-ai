package com.skyroute.ai.service.impl;

import com.skyroute.ai.common.RiskLevel;
import com.skyroute.ai.entity.MissionEntity;
import com.skyroute.ai.service.RiskAssessmentService;
import com.skyroute.ai.vo.RiskAssessmentVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class RiskAssessmentServiceImpl implements RiskAssessmentService {

    private static final BigDecimal MAX_SAFE_ALTITUDE_METERS = BigDecimal.valueOf(120);
    private static final BigDecimal MIN_RECOMMENDED_ALTITUDE_METERS = BigDecimal.valueOf(30);
    private static final BigDecimal MAX_RECOMMENDED_SPEED_MPS = BigDecimal.valueOf(15);
    private static final long MAX_RECOMMENDED_DURATION_SECONDS = 25 * 60L;
    private static final int MAX_RECOMMENDED_WAYPOINT_COUNT = 200;

    private final List<RiskRule> rules = List.of(
            new RiskRule(
                    RiskLevel.HIGH,
                    mission -> mission.getFlightAltitude() != null
                            && mission.getFlightAltitude().compareTo(MAX_SAFE_ALTITUDE_METERS) > 0,
                    mission -> "飞行高度超过 120 米，存在合规风险"),
            new RiskRule(
                    RiskLevel.MEDIUM,
                    mission -> mission.getFlightAltitude() != null
                            && mission.getFlightAltitude().compareTo(MIN_RECOMMENDED_ALTITUDE_METERS) < 0,
                    mission -> "飞行高度过低，障碍物避让安全余量不足"),
            new RiskRule(
                    RiskLevel.MEDIUM,
                    mission -> mission.getFlightSpeed() != null
                            && mission.getFlightSpeed().compareTo(MAX_RECOMMENDED_SPEED_MPS) > 0,
                    mission -> "飞行速度超过 15 m/s，控制稳定性和影像质量风险增加"),
            new RiskRule(
                    RiskLevel.MEDIUM,
                    mission -> mission.getEstimatedDurationSec() != null
                            && mission.getEstimatedDurationSec() > MAX_RECOMMENDED_DURATION_SECONDS,
                    mission -> "预计飞行时间超过 25 分钟，存在电量续航风险"),
            new RiskRule(
                    RiskLevel.HIGH,
                    mission -> mission.getWaypointCount() != null
                            && mission.getWaypointCount() > MAX_RECOMMENDED_WAYPOINT_COUNT,
                    mission -> "航点数超过 200 个，航线复杂度较高")
    );

    @Override
    public RiskAssessmentVO assess(MissionEntity mission) {
        RiskLevel riskLevel = RiskLevel.LOW;
        List<String> riskMessages = new ArrayList<>();

        for (RiskRule rule : rules) {
            if (rule.matches(mission)) {
                riskMessages.add(rule.message(mission));
                riskLevel = max(riskLevel, rule.level());
            }
        }

        if (riskMessages.isEmpty()) {
            riskMessages.add("未发现明显任务风险");
        }

        return new RiskAssessmentVO(riskLevel, riskMessages);
    }

    private RiskLevel max(RiskLevel current, RiskLevel candidate) {
        return candidate.ordinal() > current.ordinal() ? candidate : current;
    }

    private record RiskRule(
            RiskLevel level,
            RiskCondition condition,
            RiskMessage message
    ) {
        boolean matches(MissionEntity mission) {
            return condition.matches(mission);
        }

        String message(MissionEntity mission) {
            return message.build(mission);
        }
    }

    @FunctionalInterface
    private interface RiskCondition {
        boolean matches(MissionEntity mission);
    }

    @FunctionalInterface
    private interface RiskMessage {
        String build(MissionEntity mission);
    }
}
