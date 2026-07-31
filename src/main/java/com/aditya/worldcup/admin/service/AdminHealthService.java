package com.aditya.worldcup.admin.service;

import com.aditya.worldcup.admin.dto.AdminHealthResponse;
import com.aditya.worldcup.saves.repository.SaveSlotRepository;
import com.aditya.worldcup.tournaments.entity.TournamentStatus;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminHealthService {

    private static final String UP = "UP";
    private static final String DOWN = "DOWN";
    private static final String NOT_CONFIGURED = "NOT_CONFIGURED";

    private final ObjectProvider<HealthEndpoint> healthEndpointProvider;
    private final DataSource dataSource;
    private final SaveSlotRepository saveSlotRepository;
    private final TournamentRepository tournamentRepository;

    public AdminHealthResponse healthSummary() {
        return new AdminHealthResponse(
                actuatorStatus("redis", NOT_CONFIGURED),
                databaseStatus(),
                saveSlotRepository.countByActiveTrue(),
                tournamentRepository.countByStatus(TournamentStatus.IN_PROGRESS),
                LocalDateTime.now()
        );
    }

    public String redisStatus() {
        return actuatorStatus("redis", NOT_CONFIGURED);
    }

    public String databaseStatus() {
        String actuatorStatus = actuatorStatus("db", null);
        if (actuatorStatus != null) {
            return actuatorStatus;
        }

        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? UP : DOWN;
        } catch (Exception ex) {
            return DOWN;
        }
    }

    private String actuatorStatus(
            String component,
            String fallback) {

        HealthEndpoint healthEndpoint = healthEndpointProvider.getIfAvailable();
        if (healthEndpoint == null) {
            return fallback;
        }

        try {
            HealthComponent health = healthEndpoint.healthForPath(component);
            if (health == null || health.getStatus() == null) {
                return fallback;
            }
            return health.getStatus().getCode();
        } catch (Exception ex) {
            return fallback;
        }
    }
}
