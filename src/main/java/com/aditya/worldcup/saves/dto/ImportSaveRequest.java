package com.aditya.worldcup.saves.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ImportSaveRequest(
        @Valid
        @NotNull(message = "exportData is required")
        SaveExportResponse exportData,

        @Positive(message = "slotNumber must be positive")
        Integer slotNumber,

        @Size(max = 100, message = "slotName must be at most 100 characters")
        String slotName,

        @Size(max = 500, message = "description must be at most 500 characters")
        String description,

        Boolean activate
) {
}
