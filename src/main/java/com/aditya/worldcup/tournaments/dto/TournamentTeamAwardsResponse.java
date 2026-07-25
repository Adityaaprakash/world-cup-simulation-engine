package com.aditya.worldcup.tournaments.dto;

public record TournamentTeamAwardsResponse(
        TeamAward bestAttack,
        TeamAward bestDefence,
        TeamAward fairPlay
) {

    public record TeamAward(
            Long teamId,
            String team,
            Integer value
    ) {
    }
}
