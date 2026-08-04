package com.aditya.worldcup.admin.dto;

public record TeamUpdateRequest(
        Integer fifaRanking,
        String confederation,
        String manager
) {
}
