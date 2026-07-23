package com.aditya.worldcup.ai.service;

import com.aditya.worldcup.formations.entity.Formation;
import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
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
    private final RotationService rotationService;
    private final PlayerEvaluationService playerEvaluationService;
    private final MatchImportanceService matchImportanceService;
    private final SquadRepository squadRepository;

    @Transactional
    public void prepareForMatch(Squad squad, Squad opponent) {
        prepareForMatch(squad, opponent, MatchImportance.GROUP_STAGE);
    }

    @Transactional
    public void prepareForMatch(Squad squad, Squad opponent, Match match) {
        prepareForMatch(squad, opponent, matchImportanceService.determine(match));
    }

    @Transactional
    public void prepareForMatch(Squad squad, Squad opponent, MatchImportance importance) {
        int squadQuality = squad.getTeam().getOverallRating();
        int opponentQuality = opponent.getTeam().getOverallRating();
        List<SquadPlayer> matchSquad = selectMatchSquad(squad, importance);
        Formation formation = formationSelectionService.selectFormation(
                matchSquad, squadQuality, opponentQuality, squad.getFormation());
        chooseTacticalProfile(squad, matchSquad, opponent);
        selectStartingEleven(squad, formation, importance);
        squadRepository.save(squad);
        chooseCaptain(squad);
        selectBench(squad, importance);
    }

    public List<SquadPlayer> selectMatchSquad(Squad squad) {
        return lineupSelectionService.selectMatchSquad(squad);
    }

    public List<SquadPlayer> selectMatchSquad(Squad squad, MatchImportance importance) {
        return lineupSelectionService.selectMatchSquad(squad, importance);
    }

    @Transactional
    public void selectStartingEleven(Squad squad, Formation formation) {
        lineupSelectionService.selectStartingXi(squad, formation);
    }

    @Transactional
    public void selectStartingEleven(Squad squad,
                                     Formation formation,
                                     MatchImportance importance) {
        lineupSelectionService.selectStartingXi(squad, formation, importance);
    }

    public List<SquadPlayer> selectBench(Squad squad) {
        return lineupSelectionService.selectBench(squad);
    }

    public List<SquadPlayer> selectBench(Squad squad, MatchImportance importance) {
        return lineupSelectionService.selectBench(squad, importance);
    }

    public Formation chooseFormation(Squad squad, Squad opponent) {
        return formationSelectionService.selectFormation(
                selectMatchSquad(squad),
                squad.getTeam().getOverallRating(),
                opponent.getTeam().getOverallRating(),
                squad.getFormation());
    }

    public TacticalProfile chooseTacticalProfile(Squad squad,
                                                 List<SquadPlayer> matchSquad,
                                                 Squad opponent) {
        return tacticalSelectionService.selectTactics(
                squad.getTeam(),
                matchSquad,
                squad.getTeam().getOverallRating(),
                opponent.getTeam().getOverallRating());
    }

    @Transactional
    public void chooseCaptain(Squad squad) {
        captainSelectionService.selectCaptainAndViceCaptain(squad);
    }

    public boolean rotatePlayers(Player player) {
        return rotationService.shouldRest(player);
    }

    public boolean rotatePlayers(Player player, MatchImportance importance) {
        return rotationService.shouldRest(player, importance);
    }

    public double evaluatePlayer(Player player) {
        return playerEvaluationService.evaluatePlayer(player);
    }

    public TacticalProfile adjustTacticsForMatchState(Squad squad,
                                                       int goalDifference) {
        return tacticalSelectionService.adjustForMatchState(
                squad.getTeam(), goalDifference);
    }

    public TacticalProfile adjustTacticsForMatchState(Squad squad,
                                                       int goalDifference,
                                                       boolean ownRedCard,
                                                       boolean opponentRedCard,
                                                       boolean extraTime) {
        return tacticalSelectionService.adjustForMatchState(
                squad.getTeam(),
                goalDifference,
                ownRedCard,
                opponentRedCard,
                extraTime,
                selectMatchSquad(squad));
    }

    public List<MatchEventResponse> makeSubstitutions(Squad squad,
                                                       int goalDifference) {
        return substitutionDecisionService.decideSubstitutions(squad, goalDifference);
    }

    public List<MatchEventResponse> makeSubstitutions(Squad squad,
                                                       int goalDifference,
                                                       List<MatchEventResponse> matchEvents,
                                                       MatchImportance importance,
                                                       boolean extraTime) {
        return substitutionDecisionService.decideSubstitutions(
                squad, goalDifference, matchEvents, importance, extraTime);
    }

    public List<MatchEventResponse> decideSubstitutions(Squad squad,
                                                         int goalDifference) {
        return makeSubstitutions(squad, goalDifference);
    }

    public MatchImportance determineMatchImportance(Match match) {
        return matchImportanceService.determine(match);
    }

    public void planRotationForNextMatch(Squad squad) {
        selectBench(squad);
    }
}
