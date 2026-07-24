package com.aditya.worldcup.simulation.service;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.service.PlayerStateService;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PenaltyShootoutService {

    private final SquadRepository squadRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final PlayerStateService playerStateService;
    private final MatchModifierService matchModifierService;
    private final Random random = new Random();

    public boolean homeWinsShootout(Match match) {
        Optional<Squad> homeSquad = squadRepository.findFirstByTeamId(match.getHomeTeam().getId());
        Optional<Squad> awaySquad = squadRepository.findFirstByTeamId(match.getAwayTeam().getId());
        if (homeSquad.isEmpty() || awaySquad.isEmpty()) {
            return random.nextBoolean();
        }

        MatchContext context = new MatchContext(WeatherCondition.CLEAR);
        context.setCurrentPhase(MatchPhase.PENALTY_SHOOTOUT);
        List<SquadPlayer> homeOrder = penaltyOrder(homeSquad.get());
        List<SquadPlayer> awayOrder = penaltyOrder(awaySquad.get());
        Player homeGoalkeeper = goalkeeper(homeSquad.get());
        Player awayGoalkeeper = goalkeeper(awaySquad.get());

        int homeGoals = 0;
        int awayGoals = 0;
        for (int kick = 0; kick < 5; kick++) {
            if (scores(homeOrder, kick, awayGoalkeeper, true, kick, context)) {
                homeGoals++;
            }
            if (scores(awayOrder, kick, homeGoalkeeper, false, kick, context)) {
                awayGoals++;
            }
            int remaining = 4 - kick;
            if (homeGoals > awayGoals + remaining || awayGoals > homeGoals + remaining) {
                return homeGoals > awayGoals;
            }
        }

        int kick = 5;
        while (kick < Math.max(homeOrder.size(), awayOrder.size()) + 5) {
            boolean homeScores = scores(homeOrder, kick, awayGoalkeeper, true, kick, context);
            boolean awayScores = scores(awayOrder, kick, homeGoalkeeper, false, kick, context);
            if (homeScores != awayScores) {
                return homeScores;
            }
            kick++;
        }
        return random.nextBoolean();
    }

    private List<SquadPlayer> penaltyOrder(Squad squad) {
        return squadPlayerRepository.findBySquadIdAndStartingXiTrue(squad.getId())
                .stream()
                .filter(player -> player.getPlayer().getPosition() != PlayerPosition.GK)
                .filter(player -> playerStateService.isAvailable(
                        playerStateService.getOrCreateState(player.getPlayer())))
                .sorted(Comparator.comparingDouble(this::penaltyScore).reversed())
                .toList();
    }

    private Player goalkeeper(Squad squad) {
        return squadPlayerRepository.findBySquadIdAndStartingXiTrue(squad.getId())
                .stream()
                .map(SquadPlayer::getPlayer)
                .filter(player -> player.getPosition() == PlayerPosition.GK)
                .findFirst()
                .orElse(null);
    }

    private boolean scores(List<SquadPlayer> order,
                           int kick,
                           Player goalkeeper,
                           boolean homeTeam,
                           int kickNumber,
                           MatchContext context) {
        if (order.isEmpty()) {
            return random.nextBoolean();
        }
        Player shooter = order.get(kick % order.size()).getPlayer();
        PlayerState shooterState = playerStateService.getOrCreateState(shooter);
        double goalkeeperConfidence = goalkeeper == null ? 50
                : playerStateService.getOrCreateState(goalkeeper).getConfidence();
        double probability = matchModifierService.shootoutScore(
                homeTeam,
                shooterState.getConfidence(),
                goalkeeperConfidence,
                shooterState.getFatigue(),
                kickNumber,
                context);
        probability += shooter.getShooting() * 0.18
                + ((shooter.getPassing() + shooter.getDribbling()) / 2.0) * 0.04;
        return random.nextDouble() * 100 < Math.max(45, Math.min(92, probability));
    }

    private double penaltyScore(SquadPlayer player) {
        PlayerState state = playerStateService.getOrCreateState(player.getPlayer());
        return player.getPlayer().getShooting()
                + player.getPlayer().getOverallRating() * 0.35
                + state.getConfidence() * 0.18
                - state.getFatigue() * 0.12;
    }
}
