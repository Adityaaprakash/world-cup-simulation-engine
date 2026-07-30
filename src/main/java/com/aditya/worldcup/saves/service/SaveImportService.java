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
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.teams.repository.TeamRepository;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
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
}
