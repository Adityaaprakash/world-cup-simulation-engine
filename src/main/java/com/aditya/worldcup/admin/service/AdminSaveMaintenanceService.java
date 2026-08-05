package com.aditya.worldcup.admin.service;

import com.aditya.worldcup.admin.dto.MaintenanceSummaryResponse;
import com.aditya.worldcup.admin.dto.SaveMaintenanceRequest;
import com.aditya.worldcup.saves.entity.SaveSlot;
import com.aditya.worldcup.saves.repository.SaveSlotRepository;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminSaveMaintenanceService {

    private static final int BACKUP_RETENTION_DAYS = 30;

    private final SaveSlotRepository saveSlotRepository;
    private final TournamentRepository tournamentRepository;
    private final AdminAuditService adminAuditService;
    private final MaintenanceHistoryService maintenanceHistoryService;

    @Transactional
    public MaintenanceSummaryResponse cleanup(
            SaveMaintenanceRequest request,
            Authentication authentication) {

        long started = System.currentTimeMillis();
        SaveMaintenanceRequest options = request == null
                ? new SaveMaintenanceRequest(true, true, true, true) : request;
        List<SaveSlot> allSaves = saveSlotRepository.findAll();
        List<SaveSlot> orphans = findOrphans(allSaves);
        int skippedActive = 0;
        int removedOrphans = 0;
        int removedAutosaves = 0;
        int expiredBackups = 0;
        int duplicateBackups = 0;

        if (enabled(options.cleanupOrphans())) {
            for (SaveSlot save : orphans) {
                if (Boolean.TRUE.equals(save.getActive())) {
                    skippedActive++;
                } else {
                    saveSlotRepository.delete(save);
                    removedOrphans++;
                }
            }
        }
        if (enabled(options.cleanupInactiveAutosaves())) {
            for (SaveSlot save : saveSlotRepository.findByAutosaveTrueAndActiveFalse()) {
                saveSlotRepository.delete(save);
                removedAutosaves++;
            }
        }
        if (enabled(options.cleanupExpiredBackups())) {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(BACKUP_RETENTION_DAYS);
            for (SaveSlot save : saveSlotRepository
                    .findByBackupAvailableTrueAndBackupCreatedAtBefore(cutoff)) {
                if (Boolean.TRUE.equals(save.getActive())) {
                    skippedActive++;
                } else {
                    clearBackup(save);
                    expiredBackups++;
                }
            }
        }
        if (enabled(options.cleanupDuplicateBackups())) {
            DuplicateBackupCleanupResult result = cleanupDuplicateBackups();
            duplicateBackups = result.cleared();
            skippedActive += result.skipped();
        }

        long duration = System.currentTimeMillis() - started;
        String username = authentication == null ? "unknown" : authentication.getName();
        String details = "orphans=" + removedOrphans + ", inactiveAutosaves="
                + removedAutosaves + ", expiredBackups=" + expiredBackups
                + ", duplicateBackups=" + duplicateBackups;
        adminAuditService.log(username, "SAVE_CLEANUP", "SAVE", 0L);
        maintenanceHistoryService.record("SAVE_CLEANUP", username, duration, "SUCCESS", details);
        return new MaintenanceSummaryResponse(orphans.stream().map(SaveSlot::getId).toList(),
                removedOrphans, removedAutosaves, expiredBackups, duplicateBackups,
                skippedActive, duration, LocalDateTime.now());
    }

    private List<SaveSlot> findOrphans(List<SaveSlot> saves) {
        return saves.stream()
                .filter(save -> save.getCurrentTournamentId() != null
                        && !tournamentRepository.existsById(save.getCurrentTournamentId()))
                .toList();
    }

    private DuplicateBackupCleanupResult cleanupDuplicateBackups() {
        Map<Long, List<SaveSlot>> backupsByManager = new HashMap<>();
        for (SaveSlot save : saveSlotRepository.findAll()) {
            if (Boolean.TRUE.equals(save.getBackupAvailable()) && save.getManager() != null) {
                backupsByManager.computeIfAbsent(save.getManager().getId(), key -> new ArrayList<>()).add(save);
            }
        }
        int cleared = 0;
        int skipped = 0;
        for (List<SaveSlot> backups : backupsByManager.values()) {
            backups.sort(Comparator.comparing(SaveSlot::getBackupCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            for (int index = 1; index < backups.size(); index++) {
                SaveSlot duplicate = backups.get(index);
                if (Boolean.TRUE.equals(duplicate.getActive())) {
                    skipped++;
                } else {
                    clearBackup(duplicate);
                    cleared++;
                }
            }
        }
        return new DuplicateBackupCleanupResult(cleared, skipped);
    }

    private boolean enabled(Boolean value) {
        return value == null || value;
    }

    private void clearBackup(SaveSlot save) {
        save.setBackupAvailable(false);
        save.setBackupCreatedAt(null);
        save.setBackupDescription(null);
        saveSlotRepository.save(save);
    }

    private record DuplicateBackupCleanupResult(int cleared, int skipped) {
    }
}
