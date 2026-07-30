package com.aditya.worldcup.saves.service;

import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.repository.ManagerRepository;
import com.aditya.worldcup.managers.service.ManagerService;
import com.aditya.worldcup.players.repository.PlayerStateRepository;
import com.aditya.worldcup.saves.dto.ResumeSaveResponse;
import com.aditya.worldcup.saves.entity.SaveSlot;
import com.aditya.worldcup.saves.repository.SaveSlotRepository;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.tactics.repository.TacticalProfileRepository;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SaveResumeService {

    private final SaveGameService saveGameService;
    private final SaveSlotRepository saveSlotRepository;
    private final ManagerService managerService;
    private final ManagerRepository managerRepository;
    private final TournamentRepository tournamentRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final PlayerStateRepository playerStateRepository;
    private final TacticalProfileRepository tacticalProfileRepository;

    @Transactional
    public ResumeSaveResponse resumeSave(
            Long saveId,
            Authentication authentication) {

        Manager manager = managerService.getOrCreateManager(authentication);
        SaveSlot saveSlot = saveGameService.getOwnedSaveSlot(saveId, manager);
        validateSave(saveSlot);

        restoreManagerProgression(manager, saveSlot);
        deactivateOtherSaves(manager);

        LocalDateTime now = LocalDateTime.now();
        saveSlot.setActive(true);
        saveSlot.setLastPlayedAt(now);
        saveSlot.setUpdatedAt(now);
        saveSlotRepository.save(saveSlot);

        return new ResumeSaveResponse(
                saveSlot.getId(),
                manager.getId(),
                saveSlot.getCurrentTournamentId(),
                saveSlot.getCurrentStage(),
                manager.getLevel(),
                manager.getExperiencePoints(),
                manager.getReputation(),
                restoredSquadSelections(manager),
                playerStateRepository.count(),
                tacticalProfileRepository.count(),
                now,
                "Save resumed successfully"
        );
    }

    private void validateSave(SaveSlot saveSlot) {
        if (saveSlot.getFormatVersion() != null
                && !SaveGameService.CURRENT_FORMAT_VERSION.equals(
                        saveSlot.getFormatVersion())) {
            throw new IllegalArgumentException(
                    "Unsupported save format version: "
                            + saveSlot.getFormatVersion());
        }

        if (saveSlot.getCurrentTournamentId() != null
                && !tournamentRepository.existsById(
                        saveSlot.getCurrentTournamentId())) {
            throw new IllegalArgumentException(
                    "Tournament not found: "
                            + saveSlot.getCurrentTournamentId());
        }
    }

    private void restoreManagerProgression(
            Manager manager,
            SaveSlot saveSlot) {

        manager.setLevel(saveSlot.getManagerLevel());
        manager.setExperiencePoints(saveSlot.getManagerExperiencePoints());
        manager.setReputation(saveSlot.getReputation());
        manager.setUpdatedAt(LocalDateTime.now());
        managerRepository.save(manager);
    }

    private void deactivateOtherSaves(Manager manager) {
        saveSlotRepository.findByManagerIdAndActiveTrue(manager.getId())
                .forEach(activeSave -> {
                    activeSave.setActive(false);
                    activeSave.setUpdatedAt(LocalDateTime.now());
                    saveSlotRepository.save(activeSave);
                });
    }

    private long restoredSquadSelections(Manager manager) {
        return squadPlayerRepository.findAll()
                .stream()
                .filter(squadPlayer -> squadPlayer.getSquad()
                        .getUser()
                        .getEmail()
                        .equals(manager.getUsername()))
                .count();
    }
}
