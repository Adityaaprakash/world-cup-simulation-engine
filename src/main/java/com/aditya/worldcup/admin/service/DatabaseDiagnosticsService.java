package com.aditya.worldcup.admin.service;

import com.aditya.worldcup.admin.dto.DatabaseDiagnosticsResponse;
import com.aditya.worldcup.managers.repository.ManagerRepository;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.saves.repository.SaveSlotRepository;
import com.aditya.worldcup.teams.repository.TeamRepository;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import javax.sql.DataSource;

@Service
@RequiredArgsConstructor
public class DatabaseDiagnosticsService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final ObjectProvider<Flyway> flywayProvider;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final SaveSlotRepository saveSlotRepository;
    private final ManagerRepository managerRepository;
    private final AdminAuditService adminAuditService;

    @Transactional
    public DatabaseDiagnosticsResponse diagnostics(Authentication authentication) {
        Flyway flyway = flywayProvider.getIfAvailable();
        MigrationInfo current = flyway == null ? null : flyway.info().current();
        int pending = flyway == null ? 0 : flyway.info().pending().length;
        DatabaseDiagnosticsResponse response = new DatabaseDiagnosticsResponse(
                new DatabaseDiagnosticsResponse.DatabaseSummary(
                        queryLong("SELECT COUNT(*) FROM information_schema.tables "
                                + "WHERE table_schema = 'public'", 0L),
                        queryLong("SELECT COALESCE(SUM(c.reltuples)::BIGINT, 0) "
                                + "FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                                + "WHERE n.nspname = 'public' AND c.relkind = 'r'", 0L),
                        current == null || current.getVersion() == null
                                ? null : current.getVersion().getVersion(),
                        pending,
                        queryNullableLong("SELECT pg_database_size(current_database())"),
                        connectionPool()),
                new DatabaseDiagnosticsResponse.RepositorySummary(
                        playerRepository.count(), teamRepository.count(),
                        tournamentRepository.count(), matchRepository.count(),
                        saveSlotRepository.count(), managerRepository.count()));
        adminAuditService.log(username(authentication), "DATABASE_DIAGNOSTICS", "DATABASE", 0L);
        return response;
    }

    private DatabaseDiagnosticsResponse.ConnectionPoolSummary connectionPool() {
        if (dataSource instanceof HikariDataSource hikari) {
            return new DatabaseDiagnosticsResponse.ConnectionPoolSummary(
                    hikari.getHikariPoolMXBean() == null ? null
                            : hikari.getHikariPoolMXBean().getActiveConnections(),
                    hikari.getHikariPoolMXBean() == null ? null
                            : hikari.getHikariPoolMXBean().getIdleConnections(),
                    hikari.getHikariPoolMXBean() == null ? null
                            : hikari.getHikariPoolMXBean().getTotalConnections(),
                    hikari.getMaximumPoolSize());
        }
        return new DatabaseDiagnosticsResponse.ConnectionPoolSummary(null, null, null, null);
    }

    private long queryLong(String sql, long fallback) {
        Long value = queryNullableLong(sql);
        return value == null ? fallback : value;
    }

    private Long queryNullableLong(String sql) {
        try {
            return jdbcTemplate.queryForObject(sql, Long.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private String username(Authentication authentication) {
        return authentication == null ? "unknown" : authentication.getName();
    }
}
