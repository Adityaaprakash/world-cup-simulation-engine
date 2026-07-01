package com.aditya.worldcup.simulation.service;

import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.simulation.dto.ManOfTheMatchResponse;
import com.aditya.worldcup.simulation.dto.PlayerMatchRatingResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ManOfTheMatchService {

    public ManOfTheMatchResponse determine(
            List<PlayerMatchRatingResponse> playerRatings,
            List<MatchEventResponse> events,
            String winner
    ) {

        if (playerRatings == null || playerRatings.isEmpty()) {
            throw new IllegalArgumentException(
                    "Player ratings are required to determine Man of the Match"
            );
        }

        Map<String, Long> goalCounts =
                countGoalsByPlayer(events);

        Map<String, Long> assistCounts =
                countEventsByPlayer(
                        events,
                        MatchEventType.ASSIST
                );

        PlayerMatchRatingResponse selected =
                playerRatings.stream()
                        .max(Comparator
                                .comparing(
                                        PlayerMatchRatingResponse::rating
                                )
                                .thenComparing(player ->
                                        goalCounts.getOrDefault(
                                                player.playerName(),
                                                0L
                                        ))
                                .thenComparing(player ->
                                        assistCounts.getOrDefault(
                                                player.playerName(),
                                                0L
                                        ))
                                .thenComparing(player ->
                                        isWinningTeamPlayer(
                                                player,
                                                winner
                                        ) ? 1 : 0)
                                .thenComparing(player ->
                                        -player.playerId()))
                        .orElseThrow();

        return new ManOfTheMatchResponse(
                selected.playerId(),
                selected.playerName(),
                selected.team(),
                selected.position(),
                selected.rating()
        );
    }

    private Map<String, Long> countEventsByPlayer(
            List<MatchEventResponse> events,
            MatchEventType eventType
    ) {

        return events.stream()
                .filter(event -> eventType.name().equals(event.eventType()))
                .collect(Collectors.groupingBy(
                        MatchEventResponse::player,
                        Collectors.counting()
                ));
    }

    private Map<String, Long> countGoalsByPlayer(
            List<MatchEventResponse> events
    ) {

        return events.stream()
                .filter(event -> MatchEventType.GOAL.name()
                        .equals(event.eventType())
                        || isConvertedPenalty(event))
                .collect(Collectors.groupingBy(
                        MatchEventResponse::player,
                        Collectors.counting()
                ));
    }

    private boolean isConvertedPenalty(MatchEventResponse event) {

        return MatchEventType.PENALTY.name().equals(event.eventType())
                && event.description() != null
                && event.description().toLowerCase().contains("converts");
    }

    private boolean isWinningTeamPlayer(
            PlayerMatchRatingResponse player,
            String winner
    ) {

        return winner != null
                && !"DRAW".equals(winner)
                && winner.equals(player.team());
    }
}
