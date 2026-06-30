package com.aditya.worldcup.simulation.service;

import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.simulation.dto.MatchStatisticsResponse;
import com.aditya.worldcup.simulation.dto.PlayerMatchRatingResponse;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlayerRatingGenerationService {

    private static final double BASE_RATING = 6.5;
    private static final double MINIMUM_RATING = 5.0;
    private static final double MAXIMUM_RATING = 10.0;

    private final SquadPlayerRepository squadPlayerRepository;

    public List<PlayerMatchRatingResponse> generate(
            Long homeSquadId,
            Long awaySquadId,
            String homeTeam,
            String awayTeam,
            int homeGoals,
            int awayGoals,
            List<MatchEventResponse> events,
            MatchStatisticsResponse statistics
    ) {

        List<SquadPlayer> homeStartingXi =
                squadPlayerRepository.findBySquadIdAndStartingXiTrue(
                        homeSquadId
                );

        List<SquadPlayer> awayStartingXi =
                squadPlayerRepository.findBySquadIdAndStartingXiTrue(
                        awaySquadId
                );

        Map<Long, RatingState> ratings = new LinkedHashMap<>();
        Map<String, RatingState> ratingsByPlayerName = new LinkedHashMap<>();

        addStartingXi(
                ratings,
                ratingsByPlayerName,
                homeStartingXi,
                homeTeam,
                homeGoals,
                awayGoals,
                statistics.homeTeam().saves()
        );

        addStartingXi(
                ratings,
                ratingsByPlayerName,
                awayStartingXi,
                awayTeam,
                awayGoals,
                homeGoals,
                statistics.awayTeam().saves()
        );

        applyEventRatings(
                ratingsByPlayerName,
                events
        );

        return ratings.values()
                .stream()
                .map(RatingState::toResponse)
                .sorted(Comparator
                        .comparing(PlayerMatchRatingResponse::rating)
                        .reversed()
                        .thenComparing(PlayerMatchRatingResponse::playerName))
                .toList();
    }

    private void addStartingXi(
            Map<Long, RatingState> ratings,
            Map<String, RatingState> ratingsByPlayerName,
            List<SquadPlayer> startingXi,
            String team,
            int teamGoals,
            int opponentGoals,
            int saves
    ) {

        for (SquadPlayer squadPlayer : startingXi) {

            Player player = squadPlayer.getPlayer();
            RatingState rating =
                    new RatingState(
                            player.getId(),
                            player.getName(),
                            player.getPosition(),
                            team
                    );

            applyResultModifier(
                    rating,
                    teamGoals,
                    opponentGoals
            );

            applyPositionModifier(
                    rating,
                    player.getPosition(),
                    opponentGoals,
                    saves
            );

            ratings.put(
                    player.getId(),
                    rating
            );

            ratingsByPlayerName.putIfAbsent(
                    player.getName(),
                    rating
            );
        }
    }

    private void applyEventRatings(
            Map<String, RatingState> ratings,
            List<MatchEventResponse> events
    ) {

        for (MatchEventResponse event : events) {

            RatingState rating =
                    ratings.get(event.player());

            if (rating == null) {
                continue;
            }

            MatchEventType eventType =
                    MatchEventType.valueOf(event.eventType());

            switch (eventType) {
                case GOAL -> rating.adjust(1.0);
                case ASSIST -> rating.adjust(0.7);
                case YELLOW_CARD -> rating.adjust(-0.3);
                case RED_CARD -> rating.adjust(-1.0);
                case OWN_GOAL -> rating.adjust(-1.0);
                case PENALTY -> applyPenaltyRating(
                        rating,
                        event.description()
                );
                default -> {
                }
            }
        }
    }

    private void applyResultModifier(
            RatingState rating,
            int teamGoals,
            int opponentGoals
    ) {

        if (teamGoals > opponentGoals) {
            rating.adjust(0.3);
        } else if (teamGoals < opponentGoals) {
            rating.adjust(-0.2);
        }
    }

    private void applyPositionModifier(
            RatingState rating,
            PlayerPosition position,
            int opponentGoals,
            int saves
    ) {

        if (position == PlayerPosition.GK) {
            if (opponentGoals == 0) {
                rating.adjust(0.8);
            }

            rating.adjust((saves / 3) * 0.2);
            return;
        }

        if (isDefender(position) && opponentGoals == 0) {
            rating.adjust(0.5);
        }
    }

    private void applyPenaltyRating(
            RatingState rating,
            String description
    ) {

        if (description != null
                && description.toLowerCase().contains("converts")) {
            rating.adjust(0.5);
        }
    }

    private boolean isDefender(PlayerPosition position) {

        return position == PlayerPosition.RB
                || position == PlayerPosition.CB
                || position == PlayerPosition.LB;
    }

    private double roundRating(double rating) {

        double clamped =
                Math.max(
                        MINIMUM_RATING,
                        Math.min(MAXIMUM_RATING, rating)
                );

        return BigDecimal.valueOf(clamped)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private class RatingState {

        private final Long playerId;
        private final String playerName;
        private final PlayerPosition position;
        private final String team;
        private double rating = BASE_RATING;

        private RatingState(
                Long playerId,
                String playerName,
                PlayerPosition position,
                String team
        ) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.position = position;
            this.team = team;
        }

        private void adjust(double modifier) {

            rating += modifier;
        }

        private PlayerMatchRatingResponse toResponse() {

            return new PlayerMatchRatingResponse(
                    playerId,
                    playerName,
                    position.name(),
                    team,
                    roundRating(rating)
            );
        }
    }
}
