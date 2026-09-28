package com.aditya.worldcup.managers.dto;

public record DecisionOptionResponse(
        String code,
        String label,
        String consequencePreview
) {}
