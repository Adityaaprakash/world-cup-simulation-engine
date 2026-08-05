package com.aditya.worldcup.search.dto;

import java.util.List;

public record SearchResultResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
