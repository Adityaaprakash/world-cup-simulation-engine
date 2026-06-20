package com.aditya.worldcup.simulation.service;

import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.simulation.dto.TeamStrengthResponse;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamStrengthService {

    private final SquadPlayerRepository squadPlayerRepository;

    public TeamStrengthResponse calculateStrength(Long squadId) {

        List<SquadPlayer> starters =
                squadPlayerRepository.findBySquadIdAndStartingXiTrue(squadId);

        int attack = averageRating(
                starters,
                List.of("ST", "CF", "LW", "RW")
        );

        int midfield = averageRating(
                starters,
                List.of("CM", "CDM", "CAM", "LM", "RM")
        );

        int defense = averageRating(
                starters,
                List.of("CB", "LB", "RB", "LWB", "RWB")
        );

        int goalkeeper = averageRating(
                starters,
                List.of("GK")
        );

        int overall = (int) (
                attack * 0.35 +
                        midfield * 0.35 +
                        defense * 0.20 +
                        goalkeeper * 0.10
        );

        long captains = starters.stream()
                .filter(sp -> Boolean.TRUE.equals(sp.getCaptain()))
                .count();

        int chemistry =
                starters.size() == 11 && captains == 1
                        ? 100
                        : 75;

        return new TeamStrengthResponse(
                attack,
                midfield,
                defense,
                goalkeeper,
                overall,
                chemistry
        );
    }

    private int averageRating(
            List<SquadPlayer> starters,
            List<String> positions
    ) {

        List<Integer> ratings =
                starters.stream()
                        .filter(sp ->
                                positions.contains(
                                        sp.getPositionSlot()
                                ))
                        .map(SquadPlayer::getPlayer)
                        .map(Player::getOverallRating)
                        .toList();

        if (ratings.isEmpty()) {
            return 0;
        }

        return (int) ratings.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);
    }
}