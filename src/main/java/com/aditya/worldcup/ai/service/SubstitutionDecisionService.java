package com.aditya.worldcup.ai.service;

import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.squads.entity.Squad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SubstitutionDecisionService {

    private final SquadPlayerRepository squadPlayerRepository;
    private final RotationService rotationService;

    public List<MatchEventResponse> decideSubstitutions(Squad squad,
                                                         int goalDifference) {
        List<SquadPlayer> players = squadPlayerRepository.findBySquadId(squad.getId());
        List<SquadPlayer> starters = new ArrayList<>(players.stream()
                .filter(SquadPlayer::getStartingXi).toList());
        List<SquadPlayer> bench = new ArrayList<>(players.stream()
                .filter(player -> !player.getStartingXi())
                .filter(player -> rotationService.isAvailable(player.getPlayer()))
                .toList());
        List<MatchEventResponse> decisions = new ArrayList<>();
        int[] minutes = {60, 70, 80};
        for (int minute : minutes) {
            Optional<SquadPlayer> playerOff = choosePlayerOff(starters, goalDifference);
            Optional<SquadPlayer> playerOn = choosePlayerOn(bench, goalDifference);
            if (playerOff.isEmpty() || playerOn.isEmpty()) {
                break;
            }
            starters.remove(playerOff.get());
            bench.remove(playerOn.get());
            decisions.add(new MatchEventResponse(
                    minute,
                    playerOn.get().getPlayer().getName(),
                    MatchEventType.SUBSTITUTION.name(),
                    playerOn.get().getPlayer().getName() + " replaces "
                            + playerOff.get().getPlayer().getName() + "."
            ));
        }
        return decisions;
    }

    private Optional<SquadPlayer> choosePlayerOff(List<SquadPlayer> starters,
                                                   int goalDifference) {
        return starters.stream()
                .filter(player -> player.getPlayer().getPosition() != PlayerPosition.GK)
                .filter(player -> goalDifference > 0
                        ? !isDefenderOrMidfielder(player)
                        : goalDifference < 0
                        ? isDefenderOrMidfielder(player)
                        : true)
                .max(Comparator.comparingDouble(player ->
                        rotationService.availabilityScore(player.getPlayer()) * -1));
    }

    private Optional<SquadPlayer> choosePlayerOn(List<SquadPlayer> bench,
                                                  int goalDifference) {
        return bench.stream()
                .filter(player -> goalDifference > 0
                        ? isDefenderOrMidfielder(player)
                        : goalDifference < 0
                        ? !isDefenderOrMidfielder(player)
                        : true)
                .max(Comparator.comparingDouble(player ->
                        rotationService.availabilityScore(player.getPlayer())));
    }

    private boolean isDefenderOrMidfielder(SquadPlayer player) {
        return switch (player.getPlayer().getPosition()) {
            case RB, CB, LB, CDM, CM, CAM -> true;
            default -> false;
        };
    }
}
