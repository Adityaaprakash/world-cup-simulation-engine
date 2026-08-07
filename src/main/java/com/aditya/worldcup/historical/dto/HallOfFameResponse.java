package com.aditya.worldcup.historical.dto;

import java.util.List;

public record HallOfFameResponse(List<Entry> players, List<Entry> teams,
        List<Entry> managers) {
    public record Entry(Long id, String name, long legacyScore, String detail) {
    }
}
