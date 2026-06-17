package com.aditya.worldcup.tournaments.dto;

import com.aditya.worldcup.tournaments.entity.TournamentStatus;

import java.time.LocalDateTime;

public record TournamentResponse(
        Long id,
        String name,
        Integer year,
        String hostCountry,
        TournamentStatus status,
        LocalDateTime createdAt
) {
}