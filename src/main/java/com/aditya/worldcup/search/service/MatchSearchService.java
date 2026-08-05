package com.aditya.worldcup.search.service;

import com.aditya.worldcup.matchevents.entity.MatchEvent;
import com.aditya.worldcup.matches.dto.MatchResponse;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.matches.service.MatchService;
import com.aditya.worldcup.search.dto.MatchSearchRequest;
import com.aditya.worldcup.search.dto.SearchResultResponse;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class MatchSearchService {
    private final MatchRepository matchRepository;
    private final MatchService matchService;

    @Transactional(readOnly = true)
    public SearchResultResponse<MatchResponse> search(MatchSearchRequest request) {
        MatchSearchRequest criteria = request == null
                ? new MatchSearchRequest(null, null, null, null, null, null, null, null, null, null, null, null) : request;
        Specification<Match> specification = (root, query, builder) -> builder.conjunction();
        if (text(criteria.homeTeam())) specification = specification.and((root, query, builder) ->
                builder.like(builder.lower(root.get("homeTeam").get("name")), like(criteria.homeTeam())));
        if (text(criteria.awayTeam())) specification = specification.and((root, query, builder) ->
                builder.like(builder.lower(root.get("awayTeam").get("name")), like(criteria.awayTeam())));
        if (text(criteria.tournament())) specification = specification.and((root, query, builder) ->
                builder.like(builder.lower(root.get("tournament").get("name")), like(criteria.tournament())));
        if (criteria.stage() != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("round"), criteria.stage()));
        if (criteria.homeScore() != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("homeScore"), criteria.homeScore()));
        if (criteria.awayScore() != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("awayScore"), criteria.awayScore()));
        if (criteria.fromDate() != null) specification = specification.and((root, query, builder) ->
                builder.greaterThanOrEqualTo(root.get("matchDate"), criteria.fromDate()));
        if (criteria.toDate() != null) specification = specification.and((root, query, builder) ->
                builder.lessThanOrEqualTo(root.get("matchDate"), criteria.toDate()));
        if (text(criteria.player())) specification = specification.and(player(criteria.player()));

        Page<Match> page = matchRepository.findAll(specification, SearchPageableFactory.create(
                criteria.page(), criteria.size(), criteria.sort(),
                Set.of("id", "matchDate", "round", "status", "homeScore", "awayScore"), "id"));
        Page<MatchResponse> mapped = page.map(matchService::mapToResponse);
        return new SearchResultResponse<>(mapped.getContent(), mapped.getNumber(), mapped.getSize(),
                mapped.getTotalElements(), mapped.getTotalPages(), mapped.isFirst(), mapped.isLast());
    }

    private Specification<Match> player(String player) {
        return (root, query, builder) -> {
            query.distinct(true);
            Subquery<Long> events = query.subquery(Long.class);
            Root<MatchEvent> event = events.from(MatchEvent.class);
            events.select(event.get("match").get("id")).where(
                    builder.equal(event.get("match").get("id"), root.get("id")),
                    builder.like(builder.lower(event.get("player").get("name")), like(player)));
            return builder.or(root.get("id").in(events), builder.like(builder.lower(
                    root.join("manOfTheMatch", JoinType.LEFT).get("name")), like(player)));
        };
    }

    private boolean text(String value) { return value != null && !value.isBlank(); }
    private String like(String value) { return "%" + value.trim().toLowerCase() + "%"; }
}
