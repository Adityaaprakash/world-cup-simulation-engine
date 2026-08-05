package com.aditya.worldcup.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MaintenanceSummaryResponse(
        List<Long> orphanSaveSlotIds,
        int orphanSavesRemoved,
        int inactiveAutosavesRemoved,
        int expiredBackupsCleared,
        int duplicateBackupsCleared,
        int activeSavesSkipped,
        long durationMs,
        LocalDateTime completedAt
) {
}
