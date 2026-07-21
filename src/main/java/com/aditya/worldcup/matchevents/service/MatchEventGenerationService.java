package com.aditya.worldcup.matchevents.service;

import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.service.PlayerStateService;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.tactics.service.TacticalMatchModifiers;
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
    private final PlayerStateService playerStateService;

    private final Random random = new Random();

    public List<MatchEventResponse> generateGoalEvents(
            Long homeSquadId,
            Long awaySquadId,
            int homeGoals,
            int awayGoals
    ) {
        return generateMatchEvents(
                homeSquadId,
                awaySquadId,
                homeGoals,
                awayGoals
        );
    }

    public List<MatchEventResponse> generateMatchEvents(
            Long homeSquadId,
            Long awaySquadId,
            int homeGoals,
            int awayGoals
    ) {
        return generateMatchEvents(homeSquadId, awaySquadId, homeGoals, awayGoals,
                TacticalMatchModifiers.balanced(), TacticalMatchModifiers.balanced());
    }

    public List<MatchEventResponse> generateMatchEvents(
            Long homeSquadId,
            Long awaySquadId,
            int homeGoals,
            int awayGoals,
            TacticalMatchModifiers homeTactics,
            TacticalMatchModifiers awayTactics
    ) {

        List<MatchEventResponse> events = new ArrayList<>();

        List<SquadPlayer> homePlayers =
                squadPlayerRepository
                        .findBySquadIdAndStartingXiTrue(homeSquadId)
                        .stream()
                        .filter(this::isAvailable)
                        .toList();

        List<SquadPlayer> awayPlayers =
                squadPlayerRepository
                        .findBySquadIdAndStartingXiTrue(awaySquadId)
                        .stream()
                        .filter(this::isAvailable)
                        .toList();

        List<SquadPlayer> homeSquadPlayers =
                squadPlayerRepository.findBySquadId(homeSquadId)
                        .stream().filter(this::isAvailable).toList();

        List<SquadPlayer> awaySquadPlayers =
                squadPlayerRepository.findBySquadId(awaySquadId)
                        .stream().filter(this::isAvailable).toList();

        validateStartingXi(homePlayers, homeSquadId);
        validateStartingXi(awayPlayers, awaySquadId);

        for (int i = 0; i < homeGoals; i++) {

            SquadPlayer scorer =
                    chooseRandom(homePlayers);

            int minute =
                    randomMatchMinute();

            events.add(
                    createGoalEvent(
                            scorer.getPlayer(),
                            minute
                    )
            );

            addAssistEvent(
                    events,
                    homePlayers,
                    scorer,
                    minute,
                    homeTactics
            );
        }

        for (int i = 0; i < awayGoals; i++) {

            SquadPlayer scorer =
                    chooseRandom(awayPlayers);

            int minute =
                    randomMatchMinute();

            events.add(
                    createGoalEvent(
                            scorer.getPlayer(),
                            minute
                    )
            );

            addAssistEvent(
                    events,
                    awayPlayers,
                    scorer,
                    minute,
                    awayTactics
            );
        }

        addYellowCards(
                events,
                homeSquadPlayers,
                awaySquadPlayers,
                homeTactics,
                awayTactics
        );

        addRedCard(
                events,
                homeSquadPlayers,
                awaySquadPlayers,
                homeTactics,
                awayTactics
        );

        addPenalty(
                events,
                homeSquadPlayers,
                awaySquadPlayers
        );

        addOwnGoal(
                events,
                homePlayers,
                awayPlayers
        );

        addSubstitutions(
                events,
                homeSquadPlayers
        );

        addSubstitutions(
                events,
                awaySquadPlayers
        );

        events.sort(
                Comparator.comparing(
                        MatchEventResponse::minute
                )
        );

        return events;
    }

    private void validateStartingXi(
            List<SquadPlayer> startingXi,
            Long squadId
    ) {

        if (startingXi.isEmpty()) {
            throw new IllegalArgumentException(
                    "No starting XI players found for squad: " + squadId
            );
        }
    }

    private boolean isAvailable(SquadPlayer squadPlayer) {
        return playerStateService.isAvailable(
                playerStateService.getOrCreateState(squadPlayer.getPlayer())
        );
    }

    private void addAssistEvent(
            List<MatchEventResponse> events,
            List<SquadPlayer> teammates,
            SquadPlayer scorer,
            int minute,
            TacticalMatchModifiers tactics
    ) {

        int assistChance = 75 + (int) Math.round(
                (tactics.crossingModifier() + tactics.attackModifier()) * 8);
        if (teammates.size() < 2 || random.nextInt(100) >= assistChance) {
            return;
        }

        List<SquadPlayer> candidates =
                teammates.stream()
                        .filter(player -> !player.getPlayer().getId()
                                .equals(scorer.getPlayer().getId()))
                        .toList();

        if (candidates.isEmpty()) {
            return;
        }

        Player assister =
                chooseRandom(candidates).getPlayer();

        String description = tactics.crossingModifier() > 0
                && random.nextInt(100) < 55
                ? assister.getName() + " delivers a cross for the assist."
                : assister.getName() + " provides the assist.";

        events.add(
                new MatchEventResponse(
                        minute,
                        assister.getName(),
                        MatchEventType.ASSIST.name(),
                        description
                )
        );
    }

    private void addYellowCards(
            List<MatchEventResponse> events,
            List<SquadPlayer> homeSquadPlayers,
            List<SquadPlayer> awaySquadPlayers,
            TacticalMatchModifiers homeTactics,
            TacticalMatchModifiers awayTactics
    ) {

        List<SquadPlayer> players =
                combinePlayers(homeSquadPlayers, awaySquadPlayers);

        if (players.isEmpty()) {
            return;
        }

        int yellowCards = random.nextInt(6)
                + disciplineIncrease(homeTactics)
                + disciplineIncrease(awayTactics);

        for (int i = 0; i < yellowCards; i++) {

            Player player =
                    chooseRandom(players).getPlayer();

            events.add(
                    new MatchEventResponse(
                            randomMatchMinute(),
                            player.getName(),
                            MatchEventType.YELLOW_CARD.name(),
                            player.getName() + " receives a yellow card."
                    )
            );
        }
    }

    private void addRedCard(
            List<MatchEventResponse> events,
            List<SquadPlayer> homeSquadPlayers,
            List<SquadPlayer> awaySquadPlayers,
            TacticalMatchModifiers homeTactics,
            TacticalMatchModifiers awayTactics
    ) {

        int redCardChance = 12 + disciplineIncrease(homeTactics) * 2
                + disciplineIncrease(awayTactics) * 2;
        if (random.nextInt(100) >= redCardChance) {
            return;
        }

        List<SquadPlayer> players =
                combinePlayers(homeSquadPlayers, awaySquadPlayers);

        if (players.isEmpty()) {
            return;
        }

        Player player =
                chooseRandom(players).getPlayer();

        events.add(
                new MatchEventResponse(
                        randomMatchMinute(),
                        player.getName(),
                        MatchEventType.RED_CARD.name(),
                        player.getName() + " is sent off."
                )
        );
    }

    private void addPenalty(
            List<MatchEventResponse> events,
            List<SquadPlayer> homeSquadPlayers,
            List<SquadPlayer> awaySquadPlayers
    ) {

        if (random.nextInt(100) >= 25) {
            return;
        }

        List<SquadPlayer> players =
                combinePlayers(homeSquadPlayers, awaySquadPlayers);

        if (players.isEmpty()) {
            return;
        }

        Player player =
                chooseRandom(players).getPlayer();

        String description =
                random.nextBoolean()
                        ? player.getName() + " converts from the penalty spot."
                        : player.getName() + " misses from the penalty spot.";

        events.add(
                new MatchEventResponse(
                        randomMatchMinute(),
                        player.getName(),
                        MatchEventType.PENALTY.name(),
                        description
                )
        );
    }

    private void addOwnGoal(
            List<MatchEventResponse> events,
            List<SquadPlayer> homePlayers,
            List<SquadPlayer> awayPlayers
    ) {

        if (random.nextInt(100) >= 5) {
            return;
        }

        List<SquadPlayer> players =
                combinePlayers(homePlayers, awayPlayers);

        Player player =
                chooseRandom(players).getPlayer();

        events.add(
                new MatchEventResponse(
                        randomMatchMinute(),
                        player.getName(),
                        MatchEventType.OWN_GOAL.name(),
                        player.getName()
                                + " scores an unfortunate own goal."
                )
        );
    }

    private void addSubstitutions(
            List<MatchEventResponse> events,
            List<SquadPlayer> squadPlayers
    ) {

        List<SquadPlayer> starters =
                squadPlayers.stream()
                        .filter(SquadPlayer::getStartingXi)
                        .toList();

        List<SquadPlayer> bench =
                squadPlayers.stream()
                        .filter(player -> !player.getStartingXi())
                        .toList();

        int substitutionCount =
                Math.min(
                        random.nextInt(3) + 3,
                        Math.min(starters.size(), bench.size())
                );

        List<SquadPlayer> availableStarters =
                new ArrayList<>(starters);

        List<SquadPlayer> availableBench =
                new ArrayList<>(bench);

        for (int i = 0; i < substitutionCount; i++) {

            SquadPlayer playerOff =
                    removeRandom(availableStarters);

            SquadPlayer playerOn =
                    removeRandom(availableBench);

            events.add(
                    new MatchEventResponse(
                            randomSubstitutionMinute(),
                            playerOn.getPlayer().getName(),
                            MatchEventType.SUBSTITUTION.name(),
                            playerOn.getPlayer().getName()
                                    + " replaces "
                                    + playerOff.getPlayer().getName()
                                    + "."
                    )
            );
        }
    }

    private List<SquadPlayer> combinePlayers(
            List<SquadPlayer> homePlayers,
            List<SquadPlayer> awayPlayers
    ) {

        List<SquadPlayer> players = new ArrayList<>();
        players.addAll(homePlayers);
        players.addAll(awayPlayers);

        return players;
    }

    private SquadPlayer chooseRandom(
            List<SquadPlayer> players
    ) {

        return players.get(
                random.nextInt(players.size())
        );
    }

    private SquadPlayer removeRandom(
            List<SquadPlayer> players
    ) {

        return players.remove(
                random.nextInt(players.size())
        );
    }

    private MatchEventResponse createGoalEvent(
            Player player,
            int minute
    ) {

        return new MatchEventResponse(
                minute,
                player.getName(),
                MatchEventType.GOAL.name(),
                player.getName() + " scores from inside the box."
        );
    }

    private int randomMatchMinute() {
        return random.nextInt(90) + 1;
    }

    private int randomSubstitutionMinute() {
        return random.nextInt(36) + 55;
    }

    private int disciplineIncrease(TacticalMatchModifiers tactics) {
        return Math.max(0, (int) Math.round(tactics.disciplineModifier() * 2));
    }
}
