package com.aditya.worldcup.search.dto;

import com.aditya.worldcup.managers.entity.ManagerReputation;
import java.util.List;

public record ManagerSearchRequest(
        String name, String nationality, ManagerReputation reputation,
        Integer minLevel, Integer minTrophies, Double minWinPercentage,
        Integer page, Integer size, List<SearchSort> sort
) {
}
