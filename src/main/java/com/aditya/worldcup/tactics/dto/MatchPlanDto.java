package com.aditya.worldcup.tactics.dto;

import com.aditya.worldcup.tactics.entity.TacticalApproach;
import lombok.Builder;

@Builder
public record MatchPlanDto(
        Long matchId,
        Long squadId,
        TacticalApproach tacticalApproach,
        Integer pressingIntensity,
        Integer tempo,
        Integer defensiveLine,
        Integer attackingWidth,
        Boolean counterAttack,
        Boolean offsideTrap
) {}
