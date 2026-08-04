package com.aditya.worldcup.admin.service;

import com.aditya.worldcup.admin.dto.TeamRefreshResponse;
import com.aditya.worldcup.admin.dto.TeamUpdateRequest;
import com.aditya.worldcup.admin.dto.ValidationMessage;
import com.aditya.worldcup.admin.dto.ValidationResponse;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.teams.dto.TeamResponse;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.teams.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTeamService {

    private static final String ENTITY_TYPE_TEAM = "TEAM";

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final DatasetValidationService datasetValidationService;
    private final AdminAuditService adminAuditService;

    @Transactional
    public TeamResponse updateTeam(
            Long teamId,
            TeamUpdateRequest request,
            Authentication authentication) {

        if (request == null || (request.fifaRanking() == null
                && request.confederation() == null && request.manager() == null)) {
            throw new IllegalArgumentException("At least one team field must be supplied");
        }
        if (request.fifaRanking() != null && request.fifaRanking() < 1) {
            throw new IllegalArgumentException("FIFA ranking must be positive");
        }
        Team team = findTeam(teamId);
        if (request.fifaRanking() != null) {
            team.getCountry().setFifaRanking(request.fifaRanking());
        }
        if (request.confederation() != null) {
            team.setConfederation(request.confederation().trim());
        }
        if (request.manager() != null) {
            team.setManager(request.manager().trim());
        }
        Team saved = teamRepository.save(team);
        audit(authentication, "TEAM_UPDATED", saved.getId());
        return map(saved);
    }

    @Transactional
    public TeamResponse activateTeam(Long teamId, Authentication authentication) {
        Team team = findTeam(teamId);
        team.setActive(true);
        Team saved = teamRepository.save(team);
        audit(authentication, "TEAM_ACTIVATED", saved.getId());
        return map(saved);
    }

    @Transactional
    public TeamResponse deactivateTeam(Long teamId, Authentication authentication) {
        Team team = findTeam(teamId);
        team.setActive(false);
        Team saved = teamRepository.save(team);
        audit(authentication, "TEAM_DEACTIVATED", saved.getId());
        return map(saved);
    }

    @Transactional
    public TeamRefreshResponse refreshSquad(Long teamId, Authentication authentication) {
        Team team = findTeam(teamId);
        List<Player> eligiblePlayers = playerRepository
                .findByCountryIdAndActiveTrueAndRetiredFalse(team.getCountry().getId());
        if (!eligiblePlayers.isEmpty()) {
            int averageRating = (int) Math.round(eligiblePlayers.stream()
                    .mapToInt(Player::getOverallRating)
                    .average()
                    .orElse(team.getOverallRating()));
            team.setOverallRating(averageRating);
        }
        Team saved = teamRepository.save(team);
        ValidationResponse validation = datasetValidationService.validateTeamSquads(teamId);
        if (eligiblePlayers.isEmpty()) {
            List<ValidationMessage> messages = new ArrayList<>(validation.messages());
            messages.add(new ValidationMessage("INVALID_SQUAD", "TEAM", teamId,
                    "National team has no active, non-retired players"));
            validation = new ValidationResponse(false, List.copyOf(messages));
        }
        audit(authentication, "TEAM_REFRESHED", saved.getId());
        return new TeamRefreshResponse(map(saved), validation);
    }

    private Team findTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));
    }

    private TeamResponse map(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getOverallRating());
    }

    private void audit(Authentication authentication, String action, Long teamId) {
        adminAuditService.log(authentication == null ? "unknown" : authentication.getName(),
                action, ENTITY_TYPE_TEAM, teamId);
    }
}
