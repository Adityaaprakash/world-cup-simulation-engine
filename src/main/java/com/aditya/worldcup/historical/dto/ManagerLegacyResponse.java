package com.aditya.worldcup.historical.dto;

public record ManagerLegacyResponse(Long managerId, String managerName,
        String reputation, int careerLevel, int trophies, int achievements,
        int promotions, double historicalWinPercentage, int finals,
        long legacyScore) {
}
