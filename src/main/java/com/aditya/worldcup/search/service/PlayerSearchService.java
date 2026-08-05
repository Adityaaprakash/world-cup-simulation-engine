package com.aditya.worldcup.search.service;

import com.aditya.worldcup.players.dto.PlayerResponse;
import com.aditya.worldcup.players.entity.InjuryStatus;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.search.dto.PlayerSearchRequest;
import com.aditya.worldcup.search.dto.SearchResultResponse;
import com.aditya.worldcup.teams.entity.Team;
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
public class PlayerSearchService {

    private final PlayerRepository playerRepository;

    @Transactional(readOnly = true)
    public SearchResultResponse<PlayerResponse> search(PlayerSearchRequest request) {
        PlayerSearchRequest criteria = request == null ? new PlayerSearchRequest(null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null) : request;
        Specification<Player> specification = (root, query, builder) -> builder.conjunction();
        if (hasText(criteria.name())) specification = specification.and(like("name", criteria.name()));
        if (hasText(criteria.nationality())) specification = specification.and((root, query, builder) ->
                builder.like(builder.lower(root.get("country").get("name")), likeValue(criteria.nationality())));
        if (hasText(criteria.team())) specification = specification.and(teamName(criteria.team()));
        if (criteria.position() != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("position"), criteria.position()));
        if (criteria.overallRating() != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("overallRating"), criteria.overallRating()));
        if (criteria.minOverallRating() != null) specification = specification.and((root, query, builder) ->
                builder.ge(root.get("overallRating"), criteria.minOverallRating()));
        if (criteria.maxOverallRating() != null) specification = specification.and((root, query, builder) ->
                builder.le(root.get("overallRating"), criteria.maxOverallRating()));
        if (criteria.minPotential() != null) specification = specification.and((root, query, builder) ->
                builder.ge(root.get("potential"), criteria.minPotential()));
        if (hasText(criteria.preferredFoot())) specification = specification.and((root, query, builder) ->
                builder.equal(builder.upper(root.get("preferredFoot")), criteria.preferredFoot().toUpperCase()));
        if (criteria.minAge() != null) specification = specification.and((root, query, builder) ->
                builder.ge(root.get("age"), criteria.minAge()));
        if (criteria.maxAge() != null) specification = specification.and((root, query, builder) ->
                builder.le(root.get("age"), criteria.maxAge()));
        if (criteria.active() != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("active"), criteria.active()));
        if (criteria.retired() != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("retired"), criteria.retired()));
        if (criteria.injured() != null) specification = specification.and(playerState(criteria.injured(), false));
        if (criteria.suspended() != null) specification = specification.and(playerState(criteria.suspended(), true));

        Page<Player> page = playerRepository.findAll(specification, SearchPageableFactory.create(
                criteria.page(), criteria.size(), criteria.sort(),
                Set.of("id", "name", "overallRating", "potential", "age", "position"), "id"));
        return response(page.map(player -> new PlayerResponse(player.getId(), player.getName(),
                player.getPosition() == null ? null : player.getPosition().name(),
                player.getOverallRating())));
    }

    private Specification<Player> like(String field, String value) {
        return (root, query, builder) -> builder.like(builder.lower(root.get(field)), likeValue(value));
    }

    private Specification<Player> teamName(String name) {
        return (root, query, builder) -> {
            Subquery<Long> teams = query.subquery(Long.class);
            Root<Team> team = teams.from(Team.class);
            teams.select(team.get("country").get("id"))
                    .where(builder.like(builder.lower(team.get("name")), likeValue(name)));
            return root.get("country").get("id").in(teams);
        };
    }

    private Specification<Player> playerState(boolean expected, boolean suspended) {
        return (root, query, builder) -> {
            Subquery<Long> states = query.subquery(Long.class);
            Root<PlayerState> state = states.from(PlayerState.class);
            states.select(state.get("player").get("id"));
            states.where(builder.equal(state.get("player").get("id"), root.get("id")),
                    suspended ? builder.gt(state.<Integer>get("redCardSuspension"), 0)
                            : builder.notEqual(state.get("injuryStatus"), InjuryStatus.HEALTHY));
            return expected ? root.get("id").in(states) : builder.not(root.get("id").in(states));
        };
    }

    private <T> SearchResultResponse<T> response(Page<T> page) {
        return new SearchResultResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String likeValue(String value) { return "%" + value.trim().toLowerCase() + "%"; }
}
