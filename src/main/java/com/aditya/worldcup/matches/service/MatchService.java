package com.aditya.worldcup.matches.service;

import com.aditya.worldcup.matches.dto.MatchResponse;
import com.aditya.worldcup.matches.dto.MatchResultRequest;
import com.aditya.worldcup.matches.dto.MatchDetailResponse;
import com.aditya.worldcup.matches.dto.MatchHistoryResponse;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.matchevents.entity.MatchEvent;
import com.aditya.worldcup.matchevents.repository.MatchEventRepository;
import com.aditya.worldcup.matchstatistics.entity.MatchStatistics;
import com.aditya.worldcup.matchstatistics.repository.MatchStatisticsRepository;
import com.aditya.worldcup.simulation.dto.MatchStatisticsResponse;
import com.aditya.worldcup.simulation.dto.PlayerMatchRatingResponse;
import com.aditya.worldcup.simulation.dto.ManOfTheMatchResponse;
import com.aditya.worldcup.simulation.dto.CommentaryResponse;
import com.aditya.worldcup.simulation.entity.PlayerMatchRating;
import com.aditya.worldcup.simulation.repository.PlayerMatchRatingRepository;
import com.aditya.worldcup.simulation.service.MatchCommentaryService;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.teams.repository.TeamRepository;
import com.aditya.worldcup.shared.exception.TournamentNotFoundException;
import com.aditya.worldcup.shared.exception.TeamNotFoundException;
import com.aditya.worldcup.standings.service.StandingUpdateService;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final StandingUpdateService standingUpdateService;
    private final MatchEventRepository matchEventRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;
    private final PlayerMatchRatingRepository playerMatchRatingRepository;
    private final MatchCommentaryService matchCommentaryService;


    public List<MatchResponse> getTournamentMatches(Long tournamentId) {

        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException(tournamentId);
        }

        return matchRepository.findByTournamentIdOrderById(tournamentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public MatchResponse completeMatch(
            Long tournamentId,
            Long matchId,
            MatchResultRequest request
    ) {

        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException(tournamentId);
        }

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Match not found with id: " + matchId
                ));

        if (match.getTournament() == null
                || !match.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException(
                    "Match does not belong to tournament: " + tournamentId
            );
        }

        validateResult(request);

        if (match.getStatus() == MatchStatus.FINISHED) {
            throw new IllegalArgumentException(
                    "Match has already been completed"
            );
        }

        match.setHomeScore(request.homeGoals());
        match.setAwayScore(request.awayGoals());
        match.setStatus(MatchStatus.FINISHED);

        Match savedMatch = matchRepository.save(match);

        standingUpdateService.updateStandings(
                savedMatch,
                request.homeGoals(),
                request.awayGoals()
        );

        return mapToResponse(savedMatch);
    }

    public MatchResponse mapToResponse(Match match) {

        return new MatchResponse(
                match.getId(),
                match.getGroup() == null
                        ? null
                        : match.getGroup().getName(),
                match.getRound(),
                match.getHomeTeam().getName(),
                match.getAwayTeam().getName(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getStatus()
        );
    }

    private void validateResult(MatchResultRequest request) {

        if (request.homeGoals() == null || request.awayGoals() == null) {
            throw new IllegalArgumentException(
                    "Both homeGoals and awayGoals are required"
            );
        }

        if (request.homeGoals() < 0 || request.awayGoals() < 0) {
            throw new IllegalArgumentException(
                    "Goals cannot be negative"
            );
        }
    }

    public MatchDetailResponse getMatchDetail(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found with id: " + matchId));

        MatchStatisticsResponse statistics = matchStatisticsRepository.findByMatchId(matchId)
                .map(this::mapToStatisticsResponse)
                .orElse(null);

        List<MatchEvent> events = matchEventRepository.findByMatchId(matchId);
        List<MatchEventResponse> eventResponses = events.stream()
                .map(this::mapToEventResponse)
                .toList();

        List<PlayerMatchRating> playerRatings = playerMatchRatingRepository.findByMatchId(matchId);
        List<PlayerMatchRatingResponse> playerRatingResponses = playerRatings.stream()
                .map(pr -> mapToPlayerRatingResponse(pr, match))
                .toList();

        ManOfTheMatchResponse motmResponse = null;
        if (match.getManOfTheMatch() != null) {
            Player motmPlayer = match.getManOfTheMatch();
            Double rating = playerRatings.stream()
                    .filter(pr -> pr.getPlayer().getId().equals(motmPlayer.getId()))
                    .map(PlayerMatchRating::getRating)
                    .findFirst()
                    .orElse(0.0);

            motmResponse = new ManOfTheMatchResponse(
                    motmPlayer.getId(),
                    motmPlayer.getName(),
                    determinePlayerTeam(motmPlayer, match),
                    motmPlayer.getPosition().name(),
                    rating
            );
        }

        List<CommentaryResponse> commentary = matchCommentaryService.generate(eventResponses);

        String winner = "DRAW";
        if (match.getStatus() == MatchStatus.FINISHED && match.getHomeScore() != null && match.getAwayScore() != null) {
            if (match.getHomeScore() > match.getAwayScore()) {
                winner = match.getHomeTeam().getName();
            } else if (match.getAwayScore() > match.getHomeScore()) {
                winner = match.getAwayTeam().getName();
            }
        }

        return new MatchDetailResponse(
                match.getId(),
                match.getGroup() == null ? null : match.getGroup().getName(),
                match.getRound(),
                match.getHomeTeam().getName(),
                match.getAwayTeam().getName(),
                match.getHomeScore(),
                match.getAwayScore(),
                winner,
                match.getStatus(),
                statistics,
                eventResponses,
                playerRatingResponses,
                motmResponse,
                commentary
        );
    }

    public List<MatchHistoryResponse> getMatchHistory() {
        List<Match> matches = matchRepository.findCompletedMatchesHistory();
        return matches.stream()
                .map(this::mapToHistoryResponse)
                .toList();
    }

    public List<MatchHistoryResponse> getTeamMatchHistory(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new TeamNotFoundException(teamId);
        }
        List<Match> matches = matchRepository.findCompletedMatchesByTeamId(teamId);
        return matches.stream()
                .map(this::mapToHistoryResponse)
                .toList();
    }

    private MatchEventResponse mapToEventResponse(MatchEvent event) {
        return new MatchEventResponse(
                event.getMinute(),
                event.getPlayer().getName(),
                event.getEventType().name(),
                event.getDescription()
        );
    }

    private PlayerMatchRatingResponse mapToPlayerRatingResponse(PlayerMatchRating pr, Match match) {
        return new PlayerMatchRatingResponse(
                pr.getPlayer().getId(),
                pr.getPlayer().getName(),
                pr.getPlayer().getPosition().name(),
                determinePlayerTeam(pr.getPlayer(), match),
                pr.getRating()
        );
    }

    private String determinePlayerTeam(Player player, Match match) {
        if (player.getCountry() != null && match.getHomeTeam().getCountry() != null
                && player.getCountry().getId().equals(match.getHomeTeam().getCountry().getId())) {
            return match.getHomeTeam().getName();
        }
        return match.getAwayTeam().getName();
    }

    private MatchStatisticsResponse mapToStatisticsResponse(MatchStatistics stats) {
        var home = new MatchStatisticsResponse.TeamStatisticsResponse(
                stats.getHomePossession(),
                stats.getHomeShots(),
                stats.getHomeShotsOnTarget(),
                stats.getHomePasses(),
                stats.getHomePassAccuracy(),
                stats.getHomeCorners(),
                stats.getHomeFouls(),
                stats.getHomeOffsides(),
                stats.getHomeYellowCards(),
                stats.getHomeRedCards(),
                stats.getHomeSaves(),
                stats.getHomeExpectedGoals()
        );
        var away = new MatchStatisticsResponse.TeamStatisticsResponse(
                stats.getAwayPossession(),
                stats.getAwayShots(),
                stats.getAwayShotsOnTarget(),
                stats.getAwayPasses(),
                stats.getAwayPassAccuracy(),
                stats.getAwayCorners(),
                stats.getAwayFouls(),
                stats.getAwayOffsides(),
                stats.getAwayYellowCards(),
                stats.getAwayRedCards(),
                stats.getAwaySaves(),
                stats.getAwayExpectedGoals()
        );
        return new MatchStatisticsResponse(home, away);
    }

    private MatchHistoryResponse mapToHistoryResponse(Match match) {
        String winner = "DRAW";
        if (match.getHomeScore() > match.getAwayScore()) {
            winner = match.getHomeTeam().getName();
        } else if (match.getAwayScore() > match.getHomeScore()) {
            winner = match.getAwayTeam().getName();
        }

        String score = match.getHomeScore() + " - " + match.getAwayScore();

        return new MatchHistoryResponse(
                match.getId(),
                match.getHomeTeam().getName(),
                match.getAwayTeam().getName(),
                score,
                winner,
                match.getStatus(),
                match.getMatchDate()
        );
    }
}
