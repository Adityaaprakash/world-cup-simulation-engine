package com.aditya.worldcup.standings.dto;

import java.util.List;

public record GroupStandingsResponse(
        String group,
        List<StandingResponse> standings
) {
}
