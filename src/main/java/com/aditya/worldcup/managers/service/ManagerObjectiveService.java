package com.aditya.worldcup.managers.service;

import com.aditya.worldcup.managers.entity.*;
import com.aditya.worldcup.managers.repository.ManagerObjectiveRepository;
import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.saves.context.SaveContextHolder;
import com.aditya.worldcup.squads.entity.Squad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerObjectiveService {

    private final ManagerObjectiveRepository objectiveRepository;
    private final ManagerEconomyService managerEconomyService;
    private final CareerTimelineService careerTimelineService;
    private final ManagerService managerService;

    @Transactional
    public ManagerObjective createObjective(Manager manager, ObjectiveType type, String description, int targetValue, int rewardAmount, Tournament tournament) {
        ManagerObjective objective = ManagerObjective.builder()
                .manager(manager)
                .type(type)
                .description(description)
                .targetValue(targetValue)
                .currentValue(0)
                .rewardAmount(rewardAmount)
                .status(ObjectiveStatus.ACTIVE)
                .tournament(tournament)
                .createdAt(LocalDateTime.now())
                .build();
        return objectiveRepository.save(objective);
    }

    @Transactional
    public void evaluateMatch(Squad squad, boolean won, int goalsScored) {
        Manager manager = managerService.getOrCreateManager(squad.getUser().getEmail());
        if (won) {
            evaluateWinMatchProgress(manager);
        }
        if (goalsScored > 0) {
            evaluateGoalsScored(manager, goalsScored);
        }
    }

    @Transactional
    public void evaluateWinMatchProgress(Manager manager) {
        incrementObjectiveProgress(manager, ObjectiveType.WIN_MATCHES, 1);
    }

    @Transactional
    public void evaluateGoalsScored(Manager manager, int goals) {
        incrementObjectiveProgress(manager, ObjectiveType.SCORE_GOALS, goals);
    }

    @Transactional
    public void evaluateTournamentProgress(Manager manager, Tournament tournament) {
        // Find if they reached a specifically staged target or tournament win
        List<ManagerObjective> activeObjectives = objectiveRepository.findByManagerIdAndStatus(manager.getId(), ObjectiveStatus.ACTIVE);
        for (ManagerObjective obj : activeObjectives) {
            if (obj.getTournament() != null && obj.getTournament().getId().equals(tournament.getId())) {
                if (obj.getType() == ObjectiveType.WIN_TOURNAMENT || obj.getType() == ObjectiveType.REACH_TOURNAMENT_STAGE) {
                    incrementObjectiveProgress(manager, obj.getType(), 1);
                }
            }
        }
    }

    @Transactional
    public void evaluatePlayerDevelopmentProgress(Manager manager) {
        incrementObjectiveProgress(manager, ObjectiveType.DEVELOP_PLAYERS, 1);
    }

    private void incrementObjectiveProgress(Manager manager, ObjectiveType type, int amount) {
        List<ManagerObjective> activeObjectives = objectiveRepository.findByManagerIdAndStatus(manager.getId(), ObjectiveStatus.ACTIVE);
        
        for (ManagerObjective objective : activeObjectives) {
            if (objective.getType() == type) {
                int expectedTarget = objective.getTargetValue();
                int newValue = objective.getCurrentValue() + amount;
                objective.setCurrentValue(Math.min(newValue, expectedTarget));
                
                if (objective.getCurrentValue() >= expectedTarget) {
                    completeObjective(manager, objective);
                } else {
                    objectiveRepository.save(objective);
                }
            }
        }
    }

    private void completeObjective(Manager manager, ManagerObjective objective) {
        objective.setStatus(ObjectiveStatus.COMPLETED);
        objective.setCompletedAt(LocalDateTime.now());
        objectiveRepository.save(objective);

        // Economy Integration (Phase 11G & 11H Reward idempontency check inside addFunds but we use specific key)
        String idempotencyKey = "OBJ_REWARD_" + objective.getId();
        managerEconomyService.addFunds(manager, objective.getRewardAmount(), "Objective Reward: " + objective.getDescription(), idempotencyKey);

        careerTimelineService.recordEvent(
                manager, 
                TimelineEventType.OBJECTIVE_COMPLETED,
                "Objective Completed",
                objective.getDescription(),
                objective.getTournament() != null ? objective.getTournament().getId() : null,
                null
        );
    }

    @Transactional
    public void failObjective(ManagerObjective objective) {
        objective.setStatus(ObjectiveStatus.FAILED);
        objective.setCompletedAt(LocalDateTime.now());
        objectiveRepository.save(objective);

        careerTimelineService.recordEvent(
                objective.getManager(), 
                TimelineEventType.OBJECTIVE_FAILED,
                "Objective Failed",
                objective.getDescription(),
                objective.getTournament() != null ? objective.getTournament().getId() : null,
                null
        );
    }

    public List<ManagerObjective> getActiveObjectives(Long managerId) {
        return objectiveRepository.findByManagerIdAndStatus(managerId, ObjectiveStatus.ACTIVE);
    }

    public List<ManagerObjective> getAllObjectives(Long managerId) {
        return objectiveRepository.findByManagerIdOrderByCreatedAtDesc(managerId);
    }
}
