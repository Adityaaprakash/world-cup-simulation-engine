package com.aditya.worldcup.saves.dto;

public record SaveImportResponse(
        Long saveId,
        String message,
        SaveSlotResponse save
) {
}
