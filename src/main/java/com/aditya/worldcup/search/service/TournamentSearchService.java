package com.aditya.worldcup.search.service;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchRound;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.search.dto.SearchResultResponse;
import com.aditya.worldcup.search.dto.TournamentSearchRequest;
import com.aditya.worldcup.tournaments.dto.TournamentResponse;
import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.tournaments.entity.TournamentStatus;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import com.aditya.worldcup.tournaments.service.TournamentService;
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
public class TournamentSearchService {
    private final TournamentRepository tournamentRepository;
    private final TournamentService tournamentService;

    @Transactional(readOnly = true)
    public SearchResultResponse<TournamentResponse> search(TournamentSearchRequest request) {
        TournamentSearchRequest criteria = request == null
                ? new TournamentSearchRequest(null, null, null, null, null, null, null, null, null) : request;
        Specification<Tournament> specification = (root, query, builder) -> builder.conjunction();
        if (text(criteria.name())) specification = specification.and((root, query, builder) ->
                builder.like(builder.lower(root.get("name")), like(criteria.name())));
        if (criteria.status() != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("status"), criteria.status()));
        if (criteria.year() != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("year"), criteria.year()));
        if (criteria.archived() != null) specification = specification.and((root, query, builder) ->
                criteria.archived() ? builder.equal(root.get("status"), TournamentStatus.ARCHIVED)
                        : builder.notEqual(root.get("status"), TournamentStatus.ARCHIVED));
        if (criteria.stage() != null) specification = specification.and(stage(criteria.stage()));
        if (text(criteria.champion())) specification = specification.and(champion(criteria.champion()));
        Page<Tournament> page = tournamentRepository.findAll(specification, SearchPageableFactory.create(
                criteria.page(), criteria.size(), criteria.sort(),
                Set.of("id", "name", "year", "status", "createdAt"), "id"));
        Page<TournamentResponse> mapped = page.map(tournamentService::mapToResponse);
        return new SearchResultResponse<>(mapped.getContent(), mapped.getNumber(), mapped.getSize(),
                mapped.getTotalElements(), mapped.getTotalPages(), mapped.isFirst(), mapped.isLast());
    }

    private Specification<Tournament> stage(MatchRound stage) {
        return (root, query, builder) -> {
            Subquery<Long> matches = query.subquery(Long.class);
            Root<Match> match = matches.from(Match.class);
            matches.select(match.get("tournament").get("id")).where(
                    builder.equal(match.get("tournament").get("id"), root.get("id")),
                    builder.equal(match.get("round"), stage));
            return root.get("id").in(matches);
        };
    }

    private Specification<Tournament> champion(String champion) {
        return (root, query, builder) -> {
            Subquery<Long> finals = query.subquery(Long.class);
            Root<Match> match = finals.from(Match.class);
            var homeWinner = builder.and(
                    builder.like(builder.lower(match.get("homeTeam").get("name")), like(champion)),
                    builder.greaterThan(match.<Integer>get("homeScore"), match.<Integer>get("awayScore")));
            var awayWinner = builder.and(
                    builder.like(builder.lower(match.get("awayTeam").get("name")), like(champion)),
                    builder.greaterThan(match.<Integer>get("awayScore"), match.<Integer>get("homeScore")));
            finals.select(match.get("tournament").get("id")).where(
                    builder.equal(match.get("tournament").get("id"), root.get("id")),
                    builder.equal(match.get("round"), MatchRound.FINAL),
                    builder.equal(match.get("status"), MatchStatus.FINISHED),
                    builder.or(homeWinner, awayWinner));
            return root.get("id").in(finals);
        };
    }

    private boolean text(String value) { return value != null && !value.isBlank(); }
    private String like(String value) { return "%" + value.trim().toLowerCase() + "%"; }
}
