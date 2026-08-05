package com.aditya.worldcup.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AuditHistoryResponse(
        List<AuditEntry> actions,
        List<MaintenanceHistoryResponse> latestMaintenanceJobs
) {

    public record AuditEntry(
            Long id,
            String username,
            String action,
            String entityType,
            Long entityId,
            LocalDateTime timestamp
    ) {
    }
}
