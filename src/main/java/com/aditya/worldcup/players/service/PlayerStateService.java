package com.aditya.worldcup.players.service;

import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.players.entity.InjuryStatus;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.repository.PlayerStateRepository;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerStateService {

    private final PlayerStateRepository playerStateRepository;
    private final SquadPlayerRepository squadPlayerRepository;

    @Transactional
    public void updateAfterMatch(Long homeSquadId, Long awaySquadId,
                                 int homeGoals, int awayGoals,
                                 List<MatchEventResponse> events) {
        List<SquadPlayer> homePlayers = squadPlayerRepository.findBySquadId(homeSquadId);
        List<SquadPlayer> awayPlayers = squadPlayerRepository.findBySquadId(awaySquadId);
        List<SquadPlayer> allPlayers = new ArrayList<>(homePlayers);
        allPlayers.addAll(awayPlayers);

        Map<String, Player> playersByName = allPlayers.stream()
                .map(SquadPlayer::getPlayer)
                .collect(Collectors.toMap(Player::getName, Function.identity(), (first, ignored) -> first));
        Map<Long, PlayerState> states = allPlayers.stream()
                .map(SquadPlayer::getPlayer)
                .collect(Collectors.toMap(Player::getId, this::getOrCreateState, (first, ignored) -> first));
        Set<Long> existingSuspensions = states.entrySet().stream()
                .filter(entry -> entry.getValue().getRedCardSuspension() > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Set<Long> playersWhoPlayed = allPlayers.stream()
                .filter(SquadPlayer::getStartingXi)
                .filter(player -> isAvailable(states.get(player.getPlayer().getId())))
                .map(player -> player.getPlayer().getId())
                .collect(Collectors.toSet());
        if (events != null) {
            events.stream()
                    .filter(event -> MatchEventType.SUBSTITUTION.name().equals(event.eventType()))
                    .map(event -> playersByName.get(event.player()))
                    .filter(Objects::nonNull)
                    .map(Player::getId)
                    .forEach(playersWhoPlayed::add);
        }

        applyParticipation(allPlayers, states, playersWhoPlayed);
        applyResult(homePlayers, states, Integer.compare(homeGoals, awayGoals));
        applyResult(awayPlayers, states, Integer.compare(awayGoals, homeGoals));
        applyEventEffects(events, playersByName, states);
        applyCleanSheets(homePlayers, states, awayGoals == 0);
        applyCleanSheets(awayPlayers, states, homeGoals == 0);
        recoverInactivePlayers(allPlayers, states, playersWhoPlayed);
        processSuspensions(states, existingSuspensions);
        processInjuries(states.values());
        decayForm(states.values());
        playerStateRepository.saveAll(states.values());
    }

    @Transactional
    public PlayerState getOrCreateState(Player player) {
        return playerStateRepository.findByPlayerId(player.getId())
                .orElseGet(() -> playerStateRepository.save(PlayerState.builder()
                        .player(player)
                        .build()));
    }

    public boolean isAvailable(PlayerState state) {
        return state.getRedCardSuspension() == 0
                && (state.getInjuryStatus() == InjuryStatus.HEALTHY
                || state.getInjuryStatus() == InjuryStatus.MINOR
                || state.getInjuryMatchesRemaining() == 0);
    }

    public void recoverInactivePlayers(List<SquadPlayer> players,
                                       Map<Long, PlayerState> states,
                                       Set<Long> playersWhoPlayed) {
        players.stream()
                .filter(player -> !playersWhoPlayed.contains(player.getPlayer().getId()))
                .map(player -> states.get(player.getPlayer().getId()))
                .forEach(state -> {
                    state.setFitness(between(state.getFitness() + 4, 0, 100));
                    state.setFatigue(between(state.getFatigue() - 5, 0, 100));
                });
    }

    public void processSuspensions(Map<Long, PlayerState> states,
                                   Set<Long> suspendedBeforeMatch) {
        suspendedBeforeMatch.stream()
                .map(states::get)
                .filter(Objects::nonNull)
                .forEach(state -> state.setRedCardSuspension(
                        Math.max(0, state.getRedCardSuspension() - 1)));
    }

    public void processInjuries(Collection<PlayerState> states) {
        states.stream()
                .filter(state -> state.getInjuryStatus() != InjuryStatus.HEALTHY)
                .forEach(state -> {
                    int remaining = Math.max(0, state.getInjuryMatchesRemaining() - 1);
                    state.setInjuryMatchesRemaining(remaining);
                    if (remaining == 0) {
                        state.setInjuryStatus(InjuryStatus.HEALTHY);
                    }
                });
    }

    public void decayForm(Collection<PlayerState> states) {
        states.forEach(state -> {
            if (state.getCurrentForm() > 0) {
                state.setCurrentForm(state.getCurrentForm() - 1);
            } else if (state.getCurrentForm() < 0) {
                state.setCurrentForm(state.getCurrentForm() + 1);
            }
        });
    }

    private void applyParticipation(List<SquadPlayer> players,
                                    Map<Long, PlayerState> states,
                                    Set<Long> playersWhoPlayed) {
        players.stream()
                .filter(player -> playersWhoPlayed.contains(player.getPlayer().getId()))
                .map(player -> states.get(player.getPlayer().getId()))
                .forEach(state -> {
                    state.setFatigue(between(state.getFatigue() + 8, 0, 100));
                    state.setFitness(between(state.getFitness() - 5, 0, 100));
                });
    }

    private void applyResult(List<SquadPlayer> players, Map<Long, PlayerState> states,
                             int result) {
        players.stream().map(SquadPlayer::getPlayer).map(Player::getId)
                .map(states::get).forEach(state -> {
                    if (result > 0) {
                        state.setMorale(between(state.getMorale() + 4, 0, 100));
                        state.setConfidence(between(state.getConfidence() + 3, 0, 100));
                    } else if (result < 0) {
                        state.setMorale(between(state.getMorale() - 4, 0, 100));
                    }
                });
    }

    private void applyEventEffects(List<MatchEventResponse> events,
                                   Map<String, Player> playersByName,
                                   Map<Long, PlayerState> states) {
        if (events == null) {
            return;
        }
        events.forEach(event -> {
            Player player = playersByName.get(event.player());
            if (player == null) {
                return;
            }
            PlayerState state = states.get(player.getId());
            MatchEventType type = MatchEventType.valueOf(event.eventType());
            if (type == MatchEventType.GOAL) {
                state.setConfidence(between(state.getConfidence() + 5, 0, 100));
                state.setCurrentForm(between(state.getCurrentForm() + 2, -10, 10));
            } else if (type == MatchEventType.ASSIST) {
                state.setCurrentForm(between(state.getCurrentForm() + 2, -10, 10));
            } else if (type == MatchEventType.YELLOW_CARD) {
                state.setYellowCards(state.getYellowCards() + 1);
            } else if (type == MatchEventType.RED_CARD) {
                state.setRedCardSuspension(Math.max(1, state.getRedCardSuspension()));
            }
        });
    }

    private void applyCleanSheets(List<SquadPlayer> players,
                                  Map<Long, PlayerState> states,
                                  boolean cleanSheet) {
        if (!cleanSheet) {
            return;
        }
        players.stream()
                .filter(SquadPlayer::getStartingXi)
                .filter(player -> "GK".equals(player.getPositionSlot()))
                .map(player -> states.get(player.getPlayer().getId()))
                .forEach(state -> state.setConfidence(
                        between(state.getConfidence() + 3, 0, 100)));
    }

    private int between(Integer value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
