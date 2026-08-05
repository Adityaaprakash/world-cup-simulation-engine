package com.aditya.worldcup.search.dto;

import com.aditya.worldcup.players.entity.PlayerPosition;
import java.util.List;

public record PlayerSearchRequest(
        String name, String nationality, String team, PlayerPosition position,
        Integer overallRating, Integer minOverallRating, Integer maxOverallRating,
        Integer minPotential, String preferredFoot, Integer minAge, Integer maxAge,
        Boolean active, Boolean retired, Boolean injured, Boolean suspended,
        Integer page, Integer size, List<SearchSort> sort
) {
}
