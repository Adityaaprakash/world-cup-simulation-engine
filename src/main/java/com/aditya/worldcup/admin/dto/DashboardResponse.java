package com.aditya.worldcup.admin.dto;

import java.time.LocalDateTime;

public record DashboardResponse(
        SystemSummary system,
        ManagerSummary managers,
        TournamentSummary tournaments,
        CareerSummary career,
        SaveSystemSummary saveSystem,
        InfrastructureSummary infrastructure
) {

    public record SystemSummary(
            String applicationVersion,
            LocalDateTime serverTime
    ) {
    }

    public record ManagerSummary(
            Long totalManagers,
            Long activeManagers
    ) {
    }

    public record TournamentSummary(
            Long totalTournaments,
            Long activeTournaments,
            Long completedTournaments
    ) {
    }

    public record CareerSummary(
            Long totalCareers,
            Long totalAchievementsUnlocked
    ) {
    }

    public record SaveSystemSummary(
            Long totalSaveSlots,
            Long autosaveCount,
            Long activeSaves
    ) {
    }

    public record InfrastructureSummary(
            String redisAvailability,
            String databaseConnectivity
    ) {
    }
}
