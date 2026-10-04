package com.aditya.worldcup.scouting.service;

import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.service.ManagerService;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.players.service.PlayerEffectiveRatingService;
import com.aditya.worldcup.players.service.PlayerStateService;
import com.aditya.worldcup.scouting.entity.KnowledgeLevel;
import com.aditya.worldcup.scouting.entity.ReportStatus;
import com.aditya.worldcup.scouting.entity.Scout;
import com.aditya.worldcup.scouting.entity.ScoutingReport;
import com.aditya.worldcup.scouting.entity.ScoutRecommendation;
import com.aditya.worldcup.scouting.repository.ScoutRepository;
import com.aditya.worldcup.scouting.repository.ScoutingReportRepository;
import com.aditya.worldcup.tactics.entity.TacticalProfile;
import com.aditya.worldcup.tactics.service.TacticalProfileService;
import com.aditya.worldcup.teams.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ScoutingService {

    private final ScoutRepository scoutRepository;
    private final ScoutingReportRepository reportRepository;
    private final ManagerService managerService;
    private final PlayerRepository playerRepository;
    private final PlayerStateService playerStateService;
    private final PlayerEffectiveRatingService playerEffectiveRatingService;
    private final TacticalProfileService tacticalProfileService;
    private final TeamService teamService; // To get the manager's team for tactical profile

    @Transactional(readOnly = true)
    public List<Scout> getScoutsForCurrentManager() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return scoutRepository.findByManagerOrderByNameAsc(managerService.getOrCreateManager(auth));
    }
    
    @Transactional
    public Scout createScout(Manager manager, String name, int evalSkill, int potEvalSkill, int tacKnowledge, String region) {
        return scoutRepository.save(Scout.builder()
                .manager(manager)
                .name(name)
                .evaluationSkill(evalSkill)
                .potentialEvaluationSkill(potEvalSkill)
                .tacticalKnowledge(tacKnowledge)
                .regionSpecialization(region)
                .build());
    }

    @Transactional
    public ScoutingReport assignScout(Long scoutId, Long playerId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Manager manager = managerService.getOrCreateManager(auth);
        Scout scout = scoutRepository.findByIdAndManager(scoutId, manager)
                .orElseThrow(() -> new IllegalArgumentException("Scout not found"));
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        Optional<ScoutingReport> existing = reportRepository.findByManagerAndPlayer(manager, player);
        ScoutingReport report;

        if (existing.isPresent()) {
            report = existing.get();
            if (report.getStatus() == ReportStatus.SCOUTING || report.getStatus() == ReportStatus.ASSIGNED) {
                throw new IllegalStateException("Player is already being scouted");
            }
            if (report.getKnowledgeLevel() == KnowledgeLevel.HIGH) {
                throw new IllegalStateException("Player is already fully scouted");
            }
            report.setStatus(ReportStatus.SCOUTING);
            report.setProgress(0);
            report.setScout(scout);
        } else {
            report = ScoutingReport.builder()
                    .manager(manager)
                    .scout(scout)
                    .player(player)
                    .status(ReportStatus.SCOUTING)
                    .progress(0)
                    .knowledgeLevel(KnowledgeLevel.NONE)
                    .confidence(0)
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        return reportRepository.save(report);
    }

    @Transactional
    public void advanceAllAssignments(int days) {
        // Find all scouting assignments... since this might be a timed system
        // Normally this would be a CRON job for all managers, but for isolation we'll just implement progress.
        // I will let it process globally, using transactional batches if needed.
        // For now, let's just update all SCOUTING reports for the current manager, normally it's done for all.
        // Because of SaveSlots, time advances independently per save, so we'll do it for the current manager.
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Manager manager = managerService.getOrCreateManager(auth);
        List<ScoutingReport> activeReports = reportRepository.findByManagerAndStatus(manager, ReportStatus.SCOUTING);
        for (ScoutingReport report : activeReports) {
            advanceReport(report, days);
        }
    }
    
    @Transactional
    public void forceCompleteAssignment(Long reportId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Manager manager = managerService.getOrCreateManager(auth);
        ScoutingReport report = reportRepository.findByIdAndManager(reportId, manager)
                .orElseThrow();
        if (report.getStatus() == ReportStatus.SCOUTING) {
            advanceReport(report, 100);
        }
    }

    private void advanceReport(ScoutingReport report, int steps) {
        Scout scout = report.getScout();
        int jump = steps * (scout.getEvaluationSkill() / 10 + 5);
        report.setProgress(Math.min(100, report.getProgress() + jump));

        if (report.getProgress() >= 100) {
            completeReport(report);
        }
        reportRepository.save(report);
    }

    private void completeReport(ScoutingReport report) {
        report.setStatus(ReportStatus.COMPLETED);
        report.setCompletedAt(LocalDateTime.now());
        
        // Progress knowledge level
        if (report.getKnowledgeLevel() == KnowledgeLevel.NONE) {
            report.setKnowledgeLevel(KnowledgeLevel.LOW);
        } else if (report.getKnowledgeLevel() == KnowledgeLevel.LOW) {
            report.setKnowledgeLevel(KnowledgeLevel.MEDIUM);
        } else if (report.getKnowledgeLevel() == KnowledgeLevel.MEDIUM) {
            report.setKnowledgeLevel(KnowledgeLevel.HIGH);
        }
        
        Scout scout = report.getScout();
        Player player = report.getPlayer();
        PlayerState state = playerStateService.getOrCreateState(player); // manager context
        PlayerEffectiveRatingService.EffectiveAttributes effective = playerEffectiveRatingService.getEffectiveAttributes(player, state);

        int trueOverall = effective.overallRating();
        int truePotential = player.getPotential();

        // Uncertainty Calculation
        // Worse scout = wider range. Better scout = narrower.
        // Higher knowledge level = narrower range.
        int evalError = Math.max(1, (100 - scout.getEvaluationSkill()) / 10);
        int potError = Math.max(1, (100 - scout.getPotentialEvaluationSkill()) / 10);
        
        int uncertaintyOverall = report.getKnowledgeLevel() == KnowledgeLevel.HIGH ? 1 :
                                 report.getKnowledgeLevel() == KnowledgeLevel.MEDIUM ? evalError :
                                 evalError * 2;
                                 
        int uncertaintyPot = report.getKnowledgeLevel() == KnowledgeLevel.HIGH ? 2 :
                             report.getKnowledgeLevel() == KnowledgeLevel.MEDIUM ? potError :
                             potError * 2;

        // Bounded mathematically without random to remain deterministic
        // We will just statically offset it so it is perfectly testable!
        // Overall: minus uncertainty, plus uncertainty + 1
        int estOvrMin = Math.max(1, trueOverall - uncertaintyOverall);
        int estOvrMax = Math.min(99, trueOverall + uncertaintyOverall + 1);
        
        int estPotMin = Math.max(1, truePotential - uncertaintyPot);
        int estPotMax = Math.min(99, truePotential + uncertaintyPot + 2);

        report.setEstimatedOverallMin(estOvrMin);
        report.setEstimatedOverallMax(estOvrMax);
        report.setEstimatedPotentialMin(estPotMin);
        report.setEstimatedPotentialMax(estPotMax);

        // Confidence goes up to ~100
        int calculatedConfidence = switch (report.getKnowledgeLevel()) {
            case LOW -> 30 + (scout.getEvaluationSkill() / 4);
            case MEDIUM -> 60 + (scout.getEvaluationSkill() / 4);
            case HIGH -> 90 + (scout.getEvaluationSkill() / 10);
            default -> 0;
        };
        report.setConfidence(Math.min(100, calculatedConfidence));

        // Tactical Compatibility
        // Tactical knowledge of scout influences accuracy of this number. 
        // True compatibility = some formula based on team tactical profile + player attributes.
        // Since Team is not directly on Player, we compare to current Manager's active team profile.
        // In this game, managerService has team or manager has a default tactical profile?
        // Let's assume tactical compatibility is derived from Manager's favorite profile vs Player.
        int rawCompatibility = 70; // baseline
        if (effective.pace() > 80 && "4-3-3".equals(report.getManager().getFavoriteFormation())) {
            rawCompatibility += 10;
        }
        if (effective.defending() > 80 && "DEFENSIVE".equals(report.getManager().getCoachingStyle().name())) {
            rawCompatibility += 15;
        }
        
        // Add uncertainty
        int tacticalError = Math.max(0, (100 - scout.getTacticalKnowledge()) / 5);
        int estComp = rawCompatibility - tacticalError; 
        report.setTacticalCompatibility(Math.min(100, Math.max(10, estComp)));

        // Recommendation
        if (estPotMax >= 85 && estOvrMin >= 75) {
            report.setRecommendation(ScoutRecommendation.HIGHLY_RECOMMENDED);
        } else if (estPotMax >= 80) {
            report.setRecommendation(ScoutRecommendation.RECOMMENDED);
        } else if (estPotMax >= 70) {
            report.setRecommendation(ScoutRecommendation.MONITOR);
        } else {
            report.setRecommendation(ScoutRecommendation.NOT_RECOMMENDED);
        }
    }
}
