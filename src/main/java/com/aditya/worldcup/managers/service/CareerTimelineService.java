package com.aditya.worldcup.managers.service;

import com.aditya.worldcup.managers.dto.CareerTimelineResponse;
import com.aditya.worldcup.managers.entity.CareerTimelineEvent;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.entity.TimelineEventType;
import com.aditya.worldcup.managers.repository.CareerTimelineEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CareerTimelineService {

    private final CareerTimelineEventRepository timelineEventRepository;
    private final ManagerService managerService;

    @Transactional
    public List<CareerTimelineResponse> getCurrentTimeline(
            Authentication authentication) {

        Manager manager = managerService.getOrCreateManager(authentication);
        return timelineEventRepository
                .findByManagerIdOrderByOccurredAtDesc(manager.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void recordEvent(
            Manager manager,
            TimelineEventType eventType,
            String title,
            String description,
            Long tournamentId,
            Long teamId
    ) {
        timelineEventRepository.save(CareerTimelineEvent.builder()
                .manager(manager)
                .eventType(eventType)
                .title(title)
                .description(description)
                .tournamentId(tournamentId)
                .teamId(teamId)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private CareerTimelineResponse mapToResponse(
            CareerTimelineEvent event) {

        return new CareerTimelineResponse(
                event.getId(),
                event.getEventType(),
                event.getTitle(),
                event.getDescription(),
                event.getTournamentId(),
                event.getTeamId(),
                event.getOccurredAt()
        );
    }
}
