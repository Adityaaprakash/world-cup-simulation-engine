package com.aditya.worldcup.tactics.dto;

import com.aditya.worldcup.tactics.entity.AttackingApproach;
import com.aditya.worldcup.tactics.entity.BuildUpStyle;
import com.aditya.worldcup.tactics.entity.DefensiveBlock;
import com.aditya.worldcup.tactics.entity.DefensiveLine;
import com.aditya.worldcup.tactics.entity.PassingStyle;
import com.aditya.worldcup.tactics.entity.PressingIntensity;
import com.aditya.worldcup.tactics.entity.Tempo;
import com.aditya.worldcup.tactics.entity.Width;

public record TacticalProfileResponse(
        Long id,
        Long teamId,
        PressingIntensity pressingIntensity,
        DefensiveLine defensiveLine,
        Tempo tempo,
        Width width,
        PassingStyle passingStyle,
        AttackingApproach attackingApproach,
        BuildUpStyle buildUpStyle,
        DefensiveBlock defensiveBlock
) {
}
