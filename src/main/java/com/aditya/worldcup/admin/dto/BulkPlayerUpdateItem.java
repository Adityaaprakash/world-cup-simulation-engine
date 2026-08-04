package com.aditya.worldcup.admin.dto;

public record BulkPlayerUpdateItem(
        Long playerId,
        PlayerUpdateRequest update
) {
}
