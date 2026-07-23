package com.aditya.worldcup.ai.service;

import com.aditya.worldcup.formations.entity.Formation;
import com.aditya.worldcup.formations.repository.FormationRepository;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FormationSelectionService {

    private final FormationRepository formationRepository;
    private final PlayerEvaluationService playerEvaluationService;

    public Formation selectFormation(int squadQuality, int opponentQuality,
                                     Formation currentFormation) {
        return selectFormation(List.of(), squadQuality, opponentQuality, currentFormation);
    }

    public Formation selectFormation(List<SquadPlayer> availablePlayers,
                                     int squadQuality,
                                     int opponentQuality,
                                     Formation currentFormation) {
        List<Formation> formations = formationRepository.findAll();
        if (formations.isEmpty()) {
            return currentFormation;
        }

        FormationTarget target = chooseTarget(availablePlayers, squadQuality, opponentQuality);
        return formations.stream()
                .min(Comparator.comparingInt(target::distanceTo)
                        .thenComparingInt(target::namePriority))
                .orElse(currentFormation);
    }

    private FormationTarget chooseTarget(List<SquadPlayer> availablePlayers,
                                         int squadQuality,
                                         int opponentQuality) {
        int difference = squadQuality - opponentQuality;
        if (difference <= -7) {
            return new FormationTarget(5, 4, 1, List.of("5-4-1"));
        }

        double wingerStrength = positionStrength(availablePlayers, PlayerPosition.RW, PlayerPosition.LW);
        double centralStrength = positionStrength(availablePlayers,
                PlayerPosition.CDM, PlayerPosition.CM, PlayerPosition.CAM);
        double strikerStrength = positionStrength(availablePlayers, PlayerPosition.ST);

        if (wingerStrength >= centralStrength && wingerStrength >= strikerStrength) {
            return new FormationTarget(4, 3, 3, List.of("4-3-3"));
        }
        if (strikerStrength >= centralStrength) {
            return new FormationTarget(4, 4, 2, List.of("4-4-2", "4-1-2-1-2"));
        }
        if (centralStrength > 0) {
            return new FormationTarget(4, 5, 1, List.of("4-2-3-1"));
        }
        return difference >= 5
                ? new FormationTarget(4, 3, 3, List.of("4-3-3"))
                : new FormationTarget(4, 4, 2, List.of("4-4-2"));
    }

    private double positionStrength(List<SquadPlayer> players, PlayerPosition... positions) {
        List<PlayerPosition> positionList = List.of(positions);
        return players.stream()
                .filter(player -> positionList.contains(player.getPlayer().getPosition()))
                .mapToDouble(player -> playerEvaluationService.evaluatePlayer(player.getPlayer()))
                .average()
                .orElse(0);
    }

    private record FormationTarget(int defenders, int midfielders, int attackers,
                                   List<String> preferredNames) {

        private int distanceTo(Formation formation) {
            return Math.abs(formation.getDefenders() - defenders)
                    + Math.abs(formation.getMidfielders() - midfielders)
                    + Math.abs(formation.getAttackers() - attackers);
        }

        private int namePriority(Formation formation) {
            if (formation.getName() == null) {
                return 1;
            }
            String formationName = formation.getName().toLowerCase();
            return preferredNames.stream()
                    .map(String::toLowerCase)
                    .anyMatch(formationName::contains) ? 0 : 1;
        }
    }
}
