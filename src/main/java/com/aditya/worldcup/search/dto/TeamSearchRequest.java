package com.aditya.worldcup.search.dto;

import java.util.List;

public record TeamSearchRequest(
        String name, String confederation, Integer minFifaRanking,
        Integer maxFifaRanking, Boolean active, Long tournamentId,
        Integer page, Integer size, List<SearchSort> sort
) {
}
