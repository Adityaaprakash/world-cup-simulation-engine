package com.aditya.worldcup.players.service;

import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.players.entity.InjuryStatus;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.repository.PlayerStateRepository;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.tactics.service.TacticalMatchModifiers;
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
        updateAfterMatch(homeSquadId, awaySquadId, homeGoals, awayGoals, events,
                TacticalMatchModifiers.balanced(), TacticalMatchModifiers.balanced(), false);
    }

    @Transactional
    public void updateAfterMatch(Long homeSquadId, Long awaySquadId,
                                 int homeGoals, int awayGoals,
                                 List<MatchEventResponse> events,
                                 TacticalMatchModifiers homeTactics,
                                 TacticalMatchModifiers awayTactics) {
        updateAfterMatch(homeSquadId, awaySquadId, homeGoals, awayGoals, events, homeTactics, awayTactics, false);
    }

    @Transactional
    public void updateAfterMatch(Long homeSquadId, Long awaySquadId,
                                 int homeGoals, int awayGoals,
                                 List<MatchEventResponse> events,
                                 TacticalMatchModifiers homeTactics,
                                 TacticalMatchModifiers awayTactics,
                                 boolean extraTime) {
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

        Map<Long, Integer> minutesPlayed = calculateMinutesPlayed(allPlayers, events, extraTime, states);

        applyParticipation(homePlayers, states, minutesPlayed, homeTactics);
        applyParticipation(awayPlayers, states, minutesPlayed, awayTactics);
        
        applyResultAndForm(homePlayers, states, minutesPlayed, Integer.compare(homeGoals, awayGoals));
        applyResultAndForm(awayPlayers, states, minutesPlayed, Integer.compare(awayGoals, homeGoals));
        
        processSuspensions(states, existingSuspensions);
        processInjuries(states.values());

        applyEventEffects(events, playersByName, states);
        applyCleanSheets(homePlayers, states, minutesPlayed, awayGoals == 0);
        applyCleanSheets(awayPlayers, states, minutesPlayed, homeGoals == 0);
        
        recoverInactivePlayers(homePlayers, states, minutesPlayed, homeTactics);
        recoverInactivePlayers(awayPlayers, states, minutesPlayed, awayTactics);
        
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
                && state.getInjuryMatchesRemaining() == 0
                && state.getInjuryStatus() == InjuryStatus.HEALTHY;
    }

    private Map<Long, Integer> calculateMinutesPlayed(List<SquadPlayer> allPlayers,
                                                      List<MatchEventResponse> events,
                                                      boolean extraTime,
                                                      Map<Long, PlayerState> states) {
        int totalMinutes = extraTime ? 120 : 90;
        Map<Long, Integer> minutes = new HashMap<>();
        Map<String, Long> namesToIds = allPlayers.stream()
                .collect(Collectors.toMap(sp -> sp.getPlayer().getName(), sp -> sp.getPlayer().getId(), (a, b) -> a));

        allPlayers.forEach(sp -> {
            boolean active = isAvailable(states.get(sp.getPlayer().getId()));
            if (Boolean.TRUE.equals(sp.getStartingXi()) && active) {
                minutes.put(sp.getPlayer().getId(), totalMinutes);
            } else {
                minutes.put(sp.getPlayer().getId(), 0);
            }
        });

        if (events != null) {
            for (MatchEventResponse event : events) {
                if (MatchEventType.SUBSTITUTION.name().equals(event.eventType())) {
                    String desc = event.description();
                    String[] parts = desc.split(" replaces ");
                    if (parts.length == 2) {
                        String playerOnName = parts[0];
                        String playerOffName = parts[1].replace(".", "");
                        Long onId = namesToIds.get(playerOnName);
                        Long offId = namesToIds.get(playerOffName);
                        if (offId != null) {
                            minutes.put(offId, event.minute());
                        }
                        if (onId != null) {
                            minutes.put(onId, totalMinutes - event.minute());
                        }
                    }
                } else if (MatchEventType.RED_CARD.name().equals(event.eventType())) {
                    Long id = namesToIds.get(event.player());
                    if (id != null && minutes.getOrDefault(id, 0) > event.minute()) {
                        minutes.put(id, event.minute());
                    }
                }
            }
        }
        return minutes;
    }

    public void recoverInactivePlayers(List<SquadPlayer> players,
                                       Map<Long, PlayerState> states,
                                       Set<Long> playersWhoPlayed) {
        // legacy testing helper mapping set back to map
        Map<Long, Integer> activeMins = new HashMap<>();
        playersWhoPlayed.forEach(id -> activeMins.put(id, 90));
        recoverInactivePlayers(players, states, activeMins, TacticalMatchModifiers.balanced());
    }

    private void recoverInactivePlayers(List<SquadPlayer> players,
                                        Map<Long, PlayerState> states,
                                        Map<Long, Integer> minutesPlayed,
                                        TacticalMatchModifiers tactics) {
        int recovery = 4 - (int) Math.round(Math.max(0, tactics.fatigueModifier()));
        players.stream()
                .filter(player -> minutesPlayed.getOrDefault(player.getPlayer().getId(), 0) == 0)
                .map(player -> states.get(player.getPlayer().getId()))
                .forEach(state -> {
                    state.setFitness(between(state.getFitness() + Math.max(2, recovery), 0, 100));
                    state.setFatigue(between(state.getFatigue() - Math.max(3, recovery + 1), 0, 100));
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
        // Preserved for legacy test compatibility if required
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
                                    Map<Long, Integer> minutesPlayed,
                                    TacticalMatchModifiers tactics) {
        int baseFatigue = 8 + (int) Math.round(Math.max(0, tactics.fatigueModifier()) * 3);
        
        players.stream()
                .filter(player -> minutesPlayed.getOrDefault(player.getPlayer().getId(), 0) > 0)
                .map(player -> states.get(player.getPlayer().getId()))
                .forEach(state -> {
                    int mins = minutesPlayed.getOrDefault(state.getPlayer().getId(), 0);
                    double ratio = Math.min(1.0, mins / 90.0);
                    int fatigueIncrease = (int) Math.round(baseFatigue * ratio);
                    int fitnessDecrease = (int) Math.round(5 * ratio);
                    
                    state.setFatigue(between(state.getFatigue() + fatigueIncrease, 0, 100));
                    state.setFitness(between(state.getFitness() - fitnessDecrease, 0, 100));
                });
    }

    private void applyResultAndForm(List<SquadPlayer> players, Map<Long, PlayerState> states,
                                   Map<Long, Integer> minutesPlayed, int result) {
        players.stream().map(SquadPlayer::getPlayer).map(Player::getId)
                .map(states::get).forEach(state -> {
                    int mins = minutesPlayed.getOrDefault(state.getPlayer().getId(), 0);
                    if (result > 0) {
                        state.setMorale(between(state.getMorale() + 4, 0, 100));
                        state.setConfidence(between(state.getConfidence() + 3, 0, 100));
                    } else if (result < 0) {
                        state.setMorale(between(state.getMorale() - 4, 0, 100));
                    }
                    
                    if (mins == 0) {
                        if (state.getCurrentForm() > 0) state.setCurrentForm(state.getCurrentForm() - 1);
                        else if (state.getCurrentForm() < 0) state.setCurrentForm(state.getCurrentForm() + 1);
                    } else {
                        int formShift = 0;
                        if (result > 0 && mins >= 30) formShift = 1;
                        else if (result < 0 && mins >= 45) formShift = -1;
                        
                        state.setCurrentForm(between(state.getCurrentForm() + formShift, -10, 10));
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
                state.setCurrentForm(between(state.getCurrentForm() + 1, -10, 10));
            } else if (type == MatchEventType.YELLOW_CARD) {
                state.setYellowCards(state.getYellowCards() + 1);
            } else if (type == MatchEventType.RED_CARD) {
                state.setRedCardSuspension(Math.max(1, state.getRedCardSuspension()));
                state.setCurrentForm(between(state.getCurrentForm() - 2, -10, 10));
            } else if (type == MatchEventType.INJURY) {
                if (event.description().contains("MINOR")) {
                    state.setInjuryStatus(InjuryStatus.MINOR);
                    state.setInjuryMatchesRemaining(1);
                } else if (event.description().contains("MODERATE")) {
                    state.setInjuryStatus(InjuryStatus.MODERATE);
                    state.setInjuryMatchesRemaining(3);
                } else if (event.description().contains("MAJOR")) {
                    state.setInjuryStatus(InjuryStatus.MAJOR);
                    state.setInjuryMatchesRemaining(5);
                }
            }
        });
    }

    private void applyCleanSheets(List<SquadPlayer> players,
                                  Map<Long, PlayerState> states,
                                  Map<Long, Integer> minutesPlayed,
                                  boolean cleanSheet) {
        if (!cleanSheet) {
            return;
        }
        players.stream()
                .filter(player -> minutesPlayed.getOrDefault(player.getPlayer().getId(), 0) >= 60)
                .filter(player -> "GK".equals(player.getPositionSlot()) || "CB".equals(player.getPositionSlot()) || "LB".equals(player.getPositionSlot()) || "RB".equals(player.getPositionSlot()))
                .map(player -> states.get(player.getPlayer().getId()))
                .forEach(state -> {
                    state.setConfidence(between(state.getConfidence() + 3, 0, 100));
                    state.setCurrentForm(between(state.getCurrentForm() + 1, -10, 10));
                });
    }

    private int between(Integer value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
