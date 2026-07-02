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
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.aditya.worldcup.squadplayers.dto.SquadReadyResponse;
import com.aditya.worldcup.squadplayers.service.SquadPlayerService;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
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

    private final Random random = new Random();

    public MatchSimulationResponse simulate(
            MatchSimulationRequest request
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

        int difference = homeOverall - awayOverall;

        int homeGoals;
        int awayGoals;

        if (difference >= 5) {

            homeGoals = random.nextInt(4);
            awayGoals = random.nextInt(3);

        } else if (difference <= -5) {

            homeGoals = random.nextInt(3);
            awayGoals = random.nextInt(4);

        } else {

            homeGoals = random.nextInt(4);
            awayGoals = random.nextInt(4);
        }

        String winner;

        if (homeGoals > awayGoals) {
            winner = homeSquad.getName();
        } else if (awayGoals > homeGoals) {
            winner = awaySquad.getName();
        } else {
            winner = "DRAW";
        }

        List<MatchEventResponse> events =
                matchEventGenerationService.generateMatchEvents(
                        homeSquad.getId(),
                        awaySquad.getId(),
                        homeGoals,
                        awayGoals
                );

        List<CommentaryResponse> commentary =
                matchCommentaryService.generate(events);

        MatchStatisticsResponse statistics =
                matchStatisticsGenerationService.generate(
                        homeGoals,
                        awayGoals,
                        homeOverall,
                        awayOverall
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

        return new MatchSimulationResponse(
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
    }
}
