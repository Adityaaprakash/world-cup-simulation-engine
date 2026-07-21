package com.aditya.worldcup.tactics.dto;

import com.aditya.worldcup.tactics.entity.BuildUpStyle;
import com.aditya.worldcup.tactics.entity.ChanceCreation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TacticalProfileUpdateRequest(
        @NotNull @Min(1) @Max(100) Integer attackWidth,
        @NotNull @Min(1) @Max(100) Integer defensiveWidth,
        @NotNull @Min(1) @Max(100) Integer defensiveLine,
        @NotNull @Min(1) @Max(100) Integer pressingIntensity,
        @NotNull BuildUpStyle buildUpStyle,
        @NotNull ChanceCreation chanceCreation,
        @NotNull @Min(1) @Max(100) Integer attackingWidth,
        @NotNull @Min(1) @Max(100) Integer crossFrequency,
        @NotNull @Min(1) @Max(100) Integer longBallFrequency,
        @NotNull @Min(1) @Max(100) Integer passingRisk,
        @NotNull Boolean counterAttack,
        @NotNull Boolean highPress,
        @NotNull Boolean offsideTrap,
        @NotNull Boolean timeWasting
) {
}
