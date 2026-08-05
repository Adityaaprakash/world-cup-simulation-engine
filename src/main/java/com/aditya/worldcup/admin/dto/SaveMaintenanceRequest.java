package com.aditya.worldcup.admin.dto;

public record SaveMaintenanceRequest(
        Boolean cleanupOrphans,
        Boolean cleanupInactiveAutosaves,
        Boolean cleanupExpiredBackups,
        Boolean cleanupDuplicateBackups
) {
}
