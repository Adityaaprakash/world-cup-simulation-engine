package com.aditya.worldcup.tactics.service;

import com.aditya.worldcup.ai.service.AiManagerService;
import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.simulation.service.MatchContext;
import com.aditya.worldcup.simulation.service.MatchModifierService;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.tactics.entity.PressingIntensity;
import com.aditya.worldcup.tactics.entity.TacticalProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import com.aditya.worldcup.simulation.service.WeatherCondition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InMatchDecisionEngineServiceTest {

    @Mock
    private AiManagerService aiManagerService;

    @Mock
    private TacticalModifierService tacticalModifierService;

    @Mock
    private MatchModifierService matchModifierService;

    @InjectMocks
    private InMatchDecisionEngineService engineService;

    @Test
    void processOngoingMatch_AppliesTacticalChangesAtHalftime() {
        // Arrange
        Squad homeSquad = new Squad();
        Team homeTeam = new Team();
        homeTeam.setId(1L);
        homeSquad.setTeam(homeTeam);

        Squad awaySquad = new Squad();
        Team awayTeam = new Team();
        awayTeam.setId(2L);
        awaySquad.setTeam(awayTeam);

        TacticalProfile initialHomeProfile = new TacticalProfile();
        initialHomeProfile.setPressingIntensity(PressingIntensity.BALANCED);

        TacticalProfile initialAwayProfile = new TacticalProfile();
        
        TacticalProfile updatedHomeProfile = new TacticalProfile();
        updatedHomeProfile.setPressingIntensity(PressingIntensity.HIGH); // Changed

        when(aiManagerService.adjustTacticsForMatchState(any(), anyInt(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(updatedHomeProfile)
                .thenReturn(initialAwayProfile);

        when(tacticalModifierService.calculateModifiers(any(), any()))
                .thenReturn(TacticalMatchModifiers.balanced());

        when(matchModifierService.applyContext(any(), any(), anyBoolean()))
                .thenReturn(TacticalMatchModifiers.balanced());

        List<MatchEventResponse> timeline = List.of(
                new MatchEventResponse(45, "Ref", "HALF_TIME", "HT")
        );
        // Act
        InMatchDecisionEngineService.OngoingSimulationState state = engineService.processOngoingMatch(
                timeline, homeSquad, awaySquad, initialHomeProfile, initialAwayProfile, new MatchContext(WeatherCondition.CLEAR), false
        );

        // Assert
        assertEquals(PressingIntensity.HIGH, state.getHomeProfile().getPressingIntensity());
        assertTrue(state.getNewTimeline().stream().anyMatch(e -> "TACTICAL_CHANGE".equals(e.eventType())));
    }
}
