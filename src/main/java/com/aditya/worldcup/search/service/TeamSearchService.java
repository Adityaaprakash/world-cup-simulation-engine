package com.aditya.worldcup.search.service;

import com.aditya.worldcup.search.dto.SearchResultResponse;
import com.aditya.worldcup.search.dto.TeamSearchRequest;
import com.aditya.worldcup.teams.dto.TeamResponse;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.teams.repository.TeamRepository;
import com.aditya.worldcup.tournamentteams.entity.TournamentTeam;
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
public class TeamSearchService {
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public SearchResultResponse<TeamResponse> search(TeamSearchRequest request) {
        TeamSearchRequest criteria = request == null
                ? new TeamSearchRequest(null, null, null, null, null, null, null, null, null) : request;
        Specification<Team> specification = (root, query, builder) -> builder.conjunction();
        if (text(criteria.name())) specification = specification.and((root, query, builder) ->
                builder.like(builder.lower(root.get("name")), like(criteria.name())));
        if (text(criteria.confederation())) specification = specification.and((root, query, builder) ->
                builder.like(builder.lower(root.get("confederation")), like(criteria.confederation())));
        if (criteria.minFifaRanking() != null) specification = specification.and((root, query, builder) ->
                builder.ge(root.get("country").get("fifaRanking"), criteria.minFifaRanking()));
        if (criteria.maxFifaRanking() != null) specification = specification.and((root, query, builder) ->
                builder.le(root.get("country").get("fifaRanking"), criteria.maxFifaRanking()));
        if (criteria.active() != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("active"), criteria.active()));
        if (criteria.tournamentId() != null) specification = specification.and((root, query, builder) -> {
            Subquery<Long> participants = query.subquery(Long.class);
            Root<TournamentTeam> participant = participants.from(TournamentTeam.class);
            participants.select(participant.get("team").get("id")).where(
                    builder.equal(participant.get("tournament").get("id"), criteria.tournamentId()));
            return root.get("id").in(participants);
        });
        Page<Team> page = teamRepository.findAll(specification, SearchPageableFactory.create(
                criteria.page(), criteria.size(), criteria.sort(),
                Set.of("id", "name", "confederation", "overallRating", "country.fifaRanking"), "id"));
        Page<TeamResponse> mapped = page.map(team -> new TeamResponse(team.getId(), team.getName(), team.getOverallRating()));
        return new SearchResultResponse<>(mapped.getContent(), mapped.getNumber(), mapped.getSize(),
                mapped.getTotalElements(), mapped.getTotalPages(), mapped.isFirst(), mapped.isLast());
    }

    private boolean text(String value) { return value != null && !value.isBlank(); }
    private String like(String value) { return "%" + value.trim().toLowerCase() + "%"; }
}
