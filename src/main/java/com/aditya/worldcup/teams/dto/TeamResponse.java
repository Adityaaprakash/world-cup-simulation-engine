package com.aditya.worldcup.teams.dto;

public record TeamResponse(
        Long id,
        String name,
        Integer overallRating
) {
}