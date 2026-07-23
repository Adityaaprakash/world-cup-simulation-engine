package com.aditya.worldcup.ai.service;

import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.service.PlayerStateService;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.squads.entity.Squad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaptainSelectionService {

    private final SquadPlayerRepository squadPlayerRepository;
    private final PlayerStateService playerStateService;
    private final PlayerEvaluationService playerEvaluationService;

    @Transactional
    public void selectCaptainAndViceCaptain(Squad squad) {
        List<SquadPlayer> starters = squadPlayerRepository.findBySquadIdAndStartingXiTrue(squad.getId())
                .stream()
                .sorted(Comparator.comparingDouble(this::leadershipScore).reversed())
                .toList();
        if (starters.isEmpty()) {
            return;
        }

        List<SquadPlayer> allPlayers = squadPlayerRepository.findBySquadId(squad.getId());
        allPlayers.forEach(player -> {
            player.setCaptain(false);
            player.setViceCaptain(false);
        });
        starters.getFirst().setCaptain(true);
        if (starters.size() > 1) {
            starters.get(1).setViceCaptain(true);
        }
        squadPlayerRepository.saveAll(allPlayers);
    }

    private double leadershipScore(SquadPlayer squadPlayer) {
        PlayerState state = playerStateService.getOrCreateState(squadPlayer.getPlayer());
        return squadPlayer.getPlayer().getAge() * 0.35
                + playerEvaluationService.evaluatePlayer(squadPlayer.getPlayer()) * 0.35
                + state.getConfidence() * 0.18
                + state.getMorale() * 0.12;
    }
}
