package com.aditya.worldcup.admin.service;

import com.aditya.worldcup.admin.dto.DashboardResponse;
import com.aditya.worldcup.managers.repository.CareerStatisticsRepository;
import com.aditya.worldcup.managers.repository.ManagerAchievementRepository;
import com.aditya.worldcup.managers.repository.ManagerRepository;
import com.aditya.worldcup.saves.repository.SaveSlotRepository;
import com.aditya.worldcup.tournaments.entity.TournamentStatus;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final ManagerRepository managerRepository;
    private final CareerStatisticsRepository careerStatisticsRepository;
    private final ManagerAchievementRepository managerAchievementRepository;
    private final TournamentRepository tournamentRepository;
    private final SaveSlotRepository saveSlotRepository;
    private final AdminHealthService adminHealthService;

    @Value("${info.app.version:0.0.1-SNAPSHOT}")
    private String applicationVersion;

    public DashboardResponse dashboard() {
        return new DashboardResponse(
                new DashboardResponse.SystemSummary(
                        applicationVersion,
                        LocalDateTime.now()
                ),
                new DashboardResponse.ManagerSummary(
                        managerRepository.count(),
                        saveSlotRepository.countActiveManagers()
                ),
                new DashboardResponse.TournamentSummary(
                        tournamentRepository.count(),
                        tournamentRepository.countByStatus(
                                TournamentStatus.IN_PROGRESS),
                        tournamentRepository.countByStatus(
                                TournamentStatus.COMPLETED)
                ),
                new DashboardResponse.CareerSummary(
                        careerStatisticsRepository.count(),
                        managerAchievementRepository.count()
                ),
                new DashboardResponse.SaveSystemSummary(
                        saveSlotRepository.count(),
                        saveSlotRepository.countByAutosaveTrue(),
                        saveSlotRepository.countByActiveTrue()
                ),
                new DashboardResponse.InfrastructureSummary(
                        adminHealthService.redisStatus(),
                        adminHealthService.databaseStatus()
                )
        );
    }
}
