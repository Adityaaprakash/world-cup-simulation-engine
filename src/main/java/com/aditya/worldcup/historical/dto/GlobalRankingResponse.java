package com.aditya.worldcup.historical.dto;

import java.util.List;

public record GlobalRankingResponse(List<Entry> players, List<Entry> teams,
        List<Entry> managers) {
    public record Entry(int rank, Long id, String name, long score, String detail) {
    }
}
