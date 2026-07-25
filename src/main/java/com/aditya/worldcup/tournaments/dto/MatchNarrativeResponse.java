package com.aditya.worldcup.tournaments.dto;

public record MatchNarrativeResponse(
        Long matchId,
        String headline,
        String narrative
) {
}
