package com.aditya.worldcup.fixtures.dto;

import com.aditya.worldcup.matches.dto.MatchResponse;

import java.util.List;

public record FixtureGenerationResponse(
        List<MatchResponse> matches
) {
}
