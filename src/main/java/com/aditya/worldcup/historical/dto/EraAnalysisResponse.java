package com.aditya.worldcup.historical.dto;

import java.util.List;

public record EraAnalysisResponse(List<Era> eras) {
    public record Era(String label, int fromYear, int toYear, String bestTeam,
            String bestPlayer, String dominantManager, String mostSuccessfulNation,
            String tacticalTrend) {
    }
}
