package com.aditya.worldcup.managers.dto;

import com.aditya.worldcup.managers.entity.CoachingStyle;
import com.aditya.worldcup.managers.entity.ManagerReputation;

import java.time.LocalDateTime;

public record ManagerResponse(
        Long id,
        String username,
        String displayName,
        String nationality,
        String favoriteFormation,
        String favoriteTacticalProfile,
        CoachingStyle coachingStyle,
        ManagerReputation reputation,
        Integer experiencePoints,
        Integer level,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
