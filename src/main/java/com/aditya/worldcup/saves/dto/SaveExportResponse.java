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
        List<TacticalProfileSnapshot> tacticalSettings,
        List<PlayerContractSnapshot> playerContracts,
        List<PlayerLifecycleSnapshot> playerLifecycles,
        ManagerEconomySnapshot managerEconomy,
        List<ResourceTransactionSnapshot> resourceTransactions,
        List<ManagerObjectiveSnapshot> managerObjectives
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
            Integer injuryMatchesRemaining,
            Integer developmentRating,
            Integer progressionTracker
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

    public record PlayerContractSnapshot(
            Long contractId,
            Long playerId,
            String playerName,
            com.aditya.worldcup.contracts.entity.ContractStatus status,
            Integer startSeason,
            Integer expirySeason,
            com.aditya.worldcup.contracts.entity.CommitmentLevel commitmentLevel,
            Integer renewalCount
    ) {}

    public record PlayerLifecycleSnapshot(
            Long lifecycleId,
            Long playerId,
            String playerName,
            Boolean active,
            Boolean retired
    ) {}

    public record ManagerEconomySnapshot(
            Long economyId,
            Long managerId,
            Integer balance,
            Integer trainingAllocation,
            Integer medicalAllocation,
            Integer scoutingAllocation,
            LocalDateTime updatedAt
    ) {}

    public record ResourceTransactionSnapshot(
            Long transactionId,
            Long managerId,
            Integer amount,
            String reason,
            LocalDateTime transactionDate,
            String idempotencyKey
    ) {}

    public record ManagerObjectiveSnapshot(
            Long objectiveId,
            Long managerId,
            com.aditya.worldcup.managers.entity.ObjectiveType type,
            String description,
            Integer targetValue,
            Integer currentValue,
            com.aditya.worldcup.managers.entity.ObjectiveStatus status,
            Long tournamentId,
            Integer rewardAmount,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {}
}
