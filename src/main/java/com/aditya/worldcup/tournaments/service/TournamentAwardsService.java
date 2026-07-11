package com.aditya.worldcup.tournaments.service;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.matchevents.entity.MatchEvent;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.matchevents.repository.MatchEventRepository;
import com.aditya.worldcup.matchstatistics.repository.MatchStatisticsRepository;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.shared.exception.TournamentNotFoundException;
import com.aditya.worldcup.simulation.entity.PlayerMatchRating;
import com.aditya.worldcup.simulation.repository.PlayerMatchRatingRepository;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.tournaments.dto.TournamentAwardsResponse;
import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.tournaments.entity.TournamentStatus;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import com.aditya.worldcup.tournamentteams.entity.TournamentTeam;
import com.aditya.worldcup.tournamentteams.repository.TournamentTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentAwardsService {

    private static final int GOLDEN_BALL_MINIMUM_MATCHES = 3;
    private static final int GOALKEEPER_MINIMUM_MATCHES = 3;
    private static final int YOUNG_PLAYER_MINIMUM_MATCHES = 2;
    private static final int BEST_YOUNG_PLAYER_MAXIMUM_AGE = 21;

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;
    private final PlayerMatchRatingRepository playerMatchRatingRepository;
    private final TournamentTeamRepository tournamentTeamRepository;

    @Transactional(readOnly = true)
    public TournamentAwardsResponse calculateAwards(Long tournamentId) {

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(
                        tournamentId
                ));

        List<Match> tournamentMatches =
                matchRepository.findByTournamentIdOrderById(tournamentId);

        validateTournamentIsFinished(
                tournament,
                tournamentMatches
        );

        List<Match> completedMatches =
                tournamentMatches.stream()
                        .filter(match -> match.getStatus()
                                == MatchStatus.FINISHED)
                        .toList();

        validateCompletedMatches(completedMatches);

        Map<Long, Team> teamsByCountryId =
                tournamentTeamRepository.findByTournamentId(tournamentId)
                        .stream()
                        .map(TournamentTeam::getTeam)
                        .collect(Collectors.toMap(
                                team -> team.getCountry().getId(),
                                team -> team,
                                (first, second) -> first
                        ));

        Map<Long, PlayerTournamentStat> playerStats =
                new LinkedHashMap<>();

        for (Match match : completedMatches) {
            collectRatings(
                    match,
                    teamsByCountryId,
                    playerStats
            );

            collectEvents(
                    match,
                    teamsByCountryId,
                    playerStats
            );
        }

        if (playerStats.isEmpty()) {
            throw new IllegalArgumentException(
                    "Tournament has no persisted player ratings"
            );
        }

        TournamentAwardsResponse.GoldenBootAward goldenBoot =
                buildGoldenBoot(playerStats);

        TournamentAwardsResponse.RatingAward goldenBall =
                buildRatingAward(
                        selectRatingAwardWinner(
                                playerStats,
                                stat -> stat.matchesPlayed
                                        >= GOLDEN_BALL_MINIMUM_MATCHES
                        )
                );

        TournamentAwardsResponse.RatingAward goldenGlove =
                buildRatingAward(
                        selectGoldenGloveWinner(playerStats)
                );

        TournamentAwardsResponse.RatingAward bestYoungPlayer =
                buildRatingAward(
                        selectRatingAwardWinner(
                                playerStats,
                                stat -> stat.matchesPlayed
                                        >= YOUNG_PLAYER_MINIMUM_MATCHES
                                        && stat.player.getAge()
                                        <= BEST_YOUNG_PLAYER_MAXIMUM_AGE
                        )
                );

        List<TournamentAwardsResponse.TeamOfTournamentPlayer>
                teamOfTheTournament =
                buildTeamOfTheTournament(playerStats);

        return new TournamentAwardsResponse(
                goldenBoot,
                goldenBall,
                goldenGlove,
                bestYoungPlayer,
                teamOfTheTournament
        );
    }

    private void validateTournamentIsFinished(
            Tournament tournament,
            List<Match> matches
    ) {

        if (matches.isEmpty()
                || tournament.getStatus() == TournamentStatus.UPCOMING) {
            throw new IllegalArgumentException(
                    "Tournament has no completed matches"
            );
        }

        boolean hasUnfinishedMatches =
                matches.stream()
                        .anyMatch(match -> match.getStatus()
                                != MatchStatus.FINISHED);

        if (hasUnfinishedMatches) {
            throw new IllegalArgumentException(
                    "Tournament has not finished"
            );
        }
    }

    private void validateCompletedMatches(List<Match> completedMatches) {

        if (completedMatches.isEmpty()) {
            throw new IllegalArgumentException(
                    "Tournament has no completed matches"
            );
        }

        for (Match match : completedMatches) {
            matchStatisticsRepository.findByMatchId(match.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Missing statistics for match: "
                                    + match.getId()
                    ));
        }
    }

    private void collectRatings(
            Match match,
            Map<Long, Team> teamsByCountryId,
            Map<Long, PlayerTournamentStat> playerStats
    ) {

        for (PlayerMatchRating rating :
                playerMatchRatingRepository.findByMatchId(match.getId())) {

            PlayerTournamentStat stat =
                    getOrCreateStat(
                            rating.getPlayer(),
                            teamsByCountryId,
                            playerStats
                    );

            stat.matchesPlayed++;
            stat.totalRating += rating.getRating();

            if (rating.getPlayer().getPosition() == PlayerPosition.GK) {
                addGoalkeeperMatchResult(
                        stat,
                        match
                );
            }
        }
    }

    private void collectEvents(
            Match match,
            Map<Long, Team> teamsByCountryId,
            Map<Long, PlayerTournamentStat> playerStats
    ) {

        for (MatchEvent event : matchEventRepository.findByMatchId(
                match.getId()
        )) {

            if (event.getPlayer() == null) {
                continue;
            }

            PlayerTournamentStat stat =
                    getOrCreateStat(
                            event.getPlayer(),
                            teamsByCountryId,
                            playerStats
                    );

            if (event.getEventType() == MatchEventType.GOAL) {
                stat.goals++;
            } else if (event.getEventType() == MatchEventType.ASSIST) {
                stat.assists++;
            }
        }
    }

    private PlayerTournamentStat getOrCreateStat(
            Player player,
            Map<Long, Team> teamsByCountryId,
            Map<Long, PlayerTournamentStat> playerStats
    ) {

        return playerStats.computeIfAbsent(
                player.getId(),
                playerId -> new PlayerTournamentStat(
                        player,
                        teamsByCountryId.get(player.getCountry().getId())
                )
        );
    }

    private void addGoalkeeperMatchResult(
            PlayerTournamentStat stat,
            Match match
    ) {

        Integer goalsConceded = goalkeeperGoalsConceded(
                stat.team,
                match
        );

        if (goalsConceded == null) {
            return;
        }

        stat.goalsConceded += goalsConceded;

        if (goalsConceded == 0) {
            stat.cleanSheets++;
        }
    }

    private Integer goalkeeperGoalsConceded(
            Team team,
            Match match
    ) {

        if (team == null || match.getHomeTeam() == null
                || match.getAwayTeam() == null) {
            return null;
        }

        if (team.getId().equals(match.getHomeTeam().getId())) {
            return match.getAwayScore();
        }

        if (team.getId().equals(match.getAwayTeam().getId())) {
            return match.getHomeScore();
        }

        return null;
    }

    private TournamentAwardsResponse.GoldenBootAward buildGoldenBoot(
            Map<Long, PlayerTournamentStat> playerStats
    ) {

        PlayerTournamentStat winner =
                playerStats.values()
                        .stream()
                        .filter(stat -> stat.goals > 0)
                        .max(Comparator
                                .comparingInt(
                                        PlayerTournamentStat::goals
                                )
                                .thenComparingInt(
                                        PlayerTournamentStat::assists
                                )
                                .thenComparingDouble(
                                        PlayerTournamentStat::averageRating
                                )
                                .thenComparing(stat ->
                                        -stat.player.getId()))
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Tournament has no goal scorers"
                        ));

        return new TournamentAwardsResponse.GoldenBootAward(
                winner.player.getId(),
                winner.player.getName(),
                teamName(winner),
                winner.goals
        );
    }

    private PlayerTournamentStat selectRatingAwardWinner(
            Map<Long, PlayerTournamentStat> playerStats,
            Predicate<PlayerTournamentStat> eligibility
    ) {

        return playerStats.values()
                .stream()
                .filter(eligibility)
                .max(Comparator
                        .comparingDouble(
                                PlayerTournamentStat::averageRating
                        )
                        .thenComparingInt(
                                PlayerTournamentStat::goals
                        )
                        .thenComparingInt(
                                PlayerTournamentStat::assists
                        )
                        .thenComparing(stat -> -stat.player.getId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No eligible player found for tournament award"
                ));
    }

    private PlayerTournamentStat selectGoldenGloveWinner(
            Map<Long, PlayerTournamentStat> playerStats
    ) {

        return playerStats.values()
                .stream()
                .filter(stat -> stat.player.getPosition()
                        == PlayerPosition.GK)
                .filter(stat -> stat.matchesPlayed
                        >= GOALKEEPER_MINIMUM_MATCHES)
                .max(Comparator
                        .comparingDouble(
                                PlayerTournamentStat::averageRating
                        )
                        .thenComparingInt(stat -> stat.cleanSheets)
                        .thenComparingInt(stat ->
                                -stat.goalsConceded)
                        .thenComparing(stat -> -stat.player.getId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No eligible goalkeeper found for tournament award"
                ));
    }

    private TournamentAwardsResponse.RatingAward buildRatingAward(
            PlayerTournamentStat winner
    ) {

        return new TournamentAwardsResponse.RatingAward(
                winner.player.getId(),
                winner.player.getName(),
                teamName(winner),
                winner.player.getPosition().name(),
                round(winner.averageRating())
        );
    }

    private List<TournamentAwardsResponse.TeamOfTournamentPlayer>
    buildTeamOfTheTournament(
            Map<Long, PlayerTournamentStat> playerStats
    ) {

        List<PlayerTournamentStat> candidates =
                playerStats.values()
                        .stream()
                        .filter(stat -> stat.matchesPlayed > 0)
                        .sorted(playerRankingComparator())
                        .toList();

        if (candidates.size() < 11) {
            throw new IllegalArgumentException(
                    "Not enough rated players for Team of the Tournament"
            );
        }

        List<TournamentAwardsResponse.TeamOfTournamentPlayer> selected =
                new ArrayList<>();
        Set<Long> selectedPlayerIds =
                new java.util.HashSet<>();

        for (TeamSlot slot : teamSlots()) {
            PlayerTournamentStat player =
                    selectForSlot(
                            candidates,
                            selectedPlayerIds,
                            slot
                    );

            selectedPlayerIds.add(player.player.getId());
            selected.add(
                    new TournamentAwardsResponse.TeamOfTournamentPlayer(
                            player.player.getId(),
                            player.player.getName(),
                            teamName(player),
                            player.player.getPosition().name(),
                            slot.name(),
                            round(player.averageRating())
                    )
            );
        }

        return selected;
    }

    private PlayerTournamentStat selectForSlot(
            List<PlayerTournamentStat> candidates,
            Set<Long> selectedPlayerIds,
            TeamSlot slot
    ) {

        return candidates.stream()
                .filter(notAlreadySelected(selectedPlayerIds))
                .filter(stat -> slot.preferredPositions()
                        .contains(stat.player.getPosition()))
                .findFirst()
                .or(() -> candidates.stream()
                        .filter(notAlreadySelected(selectedPlayerIds))
                        .filter(stat -> slot.compatiblePositions()
                                .contains(stat.player.getPosition()))
                        .findFirst())
                .or(() -> candidates.stream()
                        .filter(notAlreadySelected(selectedPlayerIds))
                        .findFirst())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unable to select Team of the Tournament"
                ));
    }

    private Predicate<PlayerTournamentStat> notAlreadySelected(
            Set<Long> selectedPlayerIds
    ) {

        return stat -> !selectedPlayerIds.contains(stat.player.getId());
    }

    private Comparator<PlayerTournamentStat> playerRankingComparator() {

        return Comparator
                .comparingDouble(PlayerTournamentStat::averageRating)
                .reversed()
                .thenComparing(stat -> stat.player.getId());
    }

    private List<TeamSlot> teamSlots() {

        Set<PlayerPosition> defenders =
                EnumSet.of(
                        PlayerPosition.LB,
                        PlayerPosition.CB,
                        PlayerPosition.RB
                );

        Set<PlayerPosition> midfielders =
                EnumSet.of(
                        PlayerPosition.CDM,
                        PlayerPosition.CM,
                        PlayerPosition.CAM
                );

        Set<PlayerPosition> forwards =
                EnumSet.of(
                        PlayerPosition.LW,
                        PlayerPosition.ST,
                        PlayerPosition.RW
                );

        return List.of(
                new TeamSlot(
                        "GK",
                        EnumSet.of(PlayerPosition.GK),
                        EnumSet.of(PlayerPosition.GK)
                ),
                new TeamSlot(
                        "LB",
                        EnumSet.of(PlayerPosition.LB),
                        defenders
                ),
                new TeamSlot(
                        "CB",
                        EnumSet.of(PlayerPosition.CB),
                        defenders
                ),
                new TeamSlot(
                        "CB",
                        EnumSet.of(PlayerPosition.CB),
                        defenders
                ),
                new TeamSlot(
                        "RB",
                        EnumSet.of(PlayerPosition.RB),
                        defenders
                ),
                new TeamSlot(
                        "CM",
                        midfielders,
                        midfielders
                ),
                new TeamSlot(
                        "CM",
                        midfielders,
                        midfielders
                ),
                new TeamSlot(
                        "CM",
                        midfielders,
                        midfielders
                ),
                new TeamSlot(
                        "LW",
                        EnumSet.of(PlayerPosition.LW),
                        forwards
                ),
                new TeamSlot(
                        "ST",
                        EnumSet.of(PlayerPosition.ST),
                        forwards
                ),
                new TeamSlot(
                        "RW",
                        EnumSet.of(PlayerPosition.RW),
                        forwards
                )
        );
    }

    private String teamName(PlayerTournamentStat stat) {

        return stat.team == null ? null : stat.team.getName();
    }

    private double round(double value) {

        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private record TeamSlot(
            String name,
            Set<PlayerPosition> preferredPositions,
            Set<PlayerPosition> compatiblePositions
    ) {
    }

    private static class PlayerTournamentStat {

        private final Player player;
        private final Team team;
        private int goals;
        private int assists;
        private int matchesPlayed;
        private int cleanSheets;
        private int goalsConceded;
        private double totalRating;

        private PlayerTournamentStat(
                Player player,
                Team team
        ) {
            this.player = player;
            this.team = team;
        }

        private int goals() {

            return goals;
        }

        private int assists() {

            return assists;
        }

        private double averageRating() {

            if (matchesPlayed == 0) {
                return 0.0;
            }

            return totalRating / matchesPlayed;
        }
    }
}
