package com.aditya.worldcup.simulation.service;

import com.aditya.worldcup.ai.service.*;
import com.aditya.worldcup.formations.entity.Formation;
import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.players.entity.InjuryStatus;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.repository.PlayerStateRepository;
import com.aditya.worldcup.players.service.PlayerEffectiveRatingService;
import com.aditya.worldcup.players.service.PlayerStateService;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.teams.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Phase10EIntegrationTest {

    @Mock
    private PlayerStateRepository playerStateRepository;
    
    @Mock
    private SquadPlayerRepository squadPlayerRepository;

    private PlayerStateService playerStateService;
    private PlayerEffectiveRatingService playerEffectiveRatingService;
    private TeamStrengthService teamStrengthService;
    private RotationService rotationService;
    private PlayerEvaluationService playerEvaluationService;
    private LineupSelectionService lineupSelectionService;
    private SubstitutionDecisionService substitutionDecisionService;

    private Squad squad;
    private List<SquadPlayer> squadPlayers;

    @BeforeEach
    void setUp() {
        playerStateService = new PlayerStateService(playerStateRepository, squadPlayerRepository);
        playerEffectiveRatingService = new PlayerEffectiveRatingService(playerStateService);
        teamStrengthService = new TeamStrengthService(squadPlayerRepository, playerEffectiveRatingService);
        rotationService = new RotationService(playerStateService);
        playerEvaluationService = new PlayerEvaluationService(playerEffectiveRatingService, playerStateService);
        lineupSelectionService = new LineupSelectionService(squadPlayerRepository, rotationService, playerEvaluationService);
        substitutionDecisionService = new SubstitutionDecisionService(lineupSelectionService, rotationService, playerEvaluationService, playerStateService);

        squad = new Squad();
        squad.setId(10L);
        Team team = new Team();
        team.setOverallRating(80);
        squad.setTeam(team);
        squad.setFormation(new Formation());

        squadPlayers = new ArrayList<>();
        // Create 15 players (11 starters, 4 bench)
        for (long i = 1; i <= 15; i++) {
            Player p = new Player();
            p.setId(i);
            p.setName("Player " + i);
            p.setOverallRating(80);
            p.setAge(25);
            p.setPosition(i == 1 ? PlayerPosition.GK : (i <= 5 ? PlayerPosition.CB : (i <= 10 ? PlayerPosition.CM : PlayerPosition.ST)));

            SquadPlayer sp = new SquadPlayer();
            sp.setPlayer(p);
            sp.setSquad(squad);
            sp.setStartingXi(i <= 11);
            sp.setPositionSlot(i == 1 ? "GK" : (i <= 5 ? "CB" : (i <= 10 ? "CM" : "ST")));

            squadPlayers.add(sp);

            // Default state
            PlayerState state = PlayerState.builder().player(p)
                    .fatigue(0).fitness(100).currentForm(0).morale(50).confidence(50)
                    .developmentRating(0).injuryStatus(InjuryStatus.HEALTHY).build();

            lenient().when(playerStateRepository.findByPlayerId(i)).thenReturn(Optional.of(state));
        }

        lenient().when(squadPlayerRepository.findBySquadId(10L)).thenReturn(squadPlayers);
        lenient().when(squadPlayerRepository.findBySquadIdAndStartingXiTrue(10L)).thenAnswer(inv -> 
            squadPlayers.stream().filter(SquadPlayer::getStartingXi).toList()
        );
    }

    @Test
    void test1_effectiveRatingUsesFormFatigueAndDevelopment() {
        Player player = squadPlayers.get(0).getPlayer();
        PlayerState state = playerStateRepository.findByPlayerId(player.getId()).get();

        int normalRating = playerEffectiveRatingService.calculate(player, state);

        state.setCurrentForm(10);
        int highFormRating = playerEffectiveRatingService.calculate(player, state);
        assertThat(highFormRating).isGreaterThan(normalRating);

        state.setCurrentForm(0);
        state.setFatigue(100);
        int highFatigueRating = playerEffectiveRatingService.calculate(player, state);
        assertThat(highFatigueRating).isLessThan(normalRating);

        state.setFatigue(0);
        state.setDevelopmentRating(5);
        int developedRating = playerEffectiveRatingService.calculate(player, state);
        assertThat(developedRating).isGreaterThan(normalRating);
    }

    @Test
    void test2_teamStrengthReflectsEffectiveRatings() {
        int normalStrength = teamStrengthService.calculateStrength(10L).overall();

        // Fatigued team
        for (SquadPlayer sp : squadPlayers) {
            PlayerState state = playerStateRepository.findByPlayerId(sp.getPlayer().getId()).get();
            state.setFatigue(50);
        }
        
        int fatiguedStrength = teamStrengthService.calculateStrength(10L).overall();
        assertThat(fatiguedStrength).isLessThan(normalStrength);
    }

    @Test
    void test3_injuredPlayersExcludedFromStartingXI() {
        // Player 2 is a CB
        Player injuredPlayer = squadPlayers.get(1).getPlayer();
        PlayerState injuredState = playerStateRepository.findByPlayerId(injuredPlayer.getId()).get();
        injuredState.setInjuryStatus(InjuryStatus.MODERATE);
        injuredState.setInjuryMatchesRemaining(2);

        Formation formation = new Formation();
        formation.setDefenders(4);
        formation.setMidfielders(4);
        formation.setAttackers(2);

        lineupSelectionService.selectStartingXi(squad, formation);

        // Verify injured player was not selected
        SquadPlayer player2 = squadPlayers.stream().filter(sp -> sp.getPlayer().getId() == 2L).findFirst().get();
        assertThat(player2.getStartingXi()).isFalse();
    }

    @Test
    void test4_injuryDuringMatchUpdatesPlayerState() {
        List<MatchEventResponse> events = List.of(
            new MatchEventResponse(15, "Player 3", MatchEventType.INJURY.name(), "Player 3 sustains a MINOR injury.")
        );

        playerStateService.updateAfterMatch(10L, 99L, 1, 0, events);

        PlayerState state = playerStateRepository.findByPlayerId(3L).get();
        assertThat(state.getInjuryStatus()).isEqualTo(InjuryStatus.MINOR);
        assertThat(state.getInjuryMatchesRemaining()).isEqualTo(1);
    }

    @Test
    void test5_substitutionLogicRespectsInjuries() {
        // Starters include Player 1 to 11
        List<MatchEventResponse> events = List.of(
            new MatchEventResponse(10, "Player 4", MatchEventType.INJURY.name(), "Player 4 sustains a MINOR injury.")
        );

        List<MatchEventResponse> decisions = substitutionDecisionService.decideSubstitutions(squad, 0, events, MatchImportance.GROUP_STAGE, false);

        // Player 4 should be substituted out because of injury!
        boolean substitutionFound = decisions.stream().anyMatch(d -> 
            d.eventType().equals(MatchEventType.SUBSTITUTION.name()) 
            && d.description().contains("Player 4.")
        );
        
        assertThat(substitutionFound).isTrue();
    }
    
    @Test
    void test6_fatiguePersistsAfterMatch() {
        playerStateService.updateAfterMatch(10L, 99L, 2, 1, List.of());

        // Player 1 played full match (starting XI)
        PlayerState state = playerStateRepository.findByPlayerId(1L).get();
        assertThat(state.getFatigue()).isGreaterThan(0);
        assertThat(state.getFitness()).isLessThan(100);
    }
}
