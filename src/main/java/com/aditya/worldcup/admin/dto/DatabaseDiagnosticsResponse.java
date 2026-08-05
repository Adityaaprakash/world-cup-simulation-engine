package com.aditya.worldcup.admin.dto;

public record DatabaseDiagnosticsResponse(
        DatabaseSummary database,
        RepositorySummary repositories
) {

    public record DatabaseSummary(
            long totalTables,
            long approximateTotalRows,
            String flywayMigrationVersion,
            int pendingMigrations,
            Long databaseSizeBytes,
            ConnectionPoolSummary connectionPool
    ) {
    }

    public record ConnectionPoolSummary(
            Integer activeConnections,
            Integer idleConnections,
            Integer totalConnections,
            Integer maximumPoolSize
    ) {
    }

    public record RepositorySummary(
            long playerCount,
            long teamCount,
            long tournamentCount,
            long matchCount,
            long saveCount,
            long managerCount
    ) {
    }
}
