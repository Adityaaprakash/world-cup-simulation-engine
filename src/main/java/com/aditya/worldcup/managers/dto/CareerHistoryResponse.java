package com.aditya.worldcup.managers.dto;

import java.time.LocalDateTime;

public record CareerHistoryResponse(
        Long id,
        Long tournamentId,
        String tournamentName,
        Long teamId,
        String teamName,
        Integer finishingPosition,
        Integer wins,
        Integer losses,
        Integer goalsScored,
        Integer goalsConceded,
        Integer trophies,
        LocalDateTime dateCompleted
) {
}
