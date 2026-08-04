package com.aditya.worldcup.admin.service;

import com.aditya.worldcup.admin.dto.ValidationMessage;
import com.aditya.worldcup.admin.dto.ValidationResponse;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DatasetValidationService {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 99;
    private static final int MIN_AGE = 15;
    private static final int MAX_AGE = 50;
    private static final int MAX_SQUAD_SIZE = 26;

    private final PlayerRepository playerRepository;
    private final SquadRepository squadRepository;
    private final SquadPlayerRepository squadPlayerRepository;

    @Transactional(readOnly = true)
    public ValidationResponse validateDataset() {
        List<ValidationMessage> messages = new ArrayList<>();
        messages.addAll(playerValidationMessages());
        messages.addAll(squadValidationMessages(squadRepository.findAll()));
        return response(messages);
    }

    @Transactional(readOnly = true)
    public ValidationResponse validateTeamSquads(Long teamId) {
        return response(squadValidationMessages(squadRepository.findByTeamId(teamId)));
    }

    @Transactional(readOnly = true)
    public List<ValidationMessage> playerValidationMessages() {
        List<Player> players = playerRepository.findAll();
        List<ValidationMessage> messages = new ArrayList<>();
        Map<String, List<Player>> playersByNationalTeamAndName = new HashMap<>();

        for (Player player : players) {
            validatePlayer(player, messages);
            if (player.getCountry() != null && player.getName() != null) {
                String key = player.getCountry().getId() + ":"
                        + player.getName().trim().toLowerCase(Locale.ROOT);
                playersByNationalTeamAndName
                        .computeIfAbsent(key, ignored -> new ArrayList<>())
                        .add(player);
            }
        }

        playersByNationalTeamAndName.values().stream()
                .filter(duplicates -> duplicates.size() > 1)
                .flatMap(List::stream)
                .forEach(player -> messages.add(message(
                        "DUPLICATE_PLAYER",
                        "PLAYER",
                        player.getId(),
                        "Duplicate player name within the same national team"
                )));
        return messages;
    }

    @Transactional(readOnly = true)
    public List<ValidationMessage> squadValidationMessages() {
        return squadValidationMessages(squadRepository.findAll());
    }

    private List<ValidationMessage> squadValidationMessages(List<Squad> squads) {
        List<ValidationMessage> messages = new ArrayList<>();
        for (Squad squad : squads) {
            List<SquadPlayer> squadPlayers = squadPlayerRepository
                    .findBySquadId(squad.getId());
            validateSquad(squad, squadPlayers, messages);
        }
        return messages;
    }

    private void validatePlayer(Player player, List<ValidationMessage> messages) {
        if (player.getCountry() == null) {
            messages.add(message("MISSING_NATIONALITY", "PLAYER", player.getId(),
                    "Player nationality is required"));
        }
        if (player.getPosition() == null || !isKnownPosition(player.getPosition())) {
            messages.add(message("INVALID_POSITION", "PLAYER", player.getId(),
                    "Player position is invalid"));
        }
        if (player.getAge() == null || player.getAge() < MIN_AGE
                || player.getAge() > MAX_AGE) {
            messages.add(message("INVALID_AGE", "PLAYER", player.getId(),
                    "Player age must be between " + MIN_AGE + " and " + MAX_AGE));
        }
        if (player.getPreferredFoot() == null
                || !("LEFT".equalsIgnoreCase(player.getPreferredFoot())
                || "RIGHT".equalsIgnoreCase(player.getPreferredFoot()))) {
            messages.add(message("INVALID_PREFERRED_FOOT", "PLAYER", player.getId(),
                    "Preferred foot must be LEFT or RIGHT"));
        }
        validateRating(player.getId(), "overall rating", player.getOverallRating(), messages);
        validateRating(player.getId(), "pace", player.getPace(), messages);
        validateRating(player.getId(), "shooting", player.getShooting(), messages);
        validateRating(player.getId(), "passing", player.getPassing(), messages);
        validateRating(player.getId(), "dribbling", player.getDribbling(), messages);
        validateRating(player.getId(), "defending", player.getDefending(), messages);
        validateRating(player.getId(), "physical", player.getPhysical(), messages);
        validateRating(player.getId(), "potential", player.getPotential(), messages);
    }

    private void validateSquad(
            Squad squad,
            List<SquadPlayer> squadPlayers,
            List<ValidationMessage> messages) {

        if (squadPlayers.isEmpty()) {
            messages.add(message("INVALID_SQUAD", "SQUAD", squad.getId(),
                    "Squad is empty"));
            return;
        }
        if (squadPlayers.size() > MAX_SQUAD_SIZE) {
            messages.add(message("INVALID_SQUAD", "SQUAD", squad.getId(),
                    "Squad exceeds the maximum size of " + MAX_SQUAD_SIZE));
        }

        Set<Long> playerIds = new HashSet<>();
        boolean hasGoalkeeper = false;
        for (SquadPlayer squadPlayer : squadPlayers) {
            Player player = squadPlayer.getPlayer();
            if (player == null || player.getId() == null) {
                messages.add(message("INVALID_SQUAD", "SQUAD", squad.getId(),
                        "Squad contains an invalid player reference"));
                continue;
            }
            if (!playerIds.add(player.getId())) {
                messages.add(message("INVALID_SQUAD", "SQUAD", squad.getId(),
                        "Squad contains duplicate player " + player.getId()));
            }
            if (player.getPosition() == PlayerPosition.GK) {
                hasGoalkeeper = true;
            }
            if (squad.getTeam() == null || squad.getTeam().getCountry() == null
                    || player.getCountry() == null
                    || !squad.getTeam().getCountry().getId()
                    .equals(player.getCountry().getId())) {
                messages.add(message("INVALID_SQUAD", "SQUAD", squad.getId(),
                        "Player does not belong to the squad's national team"));
            }
        }
        if (!hasGoalkeeper) {
            messages.add(message("INVALID_SQUAD", "SQUAD", squad.getId(),
                    "Squad must include a goalkeeper"));
        }
    }

    private boolean isKnownPosition(PlayerPosition position) {
        for (PlayerPosition knownPosition : PlayerPosition.values()) {
            if (knownPosition == position) {
                return true;
            }
        }
        return false;
    }

    private void validateRating(
            Long playerId,
            String ratingName,
            Integer rating,
            List<ValidationMessage> messages) {

        if (rating == null || rating < MIN_RATING || rating > MAX_RATING) {
            messages.add(message("INVALID_RATING", "PLAYER", playerId,
                    ratingName + " must be between " + MIN_RATING
                            + " and " + MAX_RATING));
        }
    }

    private ValidationMessage message(
            String code,
            String entityType,
            Long entityId,
            String text) {

        return new ValidationMessage(code, entityType, entityId, text);
    }

    private ValidationResponse response(List<ValidationMessage> messages) {
        return new ValidationResponse(messages.isEmpty(), List.copyOf(messages));
    }
}
