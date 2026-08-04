package com.aditya.worldcup.admin.dto;

import java.time.LocalDateTime;

public record DatasetHealthResponse(
        PlayerSummary players,
        TeamSummary teams,
        ValidationSummary validation,
        LocalDateTime checkedAt
) {

    public record PlayerSummary(long active, long inactive, long retired) {
    }

    public record TeamSummary(long active, long inactive) {
    }

    public record ValidationSummary(
            long duplicatePlayers,
            long invalidRatings,
            long invalidSquads
    ) {
    }
}
