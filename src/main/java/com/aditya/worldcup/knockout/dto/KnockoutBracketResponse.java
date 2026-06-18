package com.aditya.worldcup.knockout.dto;

import com.aditya.worldcup.matches.entity.MatchRound;

import java.util.List;

public record KnockoutBracketResponse(
        MatchRound round,
        List<KnockoutMatchResponse> matches
) {
}
