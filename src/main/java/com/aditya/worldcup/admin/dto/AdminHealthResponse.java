package com.aditya.worldcup.admin.dto;

import java.time.LocalDateTime;

public record AdminHealthResponse(
        String redisStatus,
        String databaseStatus,
        Long activeSaveCount,
        Long activeTournamentCount,
        LocalDateTime checkedAt
) {
}
