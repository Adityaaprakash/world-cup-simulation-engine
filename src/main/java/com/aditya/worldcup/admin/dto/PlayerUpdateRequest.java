package com.aditya.worldcup.admin.dto;

public record PlayerUpdateRequest(
        Integer overallRating,
        Integer pace,
        Integer shooting,
        Integer passing,
        Integer dribbling,
        Integer defending,
        Integer physical,
        Integer potential
) {
}
