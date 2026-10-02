package com.aditya.worldcup.tactics.service;

import com.aditya.worldcup.ai.service.AiManagerService;
import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.simulation.service.MatchContext;
import com.aditya.worldcup.simulation.service.MatchModifierService;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.tactics.entity.TacticalProfile;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InMatchDecisionEngineService {

    private final AiManagerService aiManagerService;
    private final TacticalModifierService tacticalModifierService;
    private final MatchModifierService matchModifierService;

    @Data
    @Builder
    public static class OngoingSimulationState {
        private TacticalProfile homeProfile;
        private TacticalProfile awayProfile;
        private TacticalMatchModifiers homeModifiers;
        private TacticalMatchModifiers awayModifiers;
        private List<MatchEventResponse> newTimeline;
        private int currentHomeGoals;
        private int currentAwayGoals;
        private boolean homeRedCard;
        private boolean awayRedCard;
    }

    public OngoingSimulationState processOngoingMatch(
            List<MatchEventResponse> preGeneratedTimeline,
            Squad homeSquad, 
            Squad awaySquad,
            TacticalProfile initialHomeProfile, 
            TacticalProfile initialAwayProfile,
            MatchContext matchContext,
            boolean isExtraTimePossible) {

        OngoingSimulationState state = OngoingSimulationState.builder()
                .homeProfile(initialHomeProfile)
                .awayProfile(initialAwayProfile)
                .homeModifiers(tacticalModifierService.calculateModifiers(initialHomeProfile, initialAwayProfile))
                .awayModifiers(tacticalModifierService.calculateModifiers(initialAwayProfile, initialHomeProfile))
                .newTimeline(new ArrayList<>())
                .currentHomeGoals(0)
                .currentAwayGoals(0)
                .homeRedCard(false)
                .awayRedCard(false)
                .build();

        for (MatchEventResponse event : preGeneratedTimeline) {
            state.getNewTimeline().add(event);

            updateStateFromEvent(state, event, homeSquad.getTeam().getId());

            // Checkpoints: Half-Time (45) or after a major event (Goal/Red Card)
            boolean isGoal = "GOAL".equals(event.eventType()) || "PENALTY".equals(event.eventType()) && event.description().contains("converts");
            boolean isRedCard = "RED_CARD".equals(event.eventType());
            boolean isHalftime = event.minute() != null && event.minute() == 45;

            if (isGoal || isRedCard || isHalftime) {
                evaluateTacticalAdjustments(state, homeSquad, awaySquad, matchContext, event.minute(), isExtraTimePossible);
            }
        }

        return state;
    }

    private void updateStateFromEvent(OngoingSimulationState state, MatchEventResponse event, Long homeTeamId) {
        if ("GOAL".equals(event.eventType()) || ("PENALTY".equals(event.eventType()) && event.description().contains("converts"))) {
            if (homeTeamId.equals(event.teamId())) {
                state.setCurrentHomeGoals(state.getCurrentHomeGoals() + 1);
            } else if (event.teamId() != null) {
                state.setCurrentAwayGoals(state.getCurrentAwayGoals() + 1);
            }
        } else if ("RED_CARD".equals(event.eventType())) {
            if (homeTeamId.equals(event.teamId())) {
                state.setHomeRedCard(true);
            } else if (event.teamId() != null) {
                state.setAwayRedCard(true);
            }
        }
    }

    private void evaluateTacticalAdjustments(OngoingSimulationState state, 
                                             Squad homeSquad, 
                                             Squad awaySquad, 
                                             MatchContext context, 
                                             Integer minute,
                                             boolean extraTime) {
        
        int goalDiff = state.getCurrentHomeGoals() - state.getCurrentAwayGoals();

        TacticalProfile newHomeProfile = aiManagerService.adjustTacticsForMatchState(
                homeSquad, goalDiff, state.isHomeRedCard(), state.isAwayRedCard(), extraTime);
                
        TacticalProfile newAwayProfile = aiManagerService.adjustTacticsForMatchState(
                awaySquad, -goalDiff, state.isAwayRedCard(), state.isHomeRedCard(), extraTime);

        boolean homeChanged = hasProfileChanged(state.getHomeProfile(), newHomeProfile);
        boolean awayChanged = hasProfileChanged(state.getAwayProfile(), newAwayProfile);

        if (homeChanged) {
            state.setHomeProfile(newHomeProfile);
            addTacticalChangeEvent(state, homeSquad, minute, "Home Team");
        }
        if (awayChanged) {
            state.setAwayProfile(newAwayProfile);
            addTacticalChangeEvent(state, awaySquad, minute, "Away Team");
        }

        if (homeChanged || awayChanged) {
            // Recalculate Modifiers which influences fatigue, pressure and statistics
            TacticalMatchModifiers newHomeMods = tacticalModifierService.calculateModifiers(newHomeProfile, newAwayProfile);
            TacticalMatchModifiers newAwayMods = tacticalModifierService.calculateModifiers(newAwayProfile, newHomeProfile);
            
            // Apply match context for ongoing influences (attacking/defensive pressures)
            state.setHomeModifiers(matchModifierService.applyContext(newHomeMods, context, true));
            state.setAwayModifiers(matchModifierService.applyContext(newAwayMods, context, false));
            log.info("Tactical Changes applied at minute {} for Match between {} and {}", minute, homeSquad.getName(), awaySquad.getName());
        }
    }

    private void addTacticalChangeEvent(OngoingSimulationState state, Squad squad, Integer minute, String teamDesc) {
        String eventDesc = teamDesc + " adjusts tactics based on the match state.";
        MatchEventResponse tacticalEvent = new MatchEventResponse(
                minute != null ? minute + 1 : 90,
                "Manager",
                null,
                squad.getTeam().getName(),
                squad.getTeam().getId(),
                "TACTICAL_CHANGE",
                eventDesc
        );
        state.getNewTimeline().add(tacticalEvent);
    }

    private boolean hasProfileChanged(TacticalProfile current, TacticalProfile updated) {
        if (current == null || updated == null) return false;
        return current.getPressingIntensity() != updated.getPressingIntensity() ||
               current.getDefensiveLine() != updated.getDefensiveLine() ||
               current.getTempo() != updated.getTempo() ||
               current.getWidth() != updated.getWidth() ||
               current.getPassingStyle() != updated.getPassingStyle() ||
               current.getAttackingApproach() != updated.getAttackingApproach() ||
               current.getBuildUpStyle() != updated.getBuildUpStyle() ||
               current.getDefensiveBlock() != updated.getDefensiveBlock();
    }
}
