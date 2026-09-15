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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerJobServiceTest {

    @Mock private ManagerJobRepository managerJobRepository;
    @Mock private ManagerService managerService;
    @Mock private TeamRepository teamRepository;
    @Mock private CareerTimelineService careerTimelineService;
    @Mock private Authentication authentication;

    @InjectMocks
    private ManagerJobService managerJobService;

    private Manager manager;
    private Team team;

    @BeforeEach
    void setUp() {
        manager = new Manager();
        manager.setId(1L);
        manager.setUsername("manager@test.com");
        manager.setDisplayName("Test Manager");

        team = new Team();
        team.setId(10L);
        team.setName("Argentina");
        
        lenient().when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
    }

    @Test
    void acceptJobSuccessfully() {
        ManagerJobRequest request = new ManagerJobRequest(10L, BoardObjective.WIN_TOURNAMENT);

        when(managerJobRepository.findByManagerIdAndStatus(manager.getId(), JobStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(managerJobRepository.findActiveJobForTeam(10L)).thenReturn(Optional.empty());
        
        ManagerJob savedJob = new ManagerJob();
        savedJob.setId(100L);
        savedJob.setManager(manager);
        savedJob.setTeam(team);
        savedJob.setTargetObjective(BoardObjective.WIN_TOURNAMENT);
        savedJob.setBoardConfidence(100.0);
        savedJob.setStatus(JobStatus.ACTIVE);
        
        when(managerJobRepository.save(any(ManagerJob.class))).thenReturn(savedJob);

        ManagerJobResponse response = managerJobService.acceptJob(authentication, request);

        assertThat(response.managerId()).isEqualTo(1L);
        assertThat(response.teamId()).isEqualTo(10L);
        assertThat(response.boardConfidence()).isEqualTo(100.0);
        assertThat(response.status()).isEqualTo(JobStatus.ACTIVE);
        
        verify(careerTimelineService, times(1)).recordEvent(eq(manager), eq(TimelineEventType.HIRED), anyString(), anyString(), isNull(), eq(10L));
    }

    @Test
    void acceptJobResignsFromCurrentJob() {
        ManagerJobRequest request = new ManagerJobRequest(10L, BoardObjective.WIN_TOURNAMENT);
        Team oldTeam = new Team();
        oldTeam.setId(5L);
        oldTeam.setName("Brazil");
        
        ManagerJob existingActiveJob = new ManagerJob();
        existingActiveJob.setManager(manager);
        existingActiveJob.setTeam(oldTeam);
        existingActiveJob.setStatus(JobStatus.ACTIVE);

        when(managerJobRepository.findByManagerIdAndStatus(manager.getId(), JobStatus.ACTIVE))
                .thenReturn(Optional.of(existingActiveJob));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        
        ManagerJob savedJob = new ManagerJob();
        savedJob.setId(100L);
        savedJob.setManager(manager);
        savedJob.setTeam(team);
        
        when(managerJobRepository.save(any(ManagerJob.class))).thenReturn(savedJob);

        managerJobService.acceptJob(authentication, request);

        assertThat(existingActiveJob.getStatus()).isEqualTo(JobStatus.RESIGNED);
        assertThat(existingActiveJob.getEndedAt()).isNotNull();
        verify(careerTimelineService).recordEvent(eq(manager), eq(TimelineEventType.RESIGNED), anyString(), anyString(), isNull(), anyLong());
    }

    @Test
    void acceptJobSacksExistingManagerOfTeam() {
        ManagerJobRequest request = new ManagerJobRequest(10L, BoardObjective.WIN_TOURNAMENT);
        
        Manager oldManager = new Manager();
        oldManager.setId(2L);
        ManagerJob existingTeamJob = new ManagerJob();
        existingTeamJob.setManager(oldManager);
        existingTeamJob.setTeam(team);
        existingTeamJob.setStatus(JobStatus.ACTIVE);

        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(managerJobRepository.findActiveJobForTeam(10L)).thenReturn(Optional.of(existingTeamJob));
        
        ManagerJob savedJob = new ManagerJob();
        savedJob.setId(100L);
        savedJob.setManager(manager);
        savedJob.setTeam(team);
        when(managerJobRepository.save(any(ManagerJob.class))).thenReturn(savedJob);

        managerJobService.acceptJob(authentication, request);

        assertThat(existingTeamJob.getStatus()).isEqualTo(JobStatus.SACKED);
        assertThat(existingTeamJob.getEndedAt()).isNotNull();
        verify(careerTimelineService).recordEvent(eq(oldManager), eq(TimelineEventType.SACKED), anyString(), anyString(), isNull(), eq(10L));
    }

    @Test
    void acceptJobTeamNotFound() {
        ManagerJobRequest request = new ManagerJobRequest(999L, BoardObjective.WIN_TOURNAMENT);
        when(teamRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TeamNotFoundException.class, () -> managerJobService.acceptJob(authentication, request));
    }

    @Test
    void evaluateMatchResultUpdatesConfidence() {
        ManagerJob homeJob = new ManagerJob();
        homeJob.setManager(manager);
        homeJob.setTeam(team);
        homeJob.setBoardConfidence(50.0);
        homeJob.setTargetObjective(BoardObjective.NONE);
        homeJob.setStatus(JobStatus.ACTIVE);

        when(managerJobRepository.findActiveJobForTeam(10L)).thenReturn(Optional.of(homeJob));

        // Home Team wins 3-0
        managerJobService.evaluateMatchResult(10L, 20L, 3, 0);

        ArgumentCaptor<ManagerJob> captor = ArgumentCaptor.forClass(ManagerJob.class);
        verify(managerJobRepository).save(captor.capture());
        
        ManagerJob updatedJob = captor.getValue();
        assertThat(updatedJob.getBoardConfidence()).isGreaterThan(50.0); // Confidence increased
        assertThat(updatedJob.getStatus()).isEqualTo(JobStatus.ACTIVE);
    }
    
    @Test
    void evaluateMatchResultFiresManagerIfConfidenceHitsZero() {
        ManagerJob homeJob = new ManagerJob();
        homeJob.setManager(manager);
        homeJob.setTeam(team);
        homeJob.setBoardConfidence(2.0); // Very low
        homeJob.setTargetObjective(BoardObjective.WIN_TOURNAMENT); // High expectations
        homeJob.setStatus(JobStatus.ACTIVE);

        when(managerJobRepository.findActiveJobForTeam(10L)).thenReturn(Optional.of(homeJob));

        // Home Team loses 0-5
        managerJobService.evaluateMatchResult(10L, 20L, 0, 5);

        ArgumentCaptor<ManagerJob> captor = ArgumentCaptor.forClass(ManagerJob.class);
        verify(managerJobRepository).save(captor.capture());
        
        ManagerJob updatedJob = captor.getValue();
        assertThat(updatedJob.getBoardConfidence()).isEqualTo(0.0);
        assertThat(updatedJob.getStatus()).isEqualTo(JobStatus.SACKED);
        assertThat(updatedJob.getEndedAt()).isNotNull();
        verify(careerTimelineService).recordEvent(eq(manager), eq(TimelineEventType.SACKED), anyString(), anyString(), isNull(), eq(10L));
    }
}
