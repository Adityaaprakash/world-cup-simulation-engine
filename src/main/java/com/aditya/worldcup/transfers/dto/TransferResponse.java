package com.aditya.worldcup.transfers.dto;

public record TransferResponse(
        Long playerId,
        String playerName,
        Long sourceSquadId,
        Long destinationSquadId,
        String message
) {}
