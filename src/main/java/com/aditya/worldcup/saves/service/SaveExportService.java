package com.aditya.worldcup.saves.service;

import com.aditya.worldcup.managers.dto.CareerHistoryResponse;
import com.aditya.worldcup.managers.dto.CareerStatisticsResponse;
import com.aditya.worldcup.managers.entity.CareerHistory;
import com.aditya.worldcup.managers.entity.CareerStatistics;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.repository.CareerHistoryRepository;
import com.aditya.worldcup.managers.repository.CareerStatisticsRepository;
import com.aditya.worldcup.managers.service.ManagerService;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.repository.PlayerStateRepository;
import com.aditya.worldcup.saves.dto.SaveExportResponse;
import com.aditya.worldcup.saves.entity.SaveSlot;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.tactics.entity.TacticalProfile;
import com.aditya.worldcup.tactics.repository.TacticalProfileRepository;
import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaveExportService {

    private final SaveGameService saveGameService;
    private final ManagerService managerService;
    private final CareerStatisticsRepository careerStatisticsRepository;
    private final CareerHistoryRepository careerHistoryRepository;
    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final PlayerStateRepository playerStateRepository;
    private final TacticalProfileRepository tacticalProfileRepository;

    @Transactional(readOnly = true)
    public SaveExportResponse exportSave(
            Long saveId,
            Authentication authentication) {

        Manager manager = managerService.getOrCreateManager(authentication);
        SaveSlot saveSlot = saveGameService.getOwnedSaveSlot(saveId, manager);

        return new SaveExportResponse(
                SaveGameService.CURRENT_FORMAT_VERSION,
                LocalDateTime.now(),
                saveGameService.toResponse(saveSlot),
                managerService.mapToResponse(manager),
                careerStatistics(manager),
                careerHistory(manager),
                tournamentSnapshot(saveSlot),
                squadSelections(manager),
                playerStates(),
                tacticalProfiles()
        );
    }

    private CareerStatisticsResponse careerStatistics(Manager manager) {
        return careerStatisticsRepository.findByManagerId(manager.getId())
                .map(this::mapStatistics)
                .orElse(null);
    }

    private CareerStatisticsResponse mapStatistics(
            CareerStatistics statistics) {

        return new CareerStatisticsResponse(
                statistics.getManager().getId(),
                statistics.getTournamentsManaged(),
                statistics.getMatchesManaged(),
                statistics.getWins(),
                statistics.getDraws(),
                statistics.getLosses(),
                statistics.getGoalsScored(),
                statistics.getGoalsConceded(),
                statistics.getCleanSheets(),
                statistics.getTrophiesWon(),
                statistics.getFinalsReached(),
                statistics.getSemiFinalsReached()
        );
    }

    private List<CareerHistoryResponse> careerHistory(Manager manager) {
        return careerHistoryRepository
                .findByManagerIdOrderByDateCompletedDesc(manager.getId())
                .stream()
                .map(this::mapHistory)
                .toList();
    }

    private CareerHistoryResponse mapHistory(CareerHistory history) {
        return new CareerHistoryResponse(
                history.getId(),
                history.getTournament().getId(),
                history.getTournament().getName(),
                history.getTeam().getId(),
                history.getTeam().getName(),
                history.getFinishingPosition(),
                history.getWins(),
                history.getLosses(),
                history.getGoalsScored(),
                history.getGoalsConceded(),
                history.getTrophies(),
                history.getDateCompleted()
        );
    }

    private SaveExportResponse.TournamentSnapshot tournamentSnapshot(
            SaveSlot saveSlot) {

        if (saveSlot.getCurrentTournamentId() == null) {
            return null;
        }

        return tournamentRepository.findById(saveSlot.getCurrentTournamentId())
                .map(tournament -> {
                    List<Match> matches = matchRepository
                            .findByTournamentIdOrderById(tournament.getId());
                    long completed = matches.stream()
                            .filter(match -> match.getStatus()
                                    == MatchStatus.FINISHED)
                            .count();

                    return new SaveExportResponse.TournamentSnapshot(
                            tournament.getId(),
                            tournament.getName(),
                            tournament.getYear(),
                            tournament.getHostCountry(),
                            tournament.getStatus(),
                            saveSlot.getCurrentStage(),
                            completed,
                            (long) matches.size(),
                            progressPercentage(completed, matches.size()),
                            matches.stream()
                                    .map(this::mapMatch)
                                    .toList()
                    );
                })
                .orElse(null);
    }

    private SaveExportResponse.MatchSnapshot mapMatch(Match match) {
        return new SaveExportResponse.MatchSnapshot(
                match.getId(),
                match.getHomeTeam() == null ? null : match.getHomeTeam().getId(),
                match.getHomeTeam() == null ? null : match.getHomeTeam().getName(),
                match.getAwayTeam() == null ? null : match.getAwayTeam().getId(),
                match.getAwayTeam() == null ? null : match.getAwayTeam().getName(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getRound(),
                match.getStatus(),
                match.getMatchDate()
        );
    }

    private List<SaveExportResponse.SquadSelectionSnapshot> squadSelections(
            Manager manager) {

        return squadPlayerRepository.findAll()
                .stream()
                .filter(squadPlayer -> squadPlayer.getSquad()
                        .getUser()
                        .getEmail()
                        .equals(manager.getUsername()))
                .map(this::mapSquadSelection)
                .toList();
    }

    private SaveExportResponse.SquadSelectionSnapshot mapSquadSelection(
            SquadPlayer squadPlayer) {

        return new SaveExportResponse.SquadSelectionSnapshot(
                squadPlayer.getSquad().getId(),
                squadPlayer.getSquad().getName(),
                squadPlayer.getSquad().getTeam().getId(),
                squadPlayer.getSquad().getTeam().getName(),
                squadPlayer.getSquad().getFormation().getId(),
                squadPlayer.getSquad().getFormation().getName(),
                squadPlayer.getPlayer().getId(),
                squadPlayer.getPlayer().getName(),
                squadPlayer.getPlayer().getPosition(),
                squadPlayer.getPositionSlot(),
                squadPlayer.getStartingXi(),
                squadPlayer.getCaptain(),
                squadPlayer.getViceCaptain()
        );
    }

    private List<SaveExportResponse.PlayerStateSnapshot> playerStates() {
        return playerStateRepository.findAll()
                .stream()
                .map(this::mapPlayerState)
                .toList();
    }

    private SaveExportResponse.PlayerStateSnapshot mapPlayerState(
            PlayerState playerState) {

        return new SaveExportResponse.PlayerStateSnapshot(
                playerState.getId(),
                playerState.getPlayer().getId(),
                playerState.getPlayer().getName(),
                playerState.getPlayer().getPosition(),
                playerState.getCurrentForm(),
                playerState.getConfidence(),
                playerState.getFitness(),
                playerState.getFatigue(),
                playerState.getMorale(),
                playerState.getYellowCards(),
                playerState.getRedCardSuspension(),
                playerState.getInjuryStatus(),
                playerState.getInjuryMatchesRemaining()
        );
    }

    private List<SaveExportResponse.TacticalProfileSnapshot> tacticalProfiles() {
        return tacticalProfileRepository.findAll()
                .stream()
                .map(this::mapTacticalProfile)
                .toList();
    }

    private SaveExportResponse.TacticalProfileSnapshot mapTacticalProfile(
            TacticalProfile tacticalProfile) {

        return new SaveExportResponse.TacticalProfileSnapshot(
                tacticalProfile.getId(),
                tacticalProfile.getTeam().getId(),
                tacticalProfile.getTeam().getName(),
                tacticalProfile.getAttackWidth(),
                tacticalProfile.getDefensiveWidth(),
                tacticalProfile.getDefensiveLine(),
                tacticalProfile.getPressingIntensity(),
                tacticalProfile.getBuildUpStyle(),
                tacticalProfile.getChanceCreation(),
                tacticalProfile.getAttackingWidth(),
                tacticalProfile.getCrossFrequency(),
                tacticalProfile.getLongBallFrequency(),
                tacticalProfile.getPassingRisk(),
                tacticalProfile.getCounterAttack(),
                tacticalProfile.getHighPress(),
                tacticalProfile.getOffsideTrap(),
                tacticalProfile.getTimeWasting()
        );
    }

    private double progressPercentage(long completed, int total) {
        if (total == 0) {
            return 0.0;
        }

        return BigDecimal.valueOf((completed * 100.0) / total)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
