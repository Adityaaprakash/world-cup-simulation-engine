package com.aditya.worldcup.knockout.dto;

public record KnockoutMatchResponse(
        String homeTeam,
        String awayTeam,
        Integer homeScore,
        Integer awayScore,
        Boolean wentToExtraTime,
        Boolean wentToPenalties,
        Integer homePenaltiesScore,
        Integer awayPenaltiesScore,
        String status,
        Long matchId
) {
}
