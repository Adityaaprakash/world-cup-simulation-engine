package com.aditya.worldcup.managers.service;

import com.aditya.worldcup.managers.dto.ManagerLeaderboardsResponse;
import com.aditya.worldcup.managers.entity.CareerStatistics;
import com.aditya.worldcup.managers.entity.ManagerCareerAnalytics;
import com.aditya.worldcup.managers.repository.CareerStatisticsRepository;
import com.aditya.worldcup.managers.repository.ManagerCareerAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerLeaderboardService {

    private static final int LIMIT = 10;

    private final CareerStatisticsRepository statisticsRepository;
    private final ManagerCareerAnalyticsRepository analyticsRepository;

    @Transactional(readOnly = true)
    public ManagerLeaderboardsResponse getLeaderboards() {
        List<CareerStatistics> statistics =
                statisticsRepository.findAllByOrderByWinsDesc();

        return new ManagerLeaderboardsResponse(
                top(statistics, Comparator
                        .comparingDouble(this::winPercentage)
                        .thenComparingInt(stat -> stat.getManager().getLevel())),
                top(statistics, Comparator
                        .comparingInt(CareerStatistics::getTrophiesWon)
                        .thenComparingInt(CareerStatistics::getWins)),
                top(statistics, Comparator
                        .comparingInt(CareerStatistics::getMatchesManaged)
                        .thenComparingInt(CareerStatistics::getWins)),
                top(statistics, Comparator
                        .comparingInt(this::longestUnbeatenStreak)
                        .thenComparingInt(CareerStatistics::getWins)),
                top(statistics, Comparator
                        .comparingInt((CareerStatistics stat) -> stat.getManager()
                                .getReputation()
                                .ordinal())
                        .thenComparingInt(stat -> stat.getManager()
                                .getLevel()))
        );
    }

    private List<ManagerLeaderboardsResponse.ManagerLeaderboardEntry> top(
            List<CareerStatistics> statistics,
            Comparator<CareerStatistics> comparator
    ) {
        return statistics.stream()
                .sorted(comparator.reversed())
                .limit(LIMIT)
                .map(this::mapToEntry)
                .toList();
    }

    private ManagerLeaderboardsResponse.ManagerLeaderboardEntry mapToEntry(
            CareerStatistics statistics) {

        return new ManagerLeaderboardsResponse.ManagerLeaderboardEntry(
                statistics.getManager().getId(),
                statistics.getManager().getDisplayName(),
                statistics.getManager().getReputation(),
                statistics.getManager().getLevel(),
                winPercentage(statistics),
                statistics.getTrophiesWon(),
                statistics.getMatchesManaged(),
                longestUnbeatenStreak(statistics)
        );
    }

    private double winPercentage(CareerStatistics statistics) {
        if (statistics.getMatchesManaged() == 0) {
            return 0.0;
        }
        return Math.round(
                (statistics.getWins() * 10000.0)
                        / statistics.getMatchesManaged()
        ) / 100.0;
    }

    private int longestUnbeatenStreak(CareerStatistics statistics) {
        return analyticsRepository.findByManagerId(
                        statistics.getManager().getId())
                .map(ManagerCareerAnalytics::getLongestUnbeatenStreak)
                .orElse(0);
    }
}
