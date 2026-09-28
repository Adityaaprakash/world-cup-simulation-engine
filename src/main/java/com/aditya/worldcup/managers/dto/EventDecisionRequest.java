package com.aditya.worldcup.managers.dto;

import jakarta.validation.constraints.NotBlank;

public record EventDecisionRequest(
        @NotBlank(message = "Decision code is required")
        String decisionCode
) {}
