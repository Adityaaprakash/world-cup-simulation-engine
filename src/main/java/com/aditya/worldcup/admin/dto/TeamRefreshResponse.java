package com.aditya.worldcup.admin.dto;

import com.aditya.worldcup.teams.dto.TeamResponse;

public record TeamRefreshResponse(
        TeamResponse team,
        ValidationResponse validation
) {
}
