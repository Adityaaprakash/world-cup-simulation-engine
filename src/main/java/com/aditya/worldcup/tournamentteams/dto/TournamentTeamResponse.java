package com.aditya.worldcup.tournamentteams.dto;

public record TournamentTeamResponse(

        Long tournamentTeamId,

        Long teamId,

        String teamName,

        Integer overallRating,

        Long groupId,

        Integer seed
) {
}
