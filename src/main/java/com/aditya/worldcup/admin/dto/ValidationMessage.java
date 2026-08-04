package com.aditya.worldcup.admin.dto;

public record ValidationMessage(
        String code,
        String entityType,
        Long entityId,
        String message
) {
}
