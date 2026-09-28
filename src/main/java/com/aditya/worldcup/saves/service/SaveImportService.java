package com.aditya.worldcup.saves.service;

import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.repository.ManagerRepository;
import com.aditya.worldcup.managers.service.ManagerService;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.saves.dto.ImportSaveRequest;
import com.aditya.worldcup.saves.dto.SaveExportResponse;
import com.aditya.worldcup.saves.dto.SaveImportResponse;
import com.aditya.worldcup.saves.entity.SaveSlot;
import com.aditya.worldcup.saves.entity.SaveType;
import com.aditya.worldcup.saves.repository.SaveSlotRepository;
import com.aditya.worldcup.managers.entity.ManagerEconomy;
import com.aditya.worldcup.managers.entity.ManagerObjective;
import com.aditya.worldcup.managers.entity.ResourceTransaction;
import com.aditya.worldcup.managers.repository.ManagerEconomyRepository;
import com.aditya.worldcup.managers.repository.ManagerObjectiveRepository;
import com.aditya.worldcup.managers.repository.ManagerEventRepository;
import com.aditya.worldcup.managers.repository.ResourceTransactionRepository;
import com.aditya.worldcup.managers.entity.ManagerEvent;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.teams.repository.TeamRepository;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import com.aditya.worldcup.tournaments.entity.Tournament;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaveImportService {

    private static final int RESERVED_AUTOSAVE_SLOT = 0;

    private final SaveSlotRepository saveSlotRepository;
    private final ManagerService managerService;
    private final ManagerRepository managerRepository;
    private final TournamentRepository tournamentRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final SaveGameService saveGameService;
    private final ManagerEconomyRepository managerEconomyRepository;
    private final ResourceTransactionRepository resourceTransactionRepository;
    private final ManagerObjectiveRepository managerObjectiveRepository;
    private final ManagerEventRepository managerEventRepository;
    private final com.aditya.worldcup.matches.repository.MatchRepository matchRepository;

    @Transactional
    public SaveImportResponse importSave(
            ImportSaveRequest request,
            Authentication authentication) {

        SaveExportResponse exportData = validateRequest(request);
        Manager manager = managerService.getOrCreateManager(authentication);
        validateManagerOwnership(exportData, manager);
        validateReferences(exportData);

        int slotNumber = request.slotNumber() == null
                ? exportData.saveMetadata().slotNumber()
                : request.slotNumber();
        if (slotNumber <= RESERVED_AUTOSAVE_SLOT) {
            throw new IllegalArgumentException(
                    "Imported saves must use a positive manual slot number");
        }
        if (saveSlotRepository.existsByManagerIdAndSlotNumber(
                manager.getId(),
                slotNumber)) {
            throw new IllegalStateException(
                    "Save slot number already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        SaveSlot saveSlot = SaveSlot.builder()
                .manager(manager)
                .slotName(importedSlotName(request, exportData))
                .slotNumber(slotNumber)
                .description(importedDescription(request, exportData))
                .saveType(SaveType.MANUAL)
                .currentTournamentId(exportData.saveMetadata()
                        .currentTournamentId())
                .currentSeason(exportData.saveMetadata().currentSeason())
                .currentStage(exportData.saveMetadata().currentStage())
                .totalPlayTime(exportData.saveMetadata().totalPlayTime())
                .formatVersion(exportData.schemaVersion())
                .managerLevel(exportData.saveMetadata().managerLevel())
                .managerExperiencePoints(exportData.saveMetadata()
                        .managerExperiencePoints())
                .reputation(exportData.saveMetadata().reputation())
                .tournamentsPlayed(exportData.saveMetadata()
                        .tournamentsPlayed())
                .trophies(exportData.saveMetadata().trophies())
                .currentTeam(exportData.saveMetadata().currentTeam())
                .currentTournament(exportData.saveMetadata()
                        .currentTournament())
                .progressPercentage(exportData.saveMetadata()
                        .progressPercentage())
                .latestSaveTimestamp(now)
                .createdAt(now)
                .updatedAt(now)
                .lastPlayedAt(now)
                .autosave(false)
                .active(false)
                .backupAvailable(false)
                .build();

        SaveSlot saved = saveSlotRepository.save(saveSlot);
        if (Boolean.TRUE.equals(request.activate())) {
            saveSlotRepository.findByManagerIdAndActiveTrue(manager.getId())
                    .forEach(activeSave -> {
                        activeSave.setActive(false);
                        activeSave.setUpdatedAt(now);
                        saveSlotRepository.save(activeSave);
                    });
            saved.setActive(true);
            saved.setLastPlayedAt(now);
            saved = saveSlotRepository.save(saved);
        }

        restoreManagerEconomy(exportData, manager);
        restoreResourceTransactions(exportData, manager);
        restoreManagerObjectives(exportData, manager);
        restoreManagerEvents(exportData, manager);

        return new SaveImportResponse(
                saved.getId(),
                "Save imported successfully",
                saveGameService.toResponse(saved)
        );
    }

    private SaveExportResponse validateRequest(ImportSaveRequest request) {
        if (request == null || request.exportData() == null) {
            throw new IllegalArgumentException("exportData is required");
        }

        SaveExportResponse exportData = request.exportData();
        if (!SaveGameService.CURRENT_FORMAT_VERSION.equals(
                exportData.schemaVersion())) {
            throw new IllegalArgumentException(
                    "Unsupported save format version: "
                            + exportData.schemaVersion());
        }
        if (exportData.saveMetadata() == null) {
            throw new IllegalArgumentException("Save metadata is required");
        }
        if (exportData.manager() == null) {
            throw new IllegalArgumentException("Manager snapshot is required");
        }

        return exportData;
    }

    private void validateManagerOwnership(
            SaveExportResponse exportData,
            Manager authenticatedManager) {

        String exportedUsername = exportData.manager().username();
        managerRepository.findByUsername(exportedUsername)
                .filter(existing -> !existing.getId()
                        .equals(authenticatedManager.getId()))
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "Imported save belongs to another manager");
                });
    }

    private void validateReferences(SaveExportResponse exportData) {
        Long currentTournamentId = exportData.saveMetadata()
                .currentTournamentId();
        if (currentTournamentId != null
                && !tournamentRepository.existsById(currentTournamentId)) {
            throw new IllegalArgumentException(
                    "Tournament not found: " + currentTournamentId);
        }
        if (exportData.tournamentState() != null
                && exportData.tournamentState().tournamentId() != null
                && !tournamentRepository.existsById(
                exportData.tournamentState().tournamentId())) {
            throw new IllegalArgumentException(
                    "Tournament not found: "
                            + exportData.tournamentState().tournamentId());
        }

        for (SaveExportResponse.PlayerStateSnapshot playerState
                : safeList(exportData.playerStates())) {
            if (!playerRepository.existsById(playerState.playerId())) {
                throw new IllegalArgumentException(
                        "Player not found: " + playerState.playerId());
            }
        }

        for (SaveExportResponse.TacticalProfileSnapshot tacticalProfile
                : safeList(exportData.tacticalSettings())) {
            if (!teamRepository.existsById(tacticalProfile.teamId())) {
                throw new IllegalArgumentException(
                        "Team not found: " + tacticalProfile.teamId());
            }
        }

        for (SaveExportResponse.SquadSelectionSnapshot selection
                : safeList(exportData.squadSelections())) {
            if (!squadPlayerRepository.existsBySquadIdAndPlayerId(
                    selection.squadId(),
                    selection.playerId())) {
                throw new IllegalArgumentException(
                        "Squad selection not found for squad "
                                + selection.squadId()
                                + " and player "
                                + selection.playerId());
            }
        }

        for (SaveExportResponse.PlayerContractSnapshot contract
                : safeList(exportData.playerContracts())) {
            if (contract.playerId() != null && !playerRepository.existsById(contract.playerId())) {
                throw new IllegalArgumentException(
                        "Contract player not found: " + contract.playerId());
            }
        }

        for (SaveExportResponse.PlayerLifecycleSnapshot lifecycle
                : safeList(exportData.playerLifecycles())) {
            if (lifecycle.playerId() != null && !playerRepository.existsById(lifecycle.playerId())) {
                throw new IllegalArgumentException(
                        "Lifecycle player not found: " + lifecycle.playerId());
            }
        }
    }

    private String importedSlotName(
            ImportSaveRequest request,
            SaveExportResponse exportData) {

        if (request.slotName() != null && !request.slotName().isBlank()) {
            return request.slotName();
        }

        return exportData.saveMetadata().slotName();
    }

    private String importedDescription(
            ImportSaveRequest request,
            SaveExportResponse exportData) {

        if (request.description() != null) {
            return request.description();
        }

            return exportData.saveMetadata().description();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private void restoreManagerEconomy(SaveExportResponse exportData, Manager manager) {
        if (exportData.managerEconomy() == null) {
            return;
        }
        SaveExportResponse.ManagerEconomySnapshot snap = exportData.managerEconomy();
        ManagerEconomy economy = managerEconomyRepository.findByManagerId(manager.getId())
                .orElseGet(() -> ManagerEconomy.builder()
                        .manager(manager)
                        .balance(snap.balance())
                        .trainingAllocation(snap.trainingAllocation())
                        .medicalAllocation(snap.medicalAllocation())
                        .scoutingAllocation(snap.scoutingAllocation())
                        .updatedAt(snap.updatedAt())
                        .build());

        economy.setBalance(snap.balance());
        economy.setTrainingAllocation(snap.trainingAllocation());
        economy.setMedicalAllocation(snap.medicalAllocation());
        economy.setScoutingAllocation(snap.scoutingAllocation());
        economy.setUpdatedAt(snap.updatedAt());
        managerEconomyRepository.save(economy);
    }

    private void restoreResourceTransactions(SaveExportResponse exportData, Manager manager) {
        for (SaveExportResponse.ResourceTransactionSnapshot snap : safeList(exportData.resourceTransactions())) {
            if (!resourceTransactionRepository.existsByManagerIdAndIdempotencyKey(manager.getId(), snap.idempotencyKey())) {
                ResourceTransaction tx = ResourceTransaction.builder()
                        .manager(manager)
                        .amount(snap.amount())
                        .reason(snap.reason())
                        .transactionDate(snap.transactionDate())
                        .idempotencyKey(snap.idempotencyKey())
                        .build();
                resourceTransactionRepository.save(tx);
            }
        }
    }

    private void restoreManagerObjectives(SaveExportResponse exportData, Manager manager) {
        List<ManagerObjective> existingObjectives = managerObjectiveRepository.findAll()
                .stream()
                .filter(o -> o.getManager().getId().equals(manager.getId()))
                .toList();

        for (SaveExportResponse.ManagerObjectiveSnapshot snap : safeList(exportData.managerObjectives())) {
            boolean exists = existingObjectives.stream().anyMatch(o ->
                o.getType() == snap.type() &&
                o.getDescription().equals(snap.description()) &&
                o.getCreatedAt().equals(snap.createdAt())
            );

            if (!exists) {
                Tournament tournament = null;
                if (snap.tournamentId() != null) {
                    tournament = tournamentRepository.findById(snap.tournamentId()).orElse(null);
                }
                ManagerObjective obj = ManagerObjective.builder()
                        .manager(manager)
                        .type(snap.type())
                        .description(snap.description())
                        .targetValue(snap.targetValue())
                        .currentValue(snap.currentValue())
                        .status(snap.status())
                        .tournament(tournament)
                        .rewardAmount(snap.rewardAmount())
                        .createdAt(snap.createdAt())
                        .completedAt(snap.completedAt())
                        .build();
                managerObjectiveRepository.save(obj);
            } else {
                existingObjectives.stream().filter(o ->
                    o.getType() == snap.type() &&
                    o.getDescription().equals(snap.description()) &&
                    o.getCreatedAt().equals(snap.createdAt())
                ).findFirst().ifPresent(obj -> {
                    obj.setCurrentValue(snap.currentValue());
                    obj.setStatus(snap.status());
                    obj.setCompletedAt(snap.completedAt());
                    managerObjectiveRepository.save(obj);
                });
            }
        }
    }

    private void restoreManagerEvents(SaveExportResponse exportData, Manager manager) {
        List<ManagerEvent> existingEvents = managerEventRepository.findByManagerIdOrderByCreatedAtDesc(manager.getId());

        for (SaveExportResponse.ManagerEventSnapshot snap : safeList(exportData.managerEvents())) {
            boolean exists = existingEvents.stream().anyMatch(e -> e.getContextId().equals(snap.contextId()));

            if (!exists) {
                ManagerEvent event = ManagerEvent.builder()
                        .manager(manager)
                        .type(snap.type())
                        .title(snap.title())
                        .description(snap.description())
                        .contextId(snap.contextId())
                        .status(snap.status())
                        .selectedDecision(snap.selectedDecision())
                        .resolutionText(snap.resolutionText())
                        .relatedPlayer(snap.relatedPlayerId() != null ? playerRepository.findById(snap.relatedPlayerId()).orElse(null) : null)
                        .relatedMatch(snap.relatedMatchId() != null ? matchRepository.findById(snap.relatedMatchId()).orElse(null) : null)
                        .relatedTournament(snap.relatedTournamentId() != null ? tournamentRepository.findById(snap.relatedTournamentId()).orElse(null) : null)
                        .createdAt(snap.createdAt())
                        .expiresAt(snap.expiresAt())
                        .resolvedAt(snap.resolvedAt())
                        .build();
                managerEventRepository.save(event);
            } else {
                existingEvents.stream()
                        .filter(e -> e.getContextId().equals(snap.contextId()))
                        .findFirst()
                        .ifPresent(event -> {
                            event.setStatus(snap.status());
                            event.setSelectedDecision(snap.selectedDecision());
                            event.setResolutionText(snap.resolutionText());
                            event.setResolvedAt(snap.resolvedAt());
                            managerEventRepository.save(event);
                        });
            }
        }
    }
}
