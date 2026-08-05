package com.aditya.worldcup.search.service;

import com.aditya.worldcup.managers.dto.ManagerResponse;
import com.aditya.worldcup.managers.entity.CareerStatistics;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.repository.ManagerRepository;
import com.aditya.worldcup.managers.service.ManagerService;
import com.aditya.worldcup.search.dto.ManagerSearchRequest;
import com.aditya.worldcup.search.dto.SearchResultResponse;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ManagerSearchService {
    private final ManagerRepository managerRepository;
    private final ManagerService managerService;

    @Transactional(readOnly = true)
    public SearchResultResponse<ManagerResponse> search(ManagerSearchRequest request) {
        ManagerSearchRequest criteria = request == null
                ? new ManagerSearchRequest(null, null, null, null, null, null, null, null, null) : request;
        Specification<Manager> specification = (root, query, builder) -> builder.conjunction();
        if (text(criteria.name())) specification = specification.and((root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("username")), like(criteria.name())),
                builder.like(builder.lower(root.get("displayName")), like(criteria.name()))));
        if (text(criteria.nationality())) specification = specification.and((root, query, builder) ->
                builder.like(builder.lower(root.get("nationality")), like(criteria.nationality())));
        if (criteria.reputation() != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("reputation"), criteria.reputation()));
        if (criteria.minLevel() != null) specification = specification.and((root, query, builder) ->
                builder.ge(root.get("level"), criteria.minLevel()));
        if (criteria.minTrophies() != null || criteria.minWinPercentage() != null) {
            specification = specification.and(careerFilters(criteria));
        }
        Page<Manager> page = managerRepository.findAll(specification, SearchPageableFactory.create(
                criteria.page(), criteria.size(), criteria.sort(),
                Set.of("id", "username", "displayName", "nationality", "reputation", "level", "createdAt"), "id"));
        Page<ManagerResponse> mapped = page.map(managerService::mapToResponse);
        return new SearchResultResponse<>(mapped.getContent(), mapped.getNumber(), mapped.getSize(),
                mapped.getTotalElements(), mapped.getTotalPages(), mapped.isFirst(), mapped.isLast());
    }

    private Specification<Manager> careerFilters(ManagerSearchRequest criteria) {
        return (root, query, builder) -> {
            Subquery<Long> statisticsQuery = query.subquery(Long.class);
            Root<CareerStatistics> statistics = statisticsQuery.from(CareerStatistics.class);
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(builder.equal(statistics.get("manager").get("id"), root.get("id")));
            if (criteria.minTrophies() != null) predicates.add(builder.ge(statistics.get("trophiesWon"), criteria.minTrophies()));
            if (criteria.minWinPercentage() != null) predicates.add(builder.ge(
                    builder.prod(statistics.get("wins").as(Double.class), 100.0),
                    builder.prod(statistics.get("matchesManaged").as(Double.class), criteria.minWinPercentage())));
            if (criteria.minWinPercentage() != null) predicates.add(
                    builder.gt(statistics.<Integer>get("matchesManaged"), 0));
            statisticsQuery.select(statistics.get("manager").get("id"))
                    .where(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            return root.get("id").in(statisticsQuery);
        };
    }

    private boolean text(String value) { return value != null && !value.isBlank(); }
    private String like(String value) { return "%" + value.trim().toLowerCase() + "%"; }
}
