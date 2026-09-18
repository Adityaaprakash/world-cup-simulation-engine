package com.aditya.worldcup.players.dto;

import com.aditya.worldcup.players.entity.InjuryStatus;

public record PlayerDetailsResponse(
        Long id,
        String name,
        String nationality,
        String position,
        Integer age,
        Integer overallRating,
        Integer potential,
        Integer pace,
        Integer shooting,
        Integer passing,
        Integer dribbling,
        Integer defending,
        Integer physical,
        String preferredFoot,
        Boolean active,
        Boolean retired,
        Integer currentForm,
        Integer fitness,
        Integer fatigue,
        InjuryStatus injuryStatus,
        Boolean available
) {}
