package com.aditya.worldcup.players.dto;

public record PlayerResponse(
        Long id,
        String name,
        String position,
        Integer overallRating
) {
}