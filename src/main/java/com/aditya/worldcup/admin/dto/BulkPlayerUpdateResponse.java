package com.aditya.worldcup.admin.dto;

import java.util.List;

public record BulkPlayerUpdateResponse(
        int updatedCount,
        int failedCount,
        List<ValidationMessage> validationMessages
) {
}
