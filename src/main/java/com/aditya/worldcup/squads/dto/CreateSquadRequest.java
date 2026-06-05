package com.aditya.worldcup.squads.dto;

public record CreateSquadRequest(
        Long teamId,
        Long formationId,
        String name
) {
}