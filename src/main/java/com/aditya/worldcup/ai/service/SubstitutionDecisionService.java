package com.aditya.worldcup.ai.service;

import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.service.PlayerStateService;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squads.entity.Squad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SubstitutionDecisionService {

    private final LineupSelectionService lineupSelectionService;
    private final RotationService rotationService;
    private final PlayerEvaluationService playerEvaluationService;
    private final PlayerStateService playerStateService;

    public List<MatchEventResponse> decideSubstitutions(Squad squad,
                                                         int goalDifference) {
        return decideSubstitutions(squad, goalDifference, List.of(),
                MatchImportance.GROUP_STAGE, false);
    }

    public List<MatchEventResponse> decideSubstitutions(Squad squad,
                                                         int goalDifference,
                                                         List<MatchEventResponse> matchEvents,
                                                         MatchImportance importance,
                                                         boolean extraTime) {
        List<SquadPlayer> players = lineupSelectionService.selectMatchSquad(squad, importance);
        List<SquadPlayer> starters = new ArrayList<>(players.stream()
                .filter(SquadPlayer::getStartingXi).toList());
        List<SquadPlayer> bench = new ArrayList<>(lineupSelectionService.selectBench(squad, importance));
        List<MatchEventResponse> decisions = new ArrayList<>();
        Set<String> bookedPlayers = bookedPlayers(matchEvents);
        int maximumSubstitutions = extraTime ? 4 : 3;

        substituteUnavailablePlayers(starters, bench, decisions, maximumSubstitutions);

        int[] minutes = extraTime ? new int[]{60, 70, 80, 105} : new int[]{60, 70, 80};
        for (int minute : minutes) {
            if (decisions.size() >= maximumSubstitutions) {
                break;
            }
            Optional<SquadPlayer> playerOff = choosePlayerOff(starters, goalDifference,
                    bookedPlayers, importance, extraTime);
            Optional<SquadPlayer> playerOn = choosePlayerOn(bench, playerOff.orElse(null),
                    goalDifference, importance, extraTime);
            if (playerOff.isEmpty() || playerOn.isEmpty()) {
                break;
            }
            starters.remove(playerOff.get());
            bench.remove(playerOn.get());
            decisions.add(substitutionEvent(minute, playerOff.get(), playerOn.get()));
        }
        return decisions;
    }

    private Optional<SquadPlayer> choosePlayerOff(List<SquadPlayer> starters,
                                                   int goalDifference,
                                                   Set<String> bookedPlayers,
                                                   MatchImportance importance,
                                                   boolean extraTime) {
        return starters.stream()
                .filter(player -> player.getPlayer().getPosition() != PlayerPosition.GK)
                .filter(player -> goalDifference > 0
                        ? !isDefenderOrMidfielder(player)
                        : goalDifference < 0
                        ? isDefenderOrMidfielder(player)
                        : true)
                .max(Comparator.comparingDouble(player ->
                        rotationPressure(player, bookedPlayers, importance, extraTime)));
    }

    private Optional<SquadPlayer> choosePlayerOn(List<SquadPlayer> bench,
                                                  SquadPlayer playerOff,
                                                  int goalDifference,
                                                  MatchImportance importance,
                                                  boolean extraTime) {
        return bench.stream()
                .filter(player -> goalDifference > 0
                        ? isDefenderOrMidfielder(player)
                        : goalDifference < 0
                        ? !isDefenderOrMidfielder(player)
                        : true)
                .filter(player -> playerOff == null
                        || tacticalFit(player, playerOff, goalDifference))
                .max(Comparator.comparingDouble(player ->
                        playerEvaluationService.evaluatePlayer(player.getPlayer())
                                + rotationService.availabilityScore(player.getPlayer(), importance)
                                + freshnessBoost(player, extraTime)));
    }

    private double rotationPressure(SquadPlayer player,
                                    Set<String> bookedPlayers,
                                    MatchImportance importance,
                                    boolean extraTime) {
        PlayerState state = playerStateService.getOrCreateState(player.getPlayer());
        double bookingRisk = bookedPlayers.contains(player.getPlayer().getName()) ? 8.0 : 0.0;
        double suspensionRisk = state.getYellowCards() >= 1 ? 2.5 : 0.0;
        double captainProtection = Boolean.TRUE.equals(player.getCaptain()) ? -4.0 : 0.0;
        double extraTimePressure = extraTime ? state.getFatigue() * 0.08 : 0.0;
        return playerEvaluationService.evaluatePlayer(player.getPlayer()) * -0.15
                + rotationService.availabilityScore(player.getPlayer(), importance) * -1
                + bookingRisk
                + suspensionRisk
                + captainProtection
                + extraTimePressure;
    }

    private void substituteUnavailablePlayers(List<SquadPlayer> starters,
                                              List<SquadPlayer> bench,
                                              List<MatchEventResponse> decisions,
                                              int maximumSubstitutions) {
        List<SquadPlayer> unavailableStarters = starters.stream()
                .filter(player -> !rotationService.isAvailable(player.getPlayer()))
                .toList();
        for (SquadPlayer playerOff : unavailableStarters) {
            if (decisions.size() >= maximumSubstitutions) {
                return;
            }
            Optional<SquadPlayer> playerOn = choosePlayerOn(bench, playerOff,
                    0, MatchImportance.GROUP_STAGE, false);
            if (playerOn.isEmpty()) {
                continue;
            }
            starters.remove(playerOff);
            bench.remove(playerOn.get());
            decisions.add(substitutionEvent(1, playerOff, playerOn.get()));
        }
    }

    private MatchEventResponse substitutionEvent(int minute,
                                                 SquadPlayer playerOff,
                                                 SquadPlayer playerOn) {
        return new MatchEventResponse(
                minute,
                playerOn.getPlayer().getName(),
                MatchEventType.SUBSTITUTION.name(),
                playerOn.getPlayer().getName() + " replaces "
                        + playerOff.getPlayer().getName() + "."
        );
    }

    private Set<String> bookedPlayers(List<MatchEventResponse> matchEvents) {
        Set<String> players = new HashSet<>();
        matchEvents.stream()
                .filter(event -> MatchEventType.YELLOW_CARD.name().equals(event.eventType()))
                .map(MatchEventResponse::player)
                .forEach(players::add);
        return players;
    }

    private boolean tacticalFit(SquadPlayer playerOn,
                                SquadPlayer playerOff,
                                int goalDifference) {
        if (sameLine(playerOn.getPlayer().getPosition(), playerOff.getPlayer().getPosition())) {
            return true;
        }
        if (goalDifference > 0) {
            return isDefenderOrMidfielder(playerOn);
        }
        if (goalDifference < 0) {
            return !isDefenderOrMidfielder(playerOn);
        }
        return true;
    }

    private boolean sameLine(PlayerPosition first, PlayerPosition second) {
        return line(first) == line(second);
    }

    private int line(PlayerPosition position) {
        return switch (position) {
            case GK -> 0;
            case RB, CB, LB -> 1;
            case CDM, CM, CAM -> 2;
            case RW, LW, ST -> 3;
        };
    }

    private double freshnessBoost(SquadPlayer player, boolean extraTime) {
        if (!extraTime) {
            return 0;
        }
        PlayerState state = playerStateService.getOrCreateState(player.getPlayer());
        return state.getFitness() * 0.04 - state.getFatigue() * 0.08;
    }

    private boolean isDefenderOrMidfielder(SquadPlayer player) {
        return switch (player.getPlayer().getPosition()) {
            case RB, CB, LB, CDM, CM, CAM -> true;
            default -> false;
        };
    }
}
