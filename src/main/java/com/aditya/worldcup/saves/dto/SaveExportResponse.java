package com.aditya.worldcup.saves.dto;

import com.aditya.worldcup.managers.dto.CareerHistoryResponse;
import com.aditya.worldcup.managers.dto.CareerStatisticsResponse;
import com.aditya.worldcup.managers.dto.ManagerResponse;
import com.aditya.worldcup.matches.entity.MatchRound;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.players.entity.InjuryStatus;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.tactics.entity.BuildUpStyle;
import com.aditya.worldcup.tactics.entity.ChanceCreation;
import com.aditya.worldcup.tournaments.entity.TournamentStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SaveExportResponse(
        String schemaVersion,
        LocalDateTime exportedAt,
        SaveSlotResponse saveMetadata,
        ManagerResponse manager,
        CareerStatisticsResponse careerStatistics,
        List<CareerHistoryResponse> careerHistory,
        TournamentSnapshot tournamentState,
        List<SquadSelectionSnapshot> squadSelections,
        List<PlayerStateSnapshot> playerStates,
        List<TacticalProfileSnapshot> tacticalSettings
) {

    public record TournamentSnapshot(
            Long tournamentId,
            String name,
            Integer year,
            String hostCountry,
            TournamentStatus status,
            String currentStage,
            Long completedMatches,
            Long totalMatches,
            Double progressPercentage,
            List<MatchSnapshot> matches
    ) {
    }

    public record MatchSnapshot(
            Long matchId,
            Long homeTeamId,
            String homeTeam,
            Long awayTeamId,
            String awayTeam,
            Integer homeScore,
            Integer awayScore,
            MatchRound round,
            MatchStatus status,
            LocalDateTime matchDate
    ) {
    }

    public record SquadSelectionSnapshot(
            Long squadId,
            String squadName,
            Long teamId,
            String teamName,
            Long formationId,
            String formation,
            Long playerId,
            String playerName,
            PlayerPosition position,
            String positionSlot,
            Boolean startingXi,
            Boolean captain,
            Boolean viceCaptain
    ) {
    }

    public record PlayerStateSnapshot(
            Long playerStateId,
            Long playerId,
            String playerName,
            PlayerPosition position,
            Integer currentForm,
            Integer confidence,
            Integer fitness,
            Integer fatigue,
            Integer morale,
            Integer yellowCards,
            Integer redCardSuspension,
            InjuryStatus injuryStatus,
            Integer injuryMatchesRemaining
    ) {
    }

    public record TacticalProfileSnapshot(
            Long tacticalProfileId,
            Long teamId,
            String teamName,
            Integer attackWidth,
            Integer defensiveWidth,
            Integer defensiveLine,
            Integer pressingIntensity,
            BuildUpStyle buildUpStyle,
            ChanceCreation chanceCreation,
            Integer attackingWidth,
            Integer crossFrequency,
            Integer longBallFrequency,
            Integer passingRisk,
            Boolean counterAttack,
            Boolean highPress,
            Boolean offsideTrap,
            Boolean timeWasting
    ) {
    }
}
