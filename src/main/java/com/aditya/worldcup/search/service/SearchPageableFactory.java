package com.aditya.worldcup.search.service;

import com.aditya.worldcup.search.dto.SearchSort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

final class SearchPageableFactory {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private SearchPageableFactory() {
    }

    static Pageable create(Integer page, Integer size, List<SearchSort> sorts,
                           Set<String> allowedFields, String defaultField) {
        int requestedPage = page == null ? 0 : Math.max(0, page);
        int requestedSize = size == null ? DEFAULT_SIZE : Math.min(MAX_SIZE, Math.max(1, size));
        Sort sort = Sort.unsorted();
        if (sorts != null) {
            for (SearchSort requested : sorts) {
                if (requested == null || requested.field() == null
                        || !allowedFields.contains(requested.field())) continue;
                Sort.Direction direction = "DESC".equalsIgnoreCase(requested.direction())
                        ? Sort.Direction.DESC : Sort.Direction.ASC;
                sort = sort.and(Sort.by(direction, requested.field()));
            }
        }
        return PageRequest.of(requestedPage, requestedSize,
                sort.isSorted() ? sort : Sort.by(defaultField).ascending());
    }
}
