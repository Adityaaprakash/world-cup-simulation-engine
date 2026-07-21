package com.aditya.worldcup.players.service;

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
    void updateAfterMatchIncreasesFatigueForPlayersWhoPlayed() {
        Player player = player(1L, 80);
        PlayerState state = PlayerState.builder().player(player).build();
        SquadPlayer starter = squadPlayer(player, true);

        when(squadPlayerRepository.findBySquadId(10L)).thenReturn(List.of(starter));
        when(squadPlayerRepository.findBySquadId(20L)).thenReturn(List.of());
        when(playerStateRepository.findByPlayerId(1L)).thenReturn(java.util.Optional.of(state));

        service.updateAfterMatch(10L, 20L, 1, 0, List.of());

        assertThat(state.getFatigue()).isEqualTo(8);
        assertThat(state.getFitness()).isEqualTo(95);
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
