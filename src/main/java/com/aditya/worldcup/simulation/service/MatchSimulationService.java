package com.aditya.worldcup.simulation.service;

import com.aditya.worldcup.simulation.dto.MatchSimulationRequest;
import com.aditya.worldcup.simulation.dto.MatchSimulationResponse;
import com.aditya.worldcup.simulation.dto.MatchStatisticsResponse;
import com.aditya.worldcup.simulation.dto.ManOfTheMatchResponse;
import com.aditya.worldcup.simulation.dto.PlayerMatchRatingResponse;
import com.aditya.worldcup.simulation.dto.TeamStrengthResponse;
import com.aditya.worldcup.simulation.dto.CommentaryResponse;
import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.matchevents.service.MatchEventGenerationService;
import com.aditya.worldcup.players.service.PlayerStateService;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.squadplayers.dto.SquadReadyResponse;
import com.aditya.worldcup.squadplayers.service.SquadPlayerService;
import com.aditya.worldcup.ml.dto.PredictionRequest;
import com.aditya.worldcup.ml.dto.PredictionResponse;
import com.aditya.worldcup.ml.exception.MlServiceException;
import com.aditya.worldcup.ml.mapper.MlFeatureMapper;
import com.aditya.worldcup.ml.service.MlPredictionService;
import com.aditya.worldcup.tactics.entity.TacticalProfile;
import com.aditya.worldcup.tactics.service.TacticalMatchModifiers;
import com.aditya.worldcup.tactics.service.TacticalModifierService;
import com.aditya.worldcup.tactics.service.TacticalProfileService;
import com.aditya.worldcup.ai.service.AiManagerService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchSimulationService {

    private final TeamStrengthService teamStrengthService;
    private final SquadRepository squadRepository;
    private final SquadPlayerService squadPlayerService;
    private final MatchEventGenerationService matchEventGenerationService;
    private final MatchStatisticsGenerationService
            matchStatisticsGenerationService;
    private final PlayerRatingGenerationService playerRatingGenerationService;
    private final ManOfTheMatchService manOfTheMatchService;
    private final MatchCommentaryService matchCommentaryService;
    private final MatchPersistenceService matchPersistenceService;
    private final MlFeatureMapper mlFeatureMapper;
    private final MlPredictionService mlPredictionService;
    private final PlayerStateService playerStateService;
    private final TacticalProfileService tacticalProfileService;
    private final TacticalModifierService tacticalModifierService;
    private final AiManagerService aiManagerService;

    private final Random random = new Random();

    public MatchSimulationResponse simulate(
            MatchSimulationRequest request
    ) {
        return simulate(request, null);
    }

    public MatchSimulationResponse simulate(
            MatchSimulationRequest request,
            Match match
    ) {

        Squad homeSquad = squadRepository.findById(
                request.homeSquadId()
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Home squad not found: " + request.homeSquadId()
                ));

        Squad awaySquad = squadRepository.findById(
                request.awaySquadId()
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Away squad not found: " + request.awaySquadId()
                ));

        aiManagerService.prepareForMatch(homeSquad, awaySquad);
        aiManagerService.prepareForMatch(awaySquad, homeSquad);

        SquadReadyResponse homeReady =
                squadPlayerService.getSquadReadyStatus(
                        homeSquad.getId()
                );

        if (!homeReady.ready()) {
            throw new IllegalArgumentException(
                    "Home squad is not match ready: "
                            + homeReady.message()
            );
        }

        SquadReadyResponse awayReady =
                squadPlayerService.getSquadReadyStatus(
                        awaySquad.getId()
                );

        if (!awayReady.ready()) {
            throw new IllegalArgumentException(
                    "Away squad is not match ready: "
                            + awayReady.message()
            );
        }

        TeamStrengthResponse homeStrength =
                teamStrengthService.calculateStrength(
                        homeSquad.getId()
                );

        TeamStrengthResponse awayStrength =
                teamStrengthService.calculateStrength(
                        awaySquad.getId()
                );

        int homeOverall = homeStrength.overall();
        int awayOverall = awayStrength.overall();

        TacticalProfile homeProfile = tacticalProfileService.getOrCreateProfile(
                homeSquad.getTeam());
        TacticalProfile awayProfile = tacticalProfileService.getOrCreateProfile(
                awaySquad.getTeam());
        TacticalMatchModifiers homeTactics = tacticalModifierService
                .calculateModifiers(homeProfile, awayProfile);
        TacticalMatchModifiers awayTactics = tacticalModifierService
                .calculateModifiers(awayProfile, homeProfile);

        Scoreline scoreline = selectScoreline(
                homeSquad,
                awaySquad,
                homeStrength,
                awayStrength,
                match,
                homeOverall - awayOverall,
                homeTactics,
                awayTactics
        );
        int homeGoals = scoreline.homeGoals();
        int awayGoals = scoreline.awayGoals();

        homeProfile = aiManagerService.adjustTacticsForMatchState(
                homeSquad, Integer.compare(homeGoals, awayGoals));
        awayProfile = aiManagerService.adjustTacticsForMatchState(
                awaySquad, Integer.compare(awayGoals, homeGoals));
        homeTactics = tacticalModifierService.calculateModifiers(homeProfile, awayProfile);
        awayTactics = tacticalModifierService.calculateModifiers(awayProfile, homeProfile);

        String winner;

        if (homeGoals > awayGoals) {
            winner = homeSquad.getName();
        } else if (awayGoals > homeGoals) {
            winner = awaySquad.getName();
        } else {
            winner = "DRAW";
        }

        List<MatchEventResponse> generatedEvents =
                matchEventGenerationService.generateMatchEvents(
                        homeSquad.getId(),
                        awaySquad.getId(),
                        homeGoals,
                        awayGoals,
                        homeTactics,
                        awayTactics
                );
        List<MatchEventResponse> events = new ArrayList<>(generatedEvents.stream()
                .filter(event -> !"SUBSTITUTION".equals(event.eventType()))
                .toList());
        events.addAll(aiManagerService.decideSubstitutions(homeSquad,
                Integer.compare(homeGoals, awayGoals)));
        events.addAll(aiManagerService.decideSubstitutions(awaySquad,
                Integer.compare(awayGoals, homeGoals)));
        events.sort(Comparator.comparing(MatchEventResponse::minute));

        List<CommentaryResponse> commentary =
                matchCommentaryService.generate(events);

        MatchStatisticsResponse statistics =
                matchStatisticsGenerationService.generate(
                        homeGoals,
                        awayGoals,
                        homeOverall,
                        awayOverall,
                        homeTactics,
                        awayTactics
                );

        List<PlayerMatchRatingResponse> playerRatings =
                playerRatingGenerationService.generate(
                        homeSquad.getId(),
                        awaySquad.getId(),
                        homeSquad.getName(),
                        awaySquad.getName(),
                        homeGoals,
                        awayGoals,
                        events,
                        statistics
                );

        ManOfTheMatchResponse manOfTheMatch =
                manOfTheMatchService.determine(
                        playerRatings,
                        events,
                        winner
                );

        MatchSimulationResponse response = new MatchSimulationResponse(
                homeSquad.getName(),
                awaySquad.getName(),
                homeGoals,
                awayGoals,
                winner,
                homeOverall,
                awayOverall,
                events,
                statistics,
                playerRatings,
                manOfTheMatch,
                commentary
        );

        if (match != null) {
            matchPersistenceService.persistSimulationData(match, response);
        }

        playerStateService.updateAfterMatch(
                homeSquad.getId(),
                awaySquad.getId(),
                homeGoals,
                awayGoals,
                events,
                homeTactics,
                awayTactics
        );

        return response;
    }

    private Scoreline selectScoreline(
            Squad homeSquad,
            Squad awaySquad,
            TeamStrengthResponse homeStrength,
            TeamStrengthResponse awayStrength,
            Match match,
            int strengthDifference,
            TacticalMatchModifiers homeTactics,
            TacticalMatchModifiers awayTactics
    ) {
        if (!mlPredictionService.isMlServiceAvailable()) {
            log.warn("ML prediction service is unavailable; heuristic match simulation fallback activated.");
            return heuristicScoreline(tacticalStrengthDifference(
                    strengthDifference, homeTactics, awayTactics));
        }

        try {
            PredictionRequest request = mlFeatureMapper.map(
                    homeSquad, awaySquad, homeStrength, awayStrength, match
            );
            PredictionResponse prediction = mlPredictionService.predict(request);
            log.info("ML prediction service available; using hybrid score selection.");
            return mlScoreline(prediction, homeTactics, awayTactics);
        } catch (MlServiceException exception) {
            log.warn("ML prediction failed; heuristic match simulation fallback activated.");
            return heuristicScoreline(tacticalStrengthDifference(
                    strengthDifference, homeTactics, awayTactics));
        }
    }

    private Scoreline heuristicScoreline(int difference) {
        if (difference >= 5) {
            return new Scoreline(random.nextInt(4), random.nextInt(3));
        }
        if (difference <= -5) {
            return new Scoreline(random.nextInt(3), random.nextInt(4));
        }
        return new Scoreline(random.nextInt(4), random.nextInt(4));
    }

    private Scoreline mlScoreline(PredictionResponse prediction,
                                  TacticalMatchModifiers homeTactics,
                                  TacticalMatchModifiers awayTactics) {
        MatchOutcome outcome = sampleOutcome(prediction);
        int homeGoals = goalsAround(prediction.expectedHomeGoals()
                + expectedGoalModifier(homeTactics, awayTactics));
        int awayGoals = goalsAround(prediction.expectedAwayGoals()
                + expectedGoalModifier(awayTactics, homeTactics));

        if (outcome == MatchOutcome.HOME_WIN && homeGoals <= awayGoals) {
            homeGoals = Math.min(8, awayGoals + 1);
        } else if (outcome == MatchOutcome.AWAY_WIN && awayGoals <= homeGoals) {
            awayGoals = Math.min(8, homeGoals + 1);
        } else if (outcome == MatchOutcome.DRAW && homeGoals != awayGoals) {
            int drawnGoals = Math.max(0, Math.min(8, (int) Math.round((homeGoals + awayGoals) / 2.0)));
            homeGoals = drawnGoals;
            awayGoals = drawnGoals;
        }
        return new Scoreline(homeGoals, awayGoals);
    }

    private MatchOutcome sampleOutcome(PredictionResponse prediction) {
        double homeProbability = Math.max(0.0, prediction.homeWinProbability());
        double drawProbability = Math.max(0.0, prediction.drawProbability());
        double awayProbability = Math.max(0.0, prediction.awayWinProbability());
        double total = homeProbability + drawProbability + awayProbability;
        if (total == 0.0) {
            return MatchOutcome.DRAW;
        }
        double sample = random.nextDouble() * total;
        if (sample < homeProbability) {
            return MatchOutcome.HOME_WIN;
        }
        if (sample < homeProbability + drawProbability) {
            return MatchOutcome.DRAW;
        }
        return MatchOutcome.AWAY_WIN;
    }

    private int goalsAround(double expectedGoals) {
        double deviation = Math.sqrt(Math.max(0.5, expectedGoals));
        int goals = (int) Math.round(expectedGoals + random.nextGaussian() * deviation);
        return Math.max(0, Math.min(8, goals));
    }

    private int tacticalStrengthDifference(int strengthDifference,
                                           TacticalMatchModifiers homeTactics,
                                           TacticalMatchModifiers awayTactics) {
        return strengthDifference + (int) Math.round(
                (homeTactics.attackModifier() + homeTactics.counterModifier()
                        - awayTactics.attackModifier() - awayTactics.counterModifier()) * 3);
    }

    private double expectedGoalModifier(TacticalMatchModifiers tactics,
                                        TacticalMatchModifiers opponentTactics) {
        return (tactics.attackModifier() + tactics.counterModifier()
                - opponentTactics.defenseModifier()) * 0.18;
    }

    private record Scoreline(int homeGoals, int awayGoals) {
    }

    private enum MatchOutcome {
        HOME_WIN,
        DRAW,
        AWAY_WIN
    }
}
