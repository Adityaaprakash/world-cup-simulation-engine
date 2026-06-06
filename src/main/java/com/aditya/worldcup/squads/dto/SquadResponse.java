package com.aditya.worldcup.squads.dto;

public record SquadResponse(
        Long id,
        String name,
        String teamName,
        String formationName
) {
}