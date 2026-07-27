package com.aditya.worldcup.managers.dto;

public record CareerStatisticsResponse(
        Long managerId,
        Integer tournamentsManaged,
        Integer matchesManaged,
        Integer wins,
        Integer draws,
        Integer losses,
        Integer goalsScored,
        Integer goalsConceded,
        Integer cleanSheets,
        Integer trophiesWon,
        Integer finalsReached,
        Integer semiFinalsReached
) {
}
