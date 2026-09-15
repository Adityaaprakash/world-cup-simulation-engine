package com.aditya.worldcup.managers.service;

import com.aditya.worldcup.managers.dto.ManagerJobRequest;
import com.aditya.worldcup.managers.dto.ManagerJobResponse;
import com.aditya.worldcup.managers.entity.BoardObjective;
import com.aditya.worldcup.managers.entity.JobStatus;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.entity.ManagerJob;
import com.aditya.worldcup.managers.entity.TimelineEventType;
import com.aditya.worldcup.managers.repository.ManagerJobRepository;
import com.aditya.worldcup.shared.exception.TeamNotFoundException;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.teams.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerJobService {

    private final ManagerJobRepository managerJobRepository;
    private final ManagerService managerService;
    private final TeamRepository teamRepository;
    private final CareerTimelineService careerTimelineService;

    @Transactional
    public ManagerJobResponse acceptJob(Authentication authentication, ManagerJobRequest request) {
        Manager manager = managerService.getOrCreateManager(authentication);

        // Terminate any existing active job (resign)
        managerJobRepository.findByManagerIdAndStatus(manager.getId(), JobStatus.ACTIVE)
                .ifPresent(activeJob -> {
                    activeJob.setStatus(JobStatus.RESIGNED);
                    activeJob.setEndedAt(LocalDateTime.now());
                    managerJobRepository.save(activeJob);
                    careerTimelineService.recordEvent(manager, TimelineEventType.RESIGNED, "Resigned", "Resigned from " + activeJob.getTeam().getName(), null, activeJob.getTeam().getId());
                });

        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new TeamNotFoundException(request.teamId()));

        // SACK anyone else who currently has this job
        managerJobRepository.findActiveJobForTeam(team.getId())
                .ifPresent(existingJob -> {
                    existingJob.setStatus(JobStatus.SACKED);
                    existingJob.setEndedAt(LocalDateTime.now());
                    managerJobRepository.save(existingJob);
                    careerTimelineService.recordEvent(existingJob.getManager(), TimelineEventType.SACKED, "Sacked", "Sacked by " + team.getName(), null, team.getId());
                });

        ManagerJob newJob = ManagerJob.builder()
                .manager(manager)
                .team(team)
                .targetObjective(request.targetObjective())
                .boardConfidence(100.0)
                .status(JobStatus.ACTIVE)
                .startedAt(LocalDateTime.now())
                .build();

        ManagerJob savedJob = managerJobRepository.save(newJob);
        
        careerTimelineService.recordEvent(manager, TimelineEventType.HIRED, "Hired", "Hired as manager of " + team.getName(), null, team.getId());

        return mapToResponse(savedJob);
    }

    @Transactional
    public ManagerJobResponse resignFromJob(Authentication authentication) {
        Manager manager = managerService.getOrCreateManager(authentication);

        ManagerJob activeJob = managerJobRepository.findByManagerIdAndStatus(manager.getId(), JobStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("Manager does not have an active job"));

        activeJob.setStatus(JobStatus.RESIGNED);
        activeJob.setEndedAt(LocalDateTime.now());
        ManagerJob savedJob = managerJobRepository.save(activeJob);
        
        careerTimelineService.recordEvent(manager, TimelineEventType.RESIGNED, "Resigned", "Resigned from " + activeJob.getTeam().getName(), null, activeJob.getTeam().getId());

        return mapToResponse(savedJob);
    }

    @Transactional(readOnly = true)
    public List<ManagerJobResponse> getMyJobs(Authentication authentication) {
        Manager manager = managerService.getOrCreateManager(authentication);
        return managerJobRepository.findByManagerIdOrderByStartedAtDesc(manager.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void evaluateMatchResult(Long homeTeamId, Long awayTeamId, int homeGoals, int awayGoals) {
        // Evaluate Home Team Manager
        managerJobRepository.findActiveJobForTeam(homeTeamId).ifPresent(job -> {
            adjustConfidence(job, homeGoals, awayGoals);
        });

        // Evaluate Away Team Manager
        managerJobRepository.findActiveJobForTeam(awayTeamId).ifPresent(job -> {
            adjustConfidence(job, awayGoals, homeGoals);
        });
    }

    private void adjustConfidence(ManagerJob job, int goalsFor, int goalsAgainst) {
        double currentConfidence = job.getBoardConfidence();
        double modification = 0.0;

        if (goalsFor > goalsAgainst) {
            modification = 5.0; // Win
        } else if (goalsFor == goalsAgainst) {
            modification = 0.0; // Draw
        } else {
            modification = -8.0; // Loss
        }

        // Apply objective-based multipliers (simplistic for now)
        if (job.getTargetObjective() == BoardObjective.WIN_TOURNAMENT) {
            modification = modification > 0 ? modification * 0.8 : modification * 1.5; // Harder to gain, easier to lose
        } else if (job.getTargetObjective() == BoardObjective.BE_COMPETITIVE) {
            modification = modification > 0 ? modification * 1.2 : modification * 0.8; // Easier to gain, harder to lose
        }

        double newConfidence = Math.max(0.0, Math.min(100.0, currentConfidence + modification));
        job.setBoardConfidence(newConfidence);

        if (newConfidence == 0.0 && job.getStatus() == JobStatus.ACTIVE) {
            job.setStatus(JobStatus.SACKED);
            job.setEndedAt(LocalDateTime.now());
            log.info("Manager {} has been sacked by {} due to low board confidence.", job.getManager().getUsername(), job.getTeam().getName());
            
            careerTimelineService.recordEvent(job.getManager(), TimelineEventType.SACKED, "Sacked", "Sacked by " + job.getTeam().getName() + " due to poor performance", null, job.getTeam().getId());
        }

        managerJobRepository.save(job);
    }

    private ManagerJobResponse mapToResponse(ManagerJob job) {
        return new ManagerJobResponse(
                job.getId(),
                job.getManager().getId(),
                job.getManager().getDisplayName(),
                job.getTeam().getId(),
                job.getTeam().getName(),
                job.getTargetObjective(),
                job.getBoardConfidence(),
                job.getStatus(),
                job.getStartedAt(),
                job.getEndedAt()
        );
    }
}
