package com.aditya.worldcup.managers.dto;

import com.aditya.worldcup.managers.entity.AchievementCode;
import com.aditya.worldcup.managers.entity.ManagerBadge;

import java.time.LocalDateTime;

public record AchievementResponse(
        AchievementCode code,
        String title,
        String description,
        ManagerBadge badge,
        LocalDateTime unlockedAt
) {
}
