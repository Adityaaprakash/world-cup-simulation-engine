package com.aditya.worldcup.simulation.service;

import com.aditya.worldcup.live.service.LiveMatchBroadcasterService;
import com.aditya.worldcup.managers.service.CareerStatisticsService;
import com.aditya.worldcup.managers.service.ManagerJobService;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.optimization.service.SimulationMetricsService;
import com.aditya.worldcup.simulation.dto.MatchSimulationRequest;
import com.aditya.worldcup.simulation.dto.MatchSimulationResponse;
import com.aditya.worldcup.simulation.dto.TournamentMatchSimulationResponse;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.standings.service.StandingUpdateService;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.tournaments.entity.TournamentStatus;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import com.aditya.worldcup.tournaments.service.TournamentIntelligenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentMatchSimulationServiceTest {

    @Mock private MatchRepository matchRepository;
    @Mock private TournamentRepository tournamentRepository;
    @Mock private SquadRepository squadRepository;
    @Mock private MatchSimulationService matchSimulationService;
    @Mock private StandingUpdateService standingUpdateService;
    @Mock private TournamentIntelligenceService tournamentIntelligenceService;
    @Mock private SimulationMetricsService simulationMetricsService;
    @Mock private CareerStatisticsService careerStatisticsService;
    @Mock private ManagerJobService managerJobService;
    @Mock private LiveMatchBroadcasterService liveMatchBroadcasterService;

    @InjectMocks
    private TournamentMatchSimulationService tournamentMatchSimulationService;

    private Tournament tournament;
    private Match match;
    private Squad homeSquad;
    private Squad awaySquad;
    private MatchSimulationResponse simulationResponse;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();

        tournament = new Tournament();
        tournament.setId(1L);
        tournament.setStatus(TournamentStatus.UPCOMING);

        Team homeTeam = new Team();
        homeTeam.setId(10L);
        Team awayTeam = new Team();
        awayTeam.setId(20L);

        match = new Match();
        match.setId(100L);
        match.setTournament(tournament);
        match.setStatus(MatchStatus.SCHEDULED);
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);

        homeSquad = new Squad();
        homeSquad.setId(1000L);

        awaySquad = new Squad();
        awaySquad.setId(2000L);

        simulationResponse = new MatchSimulationResponse(
                "Home", "Away", 2, 1, "Home", false, false, null, null, 80, 75,
                null, null, null, null, null
        );
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clear();
    }

    private void mockSuccessfulSimulation() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(matchRepository.findById(100L)).thenReturn(Optional.of(match));
        when(squadRepository.findFirstByTeamId(10L)).thenReturn(Optional.of(homeSquad));
        when(squadRepository.findFirstByTeamId(20L)).thenReturn(Optional.of(awaySquad));
        when(matchSimulationService.simulate(any(MatchSimulationRequest.class), eq(match)))
                .thenReturn(simulationResponse);
    }

    @Test
    void simulate_shouldTriggerBroadcasterOnSuccess() {
        mockSuccessfulSimulation();

        TournamentMatchSimulationResponse result = tournamentMatchSimulationService.simulate(1L, 100L);
        
        // Execute the afterCommit block
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());

        assertThat(result).isNotNull();
        assertThat(result.homeGoals()).isEqualTo(2);
        
        verify(matchSimulationService, times(1)).simulate(any(), any());
        verify(liveMatchBroadcasterService, times(1)).broadcastMatch(100L, simulationResponse);
    }

    @Test
    void simulate_broadcasterFailureShouldNotFailSimulation() {
        mockSuccessfulSimulation();
        doThrow(new RuntimeException("Broadcast failed")).when(liveMatchBroadcasterService).broadcastMatch(anyLong(), any());

        TournamentMatchSimulationResponse result = tournamentMatchSimulationService.simulate(1L, 100L);

        // Execute the afterCommit block
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());

        assertThat(result).isNotNull();
        verify(liveMatchBroadcasterService, times(1)).broadcastMatch(100L, simulationResponse);
    }

    @Test
    void simulate_simulationFailureShouldNotTriggerBroadcaster() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(matchRepository.findById(100L)).thenReturn(Optional.of(match));
        when(squadRepository.findFirstByTeamId(10L)).thenReturn(Optional.of(homeSquad));
        when(squadRepository.findFirstByTeamId(20L)).thenReturn(Optional.of(awaySquad));
        
        when(matchSimulationService.simulate(any(), any())).thenThrow(new RuntimeException("Sim error"));

        assertThrows(RuntimeException.class, () -> tournamentMatchSimulationService.simulate(1L, 100L));

        verify(liveMatchBroadcasterService, never()).broadcastMatch(anyLong(), any());
    }

    @Test
    void simulate_invalidMatchStateShouldNotTriggerBroadcaster() {
        match.setStatus(MatchStatus.FINISHED);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(matchRepository.findById(100L)).thenReturn(Optional.of(match));

        assertThrows(IllegalStateException.class, () -> tournamentMatchSimulationService.simulate(1L, 100L));

        verify(matchSimulationService, never()).simulate(any(), any());
        verify(liveMatchBroadcasterService, never()).broadcastMatch(anyLong(), any());
    }
}
