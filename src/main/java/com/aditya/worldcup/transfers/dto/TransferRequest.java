package com.aditya.worldcup.transfers.dto;

import jakarta.validation.constraints.NotNull;

public record TransferRequest(
        @NotNull(message = "Player ID is required") Long playerId,
        @NotNull(message = "Source squad ID is required") Long sourceSquadId,
        @NotNull(message = "Destination squad ID is required") Long destinationSquadId
) {}
