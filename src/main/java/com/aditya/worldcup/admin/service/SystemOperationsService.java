package com.aditya.worldcup.admin.service;

import com.aditya.worldcup.admin.dto.SystemOperationsResponse;
import com.aditya.worldcup.saves.repository.SaveSlotRepository;
import com.aditya.worldcup.tournaments.entity.TournamentStatus;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringBootVersion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import java.lang.management.ManagementFactory;

@Service
@RequiredArgsConstructor
public class SystemOperationsService {

    private final AdminHealthService adminHealthService;
    private final TournamentRepository tournamentRepository;
    private final SaveSlotRepository saveSlotRepository;
    private final AdminAuditService adminAuditService;

    @Transactional
    public SystemOperationsResponse operations(Authentication authentication) {
        Runtime runtime = Runtime.getRuntime();
        SystemOperationsResponse response = new SystemOperationsResponse(
                new SystemOperationsResponse.ApplicationSummary(
                        ManagementFactory.getRuntimeMXBean().getUptime(),
                        runtime.totalMemory() - runtime.freeMemory(),
                        runtime.maxMemory(),
                        runtime.availableProcessors(),
                        System.getProperty("java.version"),
                        SpringBootVersion.getVersion()),
                new SystemOperationsResponse.InfrastructureSummary(
                        adminHealthService.redisStatus(), adminHealthService.databaseStatus()),
                new SystemOperationsResponse.SimulationSummary(
                        tournamentRepository.countByStatus(TournamentStatus.IN_PROGRESS),
                        tournamentRepository.countByStatus(TournamentStatus.COMPLETED),
                        saveSlotRepository.countByActiveTrue()));
        adminAuditService.log(authentication == null ? "unknown" : authentication.getName(),
                "SYSTEM_DIAGNOSTICS", "SYSTEM", 0L);
        return response;
    }
}
