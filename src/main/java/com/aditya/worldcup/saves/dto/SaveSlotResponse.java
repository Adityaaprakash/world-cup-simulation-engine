package com.aditya.worldcup.saves.dto;

import com.aditya.worldcup.managers.entity.ManagerReputation;
import com.aditya.worldcup.saves.entity.SaveType;

import java.time.LocalDateTime;

public record SaveSlotResponse(
        Long id,
        String slotName,
        Integer slotNumber,
        String description,
        SaveType saveType,
        Long currentTournamentId,
        Integer currentSeason,
        String currentStage,
        Long totalPlayTime,
        String formatVersion,
        Integer managerLevel,
        Integer managerExperiencePoints,
        ManagerReputation reputation,
        Integer tournamentsPlayed,
        Integer trophies,
        String currentTeam,
        String currentTournament,
        Double progressPercentage,
        LocalDateTime latestSaveTimestamp,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastPlayedAt,
        Boolean autosave,
        Boolean active,
        Boolean backupAvailable,
        LocalDateTime backupCreatedAt,
        String backupDescription
) {
}
