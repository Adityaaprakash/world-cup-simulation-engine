package com.aditya.worldcup.saves.dto;

import com.aditya.worldcup.managers.entity.ManagerReputation;

import java.time.LocalDateTime;

public record ResumeSaveResponse(
        Long saveId,
        Long managerId,
        Long currentTournamentId,
        String currentStage,
        Integer managerLevel,
        Integer managerExperiencePoints,
        ManagerReputation reputation,
        Long restoredSquadSelections,
        Long restoredPlayerStates,
        Long restoredTacticalProfiles,
        LocalDateTime resumedAt,
        String message
) {
}
