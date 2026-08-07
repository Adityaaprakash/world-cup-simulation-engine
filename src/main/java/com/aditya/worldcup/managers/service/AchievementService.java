package com.aditya.worldcup.managers.service;

import com.aditya.worldcup.managers.dto.AchievementResponse;
import com.aditya.worldcup.managers.entity.*;
import com.aditya.worldcup.managers.repository.ManagerAchievementRepository;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchRound;
import com.aditya.worldcup.saves.service.SaveGameService;
import com.aditya.worldcup.historical.service.HistoricalIntelligenceService;
import com.aditya.worldcup.teams.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final ManagerAchievementRepository achievementRepository;
    private final ManagerService managerService;
    private final CareerTimelineService timelineService;
    private final SaveGameService saveGameService;
    private final HistoricalIntelligenceService historicalIntelligenceService;

    @Transactional
    public List<AchievementResponse> getCurrentAchievements(
            Authentication authentication) {

        Manager manager = managerService.getOrCreateManager(authentication);
        return achievementRepository
                .findByManagerIdOrderByUnlockedAtDesc(manager.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void evaluateAfterMatch(
            Manager manager,
            Match match,
            Team team,
            int wins,
            int cleanSheets,
            int goalsFor,
            int goalsAgainst
    ) {
        if (wins >= 1) {
            unlock(manager, AchievementCode.FIRST_VICTORY);
        }

        if (cleanSheets >= 10) {
            unlock(manager, AchievementCode.CLEAN_SHEET_MACHINE);
        }

        if (goalsAgainst == 0 && match != null
                && match.getRound() != MatchRound.GROUP_STAGE) {
            unlock(manager, AchievementCode.DEFENSIVE_MASTER);
        }

        if (goalsFor >= 4) {
            unlock(manager, AchievementCode.ATTACKING_GENIUS);
        }

        if (match != null
                && team != null
                && wonMatchForTeam(match, team)
                && opponentRating(match, team) - safeRating(team) >= 5) {
            unlock(manager, AchievementCode.GIANT_KILLER);
        }

        if (manager.getLevel() >= 5) {
            unlock(manager, AchievementCode.YOUTH_DEVELOPER);
        }
    }

    @Transactional
    public void evaluateAfterTournament(
            Manager manager,
            boolean tournamentVictory,
            boolean invincible,
            int tournamentsManaged,
            int goalsScored,
            int goalsConceded,
            int knockoutWins
    ) {
        if (tournamentVictory) {
            unlock(manager, AchievementCode.FIRST_TROPHY);
            unlock(manager, AchievementCode.WORLD_CHAMPION);
        }

        if (tournamentVictory && invincible) {
            unlock(manager, AchievementCode.INVINCIBLE_TOURNAMENT);
        }

        if (goalsConceded <= 3 && tournamentsManaged > 0) {
            unlock(manager, AchievementCode.DEFENSIVE_MASTER);
        }

        if (goalsScored >= 15) {
            unlock(manager, AchievementCode.ATTACKING_GENIUS);
        }

        if (knockoutWins >= 3) {
            unlock(manager, AchievementCode.PENALTY_SPECIALIST);
        }

        if (tournamentsManaged >= 5) {
            unlock(manager, AchievementCode.TOURNAMENT_VETERAN);
        }
    }

    private void unlock(Manager manager, AchievementCode code) {
        if (achievementRepository.existsByManagerIdAndAchievementCode(
                manager.getId(),
                code)) {
            return;
        }

        AchievementDefinition definition = definition(code);
        ManagerAchievement achievement = ManagerAchievement.builder()
                .manager(manager)
                .achievementCode(code)
                .title(definition.title())
                .description(definition.description())
                .badge(definition.badge())
                .unlockedAt(LocalDateTime.now())
                .build();

        achievementRepository.save(achievement);
        historicalIntelligenceService.clearCache();
        timelineService.recordEvent(
                manager,
                TimelineEventType.ACHIEVEMENT,
                definition.title(),
                definition.description(),
                null,
                null
        );
        saveGameService.autosave(
                manager,
                null,
                "Achievement unlocked: " + definition.title()
        );
    }

    private boolean wonMatchForTeam(Match match, Team team) {
        if (match.getHomeTeam() != null
                && match.getHomeTeam().getId().equals(team.getId())) {
            return match.getHomeScore() > match.getAwayScore();
        }
        if (match.getAwayTeam() != null
                && match.getAwayTeam().getId().equals(team.getId())) {
            return match.getAwayScore() > match.getHomeScore();
        }
        return false;
    }

    private int opponentRating(Match match, Team team) {
        if (match.getHomeTeam() != null
                && match.getHomeTeam().getId().equals(team.getId())) {
            return safeRating(match.getAwayTeam());
        }
        return safeRating(match.getHomeTeam());
    }

    private int safeRating(Team team) {
        if (team == null || team.getOverallRating() == null) {
            return 0;
        }
        return team.getOverallRating();
    }

    private AchievementDefinition definition(AchievementCode code) {
        return switch (code) {
            case FIRST_VICTORY -> new AchievementDefinition(
                    "First Victory",
                    "Win the first match of your managerial career.",
                    ManagerBadge.BRONZE);
            case FIRST_TROPHY -> new AchievementDefinition(
                    "First Trophy",
                    "Win your first tournament trophy.",
                    ManagerBadge.SILVER);
            case WORLD_CHAMPION -> new AchievementDefinition(
                    "World Champion",
                    "Become champion of a completed World Cup tournament.",
                    ManagerBadge.GOLD);
            case INVINCIBLE_TOURNAMENT -> new AchievementDefinition(
                    "Invincible Tournament",
                    "Complete a trophy-winning tournament without a loss.",
                    ManagerBadge.PLATINUM);
            case DEFENSIVE_MASTER -> new AchievementDefinition(
                    "Defensive Master",
                    "Build a defence that delivers elite clean-sheet or low-concession results.",
                    ManagerBadge.GOLD);
            case ATTACKING_GENIUS -> new AchievementDefinition(
                    "Attacking Genius",
                    "Produce a high-scoring match or tournament campaign.",
                    ManagerBadge.GOLD);
            case PENALTY_SPECIALIST -> new AchievementDefinition(
                    "Penalty Specialist",
                    "Build a strong knockout record in high-pressure matches.",
                    ManagerBadge.PLATINUM);
            case GIANT_KILLER -> new AchievementDefinition(
                    "Giant Killer",
                    "Beat a significantly stronger opponent.",
                    ManagerBadge.GOLD);
            case CLEAN_SHEET_MACHINE -> new AchievementDefinition(
                    "Clean Sheet Machine",
                    "Record ten career clean sheets.",
                    ManagerBadge.SILVER);
            case YOUTH_DEVELOPER -> new AchievementDefinition(
                    "Youth Developer",
                    "Reach level five and establish a development reputation.",
                    ManagerBadge.SILVER);
            case TOURNAMENT_VETERAN -> new AchievementDefinition(
                    "Tournament Veteran",
                    "Manage five completed tournaments.",
                    ManagerBadge.DIAMOND);
        };
    }

    private AchievementResponse mapToResponse(ManagerAchievement achievement) {
        return new AchievementResponse(
                achievement.getAchievementCode(),
                achievement.getTitle(),
                achievement.getDescription(),
                achievement.getBadge(),
                achievement.getUnlockedAt()
        );
    }

    private record AchievementDefinition(
            String title,
            String description,
            ManagerBadge badge
    ) {
    }
}
