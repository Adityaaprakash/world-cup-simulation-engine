package com.aditya.worldcup.players.service;

import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.repository.PlayerStateRepository;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PlayerStateServiceTest {

    private final PlayerStateRepository playerStateRepository = mock(PlayerStateRepository.class);
    private final SquadPlayerRepository squadPlayerRepository = mock(SquadPlayerRepository.class);
    private final PlayerStateService service = new PlayerStateService(
            playerStateRepository, squadPlayerRepository);

    @Test
    void updateAfterMatchIncreasesFatigueForFullMatchParticipants() {
        Player player = player(1L, 80);
        PlayerState state = PlayerState.builder().player(player).fatigue(0).fitness(100).build();
        SquadPlayer starter = squadPlayer(player, true);

        when(squadPlayerRepository.findBySquadId(10L)).thenReturn(List.of(starter));
        when(squadPlayerRepository.findBySquadId(20L)).thenReturn(List.of());
        when(playerStateRepository.findByPlayerId(1L)).thenReturn(java.util.Optional.of(state));

        // 90 minutes mapped dynamically across 
        service.updateAfterMatch(10L, 20L, 1, 0, List.of());

        assertThat(state.getFatigue()).isEqualTo(8);
        assertThat(state.getFitness()).isEqualTo(95);
    }

    @Test
    void partialMatchParticipantAccumulatesProportionalFatigue() {
        Player starterPlayer = player(1L, 80);
        Player subPlayer = player(2L, 80);
        
        PlayerState starterState = PlayerState.builder().player(starterPlayer).build();
        PlayerState subState = PlayerState.builder().player(subPlayer).build();
        
        SquadPlayer starter = squadPlayer(starterPlayer, true);
        SquadPlayer sub = squadPlayer(subPlayer, false);

        when(squadPlayerRepository.findBySquadId(10L)).thenReturn(List.of(starter, sub));
        when(squadPlayerRepository.findBySquadId(20L)).thenReturn(List.of());
        when(playerStateRepository.findByPlayerId(1L)).thenReturn(java.util.Optional.of(starterState));
        when(playerStateRepository.findByPlayerId(2L)).thenReturn(java.util.Optional.of(subState));

        MatchEventResponse substitution = new MatchEventResponse(
                45, "Player 2", MatchEventType.SUBSTITUTION.name(), "Player 2 replaces Player 1.");

        service.updateAfterMatch(10L, 20L, 1, 0, List.of(substitution));

        assertThat(starterState.getFatigue()).isEqualTo(4); // 45 mins = half of 8
        assertThat(subState.getFatigue()).isEqualTo(4); // 45 mins = half of 8
    }

    @Test
    void unusedBenchPlayerLogsZeroFatigueAndIncreasesFitness() {
        Player player = player(1L, 80);
        PlayerState state = PlayerState.builder().player(player).fatigue(10).fitness(90).build();
        SquadPlayer bench = squadPlayer(player, false);

        when(squadPlayerRepository.findBySquadId(10L)).thenReturn(List.of(bench));
        when(squadPlayerRepository.findBySquadId(20L)).thenReturn(List.of());
        when(playerStateRepository.findByPlayerId(1L)).thenReturn(java.util.Optional.of(state));

        service.updateAfterMatch(10L, 20L, 1, 0, List.of());

        assertThat(state.getFatigue()).isEqualTo(5); // rested -> fatige drops
        assertThat(state.getFitness()).isEqualTo(94); // rested -> fitness rises
    }

    @Test
    void formBoundedToMaxAndMinValues() {
        Player playerP = player(1L, 80);
        PlayerState ceiling = PlayerState.builder().player(playerP).currentForm(10).build();
        SquadPlayer starter1 = squadPlayer(playerP, true);
        
        Player playerN = player(2L, 80);
        PlayerState floor = PlayerState.builder().player(playerN).currentForm(-10).build();
        SquadPlayer starter2 = squadPlayer(playerN, true);

        when(squadPlayerRepository.findBySquadId(10L)).thenReturn(List.of(starter1));
        when(squadPlayerRepository.findBySquadId(20L)).thenReturn(List.of(starter2));
        
        when(playerStateRepository.findByPlayerId(1L)).thenReturn(java.util.Optional.of(ceiling));
        when(playerStateRepository.findByPlayerId(2L)).thenReturn(java.util.Optional.of(floor));

        // Team 1 wins, adding form. Ceiling is reached. Team 2 loses, dropping form. Floor is reached.
        service.updateAfterMatch(10L, 20L, 2, 0, List.of());

        assertThat(ceiling.getCurrentForm()).isEqualTo(10);
        assertThat(floor.getCurrentForm()).isEqualTo(-10);
    }
    
    @Test
    void formUpdatesPositivelyOnGoalAndAssist() {
        Player player = player(1L, 80);
        PlayerState state = PlayerState.builder().player(player).currentForm(0).build();
        SquadPlayer starter = squadPlayer(player, true);

        when(squadPlayerRepository.findBySquadId(10L)).thenReturn(List.of(starter));
        when(squadPlayerRepository.findBySquadId(20L)).thenReturn(List.of());
        when(playerStateRepository.findByPlayerId(1L)).thenReturn(java.util.Optional.of(state));

        MatchEventResponse goal = new MatchEventResponse(
                45, "Player 1", MatchEventType.GOAL.name(), "Goal.");

        service.updateAfterMatch(10L, 20L, 1, 0, List.of(goal));

        // +1 from win, +2 from Goal = 3
        assertThat(state.getCurrentForm()).isEqualTo(3); 
    }

    @Test
    void inactivePlayersRecoverFitnessAndFatigue() {
        Player player = player(1L, 80);
        PlayerState state = PlayerState.builder().player(player).fitness(90).fatigue(10).build();

        service.recoverInactivePlayers(List.of(squadPlayer(player, false)),
                Map.of(1L, state), Set.of());

        assertThat(state.getFitness()).isEqualTo(94);
        assertThat(state.getFatigue()).isEqualTo(5);
    }

    @Test
    void suspensionDecreasesAfterTheNextMatchday() {
        PlayerState state = PlayerState.builder().redCardSuspension(1).build();

        service.processSuspensions(Map.of(1L, state), Set.of(1L));

        assertThat(state.getRedCardSuspension()).isZero();
    }

    @Test
    void formDecaysTowardZero() {
        PlayerState positive = PlayerState.builder().currentForm(7).build();
        PlayerState negative = PlayerState.builder().currentForm(-5).build();

        service.decayForm(List.of(positive, negative));

        assertThat(positive.getCurrentForm()).isEqualTo(6);
        assertThat(negative.getCurrentForm()).isEqualTo(-4);
    }

    @Test
    void effectiveRatingUsesStateWithoutChangingBaseRating() {
        Player player = player(1L, 80);
        PlayerState state = PlayerState.builder()
                .currentForm(10).confidence(100).fitness(100).fatigue(0).morale(100).build();
        PlayerEffectiveRatingService ratingService = new PlayerEffectiveRatingService(service);

        int effectiveRating = ratingService.calculate(player, state);

        assertThat(effectiveRating).isGreaterThan(80);
        assertThat(player.getOverallRating()).isEqualTo(80);
    }

    private Player player(Long id, int rating) {
        Player player = new Player();
        player.setId(id);
        player.setOverallRating(rating);
        player.setName("Player " + id);
        return player;
    }

    private SquadPlayer squadPlayer(Player player, boolean startingXi) {
        SquadPlayer squadPlayer = new SquadPlayer();
        squadPlayer.setPlayer(player);
        squadPlayer.setStartingXi(startingXi);
        squadPlayer.setPositionSlot("CM");
        return squadPlayer;
    }
}
