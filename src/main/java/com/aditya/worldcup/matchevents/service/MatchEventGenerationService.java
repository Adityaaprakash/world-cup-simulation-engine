package com.aditya.worldcup.matchevents.service;

import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class MatchEventGenerationService {

    private final SquadPlayerRepository squadPlayerRepository;

    private final Random random = new Random();

    public List<MatchEventResponse> generateGoalEvents(
            Long homeSquadId,
            Long awaySquadId,
            int homeGoals,
            int awayGoals
    ) {

        List<MatchEventResponse> events = new ArrayList<>();

        List<SquadPlayer> homePlayers =
                squadPlayerRepository
                        .findBySquadIdAndStartingXiTrue(homeSquadId);

        List<SquadPlayer> awayPlayers =
                squadPlayerRepository
                        .findBySquadIdAndStartingXiTrue(awaySquadId);

        for (int i = 0; i < homeGoals; i++) {

            SquadPlayer scorer =
                    chooseScorer(homePlayers, homeSquadId);

            events.add(
                    createGoalEvent(
                            scorer.getPlayer()
                    )
            );
        }

        for (int i = 0; i < awayGoals; i++) {

            SquadPlayer scorer =
                    chooseScorer(awayPlayers, awaySquadId);

            events.add(
                    createGoalEvent(
                            scorer.getPlayer()
                    )
            );
        }

        events.sort(
                Comparator.comparing(
                        MatchEventResponse::minute
                )
        );

        return events;
    }

    private SquadPlayer chooseScorer(
            List<SquadPlayer> players,
            Long squadId
    ) {

        if (players.isEmpty()) {
            throw new IllegalArgumentException(
                    "No starting XI players found for squad: " + squadId
            );
        }

        return players.get(
                random.nextInt(players.size())
        );
    }

    private MatchEventResponse createGoalEvent(
            Player player
    ) {

        int minute =
                random.nextInt(90) + 1;

        return new MatchEventResponse(
                minute,
                player.getName(),
                MatchEventType.GOAL.name(),
                player.getName() + " scored"
        );
    }
}
