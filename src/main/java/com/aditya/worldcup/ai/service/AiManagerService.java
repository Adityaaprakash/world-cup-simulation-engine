package com.aditya.worldcup.ai.service;

import com.aditya.worldcup.formations.entity.Formation;
import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.tactics.entity.TacticalProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiManagerService {

    private final FormationSelectionService formationSelectionService;
    private final LineupSelectionService lineupSelectionService;
    private final CaptainSelectionService captainSelectionService;
    private final TacticalSelectionService tacticalSelectionService;
    private final SubstitutionDecisionService substitutionDecisionService;
    private final SquadRepository squadRepository;

    @Transactional
    public void prepareForMatch(Squad squad, Squad opponent) {
        int squadQuality = squad.getTeam().getOverallRating();
        int opponentQuality = opponent.getTeam().getOverallRating();
        Formation formation = formationSelectionService.selectFormation(
                squadQuality, opponentQuality, squad.getFormation());
        lineupSelectionService.selectStartingXi(squad, formation);
        squadRepository.save(squad);
        captainSelectionService.selectCaptainAndViceCaptain(squad);
        tacticalSelectionService.selectTactics(
                squad.getTeam(), squadQuality, opponentQuality);
    }

    public TacticalProfile adjustTacticsForMatchState(Squad squad,
                                                       int goalDifference) {
        return tacticalSelectionService.adjustForMatchState(
                squad.getTeam(), goalDifference);
    }

    public List<MatchEventResponse> decideSubstitutions(Squad squad,
                                                         int goalDifference) {
        return substitutionDecisionService.decideSubstitutions(squad, goalDifference);
    }
}
