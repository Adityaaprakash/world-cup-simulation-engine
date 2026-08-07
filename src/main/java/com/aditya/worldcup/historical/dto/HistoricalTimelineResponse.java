package com.aditya.worldcup.historical.dto;

import java.util.List;

public record HistoricalTimelineResponse(List<Entry> entries) {
    public record Entry(Long tournamentId, int year, String tournament,
            String champion, String runnerUp, String goldenBoot, String goldenBall,
            String biggestUpset, String milestone) {
    }
}
