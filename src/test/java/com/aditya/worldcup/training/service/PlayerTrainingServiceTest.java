package com.aditya.worldcup.training.service;

import com.aditya.worldcup.players.entity.InjuryStatus;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.service.PlayerStateService;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.training.entity.TrainingCategory;
import com.aditya.worldcup.training.entity.TrainingIntensity;
import com.aditya.worldcup.managers.entity.ManagerEconomy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerTrainingServiceTest {

    @Mock
    private PlayerStateService playerStateService;

    @Mock
    private SquadPlayerRepository squadPlayerRepository;

    @Mock
    private com.aditya.worldcup.squads.repository.SquadRepository squadRepository;

    @Mock
    private com.aditya.worldcup.managers.repository.ManagerEconomyRepository managerEconomyRepository;

    @InjectMocks
    private PlayerTrainingService playerTrainingService;

    private Player youngPlayer;
    private Player oldPlayer;
    private PlayerState youngState;
    private PlayerState oldState;

    @BeforeEach
    void setUp() {
        youngPlayer = Player.builder().id(1L).age(20).overallRating(70).potential(85).build();
        youngState = PlayerState.builder().player(youngPlayer).fatigue(10).fitness(100).morale(50).developmentRating(0).progressionTracker(0).injuryStatus(InjuryStatus.HEALTHY).build();

        oldPlayer = Player.builder().id(2L).age(32).overallRating(80).potential(80).build();
        oldState = PlayerState.builder().player(oldPlayer).fatigue(10).fitness(100).morale(50).developmentRating(0).progressionTracker(0).injuryStatus(InjuryStatus.HEALTHY).build();
    }

    @Test
    void testTrainSquad() {
        SquadPlayer sp = SquadPlayer.builder().player(youngPlayer).build();
        when(squadPlayerRepository.findBySquadId(10L)).thenReturn(List.of(sp));
        when(playerStateService.getOrCreateState(youngPlayer)).thenReturn(youngState);
        when(playerStateService.isAvailable(youngState)).thenReturn(true);

        com.aditya.worldcup.squads.entity.Squad squad = new com.aditya.worldcup.squads.entity.Squad();
        squad.setId(10L);
        com.aditya.worldcup.teams.entity.Team t = new com.aditya.worldcup.teams.entity.Team();
        squad.setTeam(t);
        com.aditya.worldcup.users.entity.User u = new com.aditya.worldcup.users.entity.User();
        u.setId(99L);
        squad.setUser(u);
        when(squadRepository.findById(10L)).thenReturn(java.util.Optional.of(squad));

        playerTrainingService.trainSquad(10L, TrainingCategory.TECHNICAL, TrainingIntensity.NORMAL);

        verify(playerStateService).saveAll(any());
        assertThat(youngState.getProgressionTracker()).isGreaterThan(0);
        assertThat(youngState.getFatigue()).isEqualTo(10 + 12); // NORMAL intensity fatigue
    }

    @Test
    void injuredPlayerCannotTrain() {
        youngState.setInjuryStatus(InjuryStatus.MINOR);
        when(playerStateService.isAvailable(youngState)).thenReturn(false);

        playerTrainingService.processPlayerTraining(youngState, TrainingCategory.TECHNICAL, TrainingIntensity.NORMAL, null);

        assertThat(youngState.getProgressionTracker()).isZero();
        assertThat(youngState.getFatigue()).isEqualTo(10);
    }

    @Test
    void restReducesFatigueAndGivesNoProgression() {
        youngState.setFatigue(50);
        
        playerTrainingService.processPlayerTraining(youngState, TrainingCategory.REST, TrainingIntensity.LIGHT, null);

        assertThat(youngState.getProgressionTracker()).isZero();
        assertThat(youngState.getFatigue()).isEqualTo(30);
        assertThat(youngState.getWorkload()).isZero();
    }

    @Test
    void progressionRollsOverToDevelopmentRating() {
        youngState.setProgressionTracker(95);
        when(playerStateService.isAvailable(youngState)).thenReturn(true);

        playerTrainingService.processPlayerTraining(youngState, TrainingCategory.PHYSICAL, TrainingIntensity.INTENSE, null);

        // Young player roomToGrow = 15. intense = 6, physical = +1 means 7. 15 * 7 = 105 tracker points.
        // 95 + 105 = 200 => +2 development rating, 0 tracker!
        
        assertThat(youngState.getDevelopmentRating()).isGreaterThan(0);
    }
    
    @Test
    void oldPlayerDeclinesWithIntenseTraining() {
        when(playerStateService.isAvailable(oldState)).thenReturn(true);
        oldState.setProgressionTracker(-95);
        
        playerTrainingService.processPlayerTraining(oldState, TrainingCategory.TECHNICAL, TrainingIntensity.INTENSE, null);

        // Old player intense = -5
        // -95 + -5 = -100 => -1 development rating, 0 tracker
        
        assertThat(oldState.getDevelopmentRating()).isEqualTo(-1);
        assertThat(oldState.getProgressionTracker()).isEqualTo(0);
    }
    
    @Test
    void highFatigueReducesProgression() {
        PlayerState freshState = PlayerState.builder().player(youngPlayer).fatigue(0).fitness(100).morale(50).developmentRating(0).progressionTracker(0).injuryStatus(InjuryStatus.HEALTHY).build();
        PlayerState tiredState = PlayerState.builder().player(youngPlayer).fatigue(100).fitness(100).morale(50).developmentRating(0).progressionTracker(0).injuryStatus(InjuryStatus.HEALTHY).build();
        
        when(playerStateService.isAvailable(freshState)).thenReturn(true);
        when(playerStateService.isAvailable(tiredState)).thenReturn(true);
        
        playerTrainingService.processPlayerTraining(freshState, TrainingCategory.TECHNICAL, TrainingIntensity.NORMAL, null);
        playerTrainingService.processPlayerTraining(tiredState, TrainingCategory.TECHNICAL, TrainingIntensity.NORMAL, null);
        
        assertThat(freshState.getProgressionTracker()).isGreaterThan(tiredState.getProgressionTracker());
        assertThat(tiredState.getProgressionTracker()).isZero(); // 100 fatigue = 1.0 penalty
    }

    @Test
    void propertiesRemainBounded() {
        youngState.setDevelopmentRating(10);
        youngState.setProgressionTracker(99);
        when(playerStateService.isAvailable(youngState)).thenReturn(true);
        
        playerTrainingService.processPlayerTraining(youngState, TrainingCategory.TECHNICAL, TrainingIntensity.INTENSE, null);
        
        assertThat(youngState.getDevelopmentRating()).isEqualTo(10);
        assertThat(youngState.getProgressionTracker()).isZero();
    }
    
    @Test
    void repeatedIntenseTrainingCompoundsFatigueAndWorkload() {
        when(playerStateService.isAvailable(youngState)).thenReturn(true);
        youngState.setWorkload(75);
        
        playerTrainingService.processPlayerTraining(youngState, TrainingCategory.PHYSICAL, TrainingIntensity.INTENSE, null);
        
        // Intensity INTENSE = 25 workload. Baseline fatigue increase = 25.
        // Since workload > 70 previously -> multiplier is 1.5. 25 * 1.5 = 38 fatigue!
        assertThat(youngState.getWorkload()).isEqualTo(100);
        assertThat(youngState.getFatigue()).isEqualTo(48); // 10 original + 38
    }
}
