package com.aditya.worldcup.simulation.service;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.matchevents.entity.MatchEvent;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.matchevents.repository.MatchEventRepository;
import com.aditya.worldcup.matchstatistics.entity.MatchStatistics;
import com.aditya.worldcup.matchstatistics.repository.MatchStatisticsRepository;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.simulation.dto.MatchSimulationResponse;
import com.aditya.worldcup.simulation.dto.PlayerMatchRatingResponse;
import com.aditya.worldcup.simulation.entity.PlayerMatchRating;
import com.aditya.worldcup.simulation.repository.PlayerMatchRatingRepository;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchPersistenceService {

    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;
    private final PlayerMatchRatingRepository playerMatchRatingRepository;
    private final SquadRepository squadRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final PlayerRepository playerRepository;

    @Transactional
    public void persistSimulationData(Match match, MatchSimulationResponse simulation) {
        if (match == null || simulation == null) {
            return;
        }

        // 1. Update Match score and status
        match.setHomeScore(simulation.homeGoals());
        match.setAwayScore(simulation.awayGoals());
        match.setStatus(MatchStatus.FINISHED);

        // 2. Resolve squads for lookups
        Squad homeSquad = squadRepository.findFirstByTeamId(match.getHomeTeam().getId())
                .orElseThrow(() -> new IllegalArgumentException("No squad found for home team: " + match.getHomeTeam().getId()));
        Squad awaySquad = squadRepository.findFirstByTeamId(match.getAwayTeam().getId())
                .orElseThrow(() -> new IllegalArgumentException("No squad found for away team: " + match.getAwayTeam().getId()));

        List<SquadPlayer> allSquadPlayers = new ArrayList<>();
        allSquadPlayers.addAll(squadPlayerRepository.findBySquadId(homeSquad.getId()));
        allSquadPlayers.addAll(squadPlayerRepository.findBySquadId(awaySquad.getId()));

        // Create player name -> Player entity map
        Map<String, Player> nameToPlayerMap = allSquadPlayers.stream()
                .map(SquadPlayer::getPlayer)
                .collect(Collectors.toMap(Player::getName, p -> p, (p1, p2) -> p1));

        // Create player ID -> Player entity map
        Map<Long, Player> idToPlayerMap = allSquadPlayers.stream()
                .map(SquadPlayer::getPlayer)
                .collect(Collectors.toMap(Player::getId, p -> p, (p1, p2) -> p1));

        // 3. Set Man of the Match on Match entity
        if (simulation.manOfTheMatch() != null && simulation.manOfTheMatch().playerId() != null) {
            Long motmId = simulation.manOfTheMatch().playerId();
            Player motmPlayer = idToPlayerMap.get(motmId);
            if (motmPlayer == null) {
                motmPlayer = playerRepository.findById(motmId).orElse(null);
            }
            match.setManOfTheMatch(motmPlayer);
        }

        // Save match
        final Match savedMatch = matchRepository.save(match);

        // 4. Persist Match Statistics (Exactly one record per match)
        if (simulation.statistics() != null) {
            var statsDto = simulation.statistics();
            var homeStats = statsDto.homeTeam();
            var awayStats = statsDto.awayTeam();

            // Clear any existing stats for this match just in case
            matchStatisticsRepository.findByMatchId(savedMatch.getId())
                    .ifPresent(matchStatisticsRepository::delete);

            MatchStatistics statistics = MatchStatistics.builder()
                    .match(savedMatch)
                    .homePossession(homeStats.possession())
                    .awayPossession(awayStats.possession())
                    .homeShots(homeStats.shots())
                    .awayShots(awayStats.shots())
                    .homeShotsOnTarget(homeStats.shotsOnTarget())
                    .awayShotsOnTarget(awayStats.shotsOnTarget())
                    .homePasses(homeStats.passes())
                    .awayPasses(awayStats.passes())
                    .homePassAccuracy(homeStats.passAccuracy())
                    .awayPassAccuracy(awayStats.passAccuracy())
                    .homeCorners(homeStats.corners())
                    .awayCorners(awayStats.corners())
                    .homeFouls(homeStats.fouls())
                    .awayFouls(awayStats.fouls())
                    .homeOffsides(homeStats.offsides())
                    .awayOffsides(awayStats.offsides())
                    .homeYellowCards(homeStats.yellowCards())
                    .awayYellowCards(awayStats.yellowCards())
                    .homeRedCards(homeStats.redCards())
                    .awayRedCards(awayStats.redCards())
                    .homeSaves(homeStats.saves())
                    .awaySaves(awayStats.saves())
                    .homeExpectedGoals(homeStats.expectedGoals())
                    .awayExpectedGoals(awayStats.expectedGoals())
                    .build();

            matchStatisticsRepository.save(statistics);
        }

        // 5. Persist Player Ratings (Clear existing first to maintain clean state)
        List<PlayerMatchRating> existingRatings = playerMatchRatingRepository.findByMatchId(savedMatch.getId());
        if (!existingRatings.isEmpty()) {
            playerMatchRatingRepository.deleteAll(existingRatings);
        }

        if (simulation.playerRatings() != null) {
            for (PlayerMatchRatingResponse ratingDto : simulation.playerRatings()) {
                Player player = idToPlayerMap.get(ratingDto.playerId());
                if (player == null) {
                    player = playerRepository.findById(ratingDto.playerId()).orElse(null);
                }
                if (player != null) {
                    PlayerMatchRating rating = PlayerMatchRating.builder()
                            .match(savedMatch)
                            .player(player)
                            .rating(ratingDto.rating())
                            .build();
                    playerMatchRatingRepository.save(rating);
                }
            }
        }

        // 6. Persist Match Events
        List<MatchEvent> existingEvents = matchEventRepository.findByMatchId(savedMatch.getId());
        if (!existingEvents.isEmpty()) {
            matchEventRepository.deleteAll(existingEvents);
        }

        if (simulation.events() != null) {
            for (MatchEventResponse eventDto : simulation.events()) {
                Player player = nameToPlayerMap.get(eventDto.player());
                if (player == null) {
                    player = playerRepository.findAll().stream()
                            .filter(p -> p.getName().equalsIgnoreCase(eventDto.player()))
                            .findFirst()
                            .orElse(null);
                }
                if (player != null) {
                    MatchEvent matchEvent = MatchEvent.builder()
                            .match(savedMatch)
                            .player(player)
                            .minute(eventDto.minute())
                            .eventType(MatchEventType.valueOf(eventDto.eventType()))
                            .description(eventDto.description())
                            .build();
                    matchEventRepository.save(matchEvent);
                }
            }
        }
    }
}
