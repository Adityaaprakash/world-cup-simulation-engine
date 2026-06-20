package com.aditya.worldcup.simulation.dto;

public record MatchSimulationRequest(
        Long homeSquadId,
        Long awaySquadId
) {
}