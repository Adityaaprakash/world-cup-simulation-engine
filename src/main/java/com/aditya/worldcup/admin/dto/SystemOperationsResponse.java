package com.aditya.worldcup.admin.dto;

public record SystemOperationsResponse(
        ApplicationSummary application,
        InfrastructureSummary infrastructure,
        SimulationSummary simulation
) {

    public record ApplicationSummary(
            long uptimeMs,
            long usedMemoryBytes,
            long maxMemoryBytes,
            int processors,
            String javaVersion,
            String springBootVersion
    ) {
    }

    public record InfrastructureSummary(String redisStatus, String databaseStatus) {
    }

    public record SimulationSummary(
            long runningTournaments,
            long completedTournaments,
            long activeSaves
    ) {
    }
}
