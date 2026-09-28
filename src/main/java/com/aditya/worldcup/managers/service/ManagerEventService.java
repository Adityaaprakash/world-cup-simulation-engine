package com.aditya.worldcup.managers.service;

import com.aditya.worldcup.managers.dto.DecisionOptionResponse;
import com.aditya.worldcup.managers.dto.ManagerEventResponse;
import com.aditya.worldcup.managers.entity.ManagerEvent;
import com.aditya.worldcup.managers.entity.ManagerEventStatus;
import com.aditya.worldcup.managers.entity.ManagerEventType;
import com.aditya.worldcup.managers.repository.ManagerEventRepository;
import com.aditya.worldcup.saves.context.SaveContextHolder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerEventService {

    private final ManagerEventRepository eventRepository;
    private final ManagerConsequenceService consequenceService;

    public List<ManagerEventResponse> getManagerEvents() {
        Long managerId = SaveContextHolder.getManagerId();
        return eventRepository.findByManagerIdOrderByCreatedAtDesc(managerId).stream()
                .map(this::mapToResponse)
                .toList();
    }
    
    public List<ManagerEventResponse> getPendingEvents() {
        Long managerId = SaveContextHolder.getManagerId();
        return eventRepository.findByManagerIdAndStatusOrderByCreatedAtDesc(managerId, ManagerEventStatus.PENDING).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ManagerEventResponse getEvent(Long eventId) {
        Long managerId = SaveContextHolder.getManagerId();
        ManagerEvent event = eventRepository.findByIdAndManagerId(eventId, managerId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found or not owned by current manager"));
        return mapToResponse(event);
    }

    @Transactional
    public ManagerEventResponse makeDecision(Long eventId, String decisionCode) {
        Long managerId = SaveContextHolder.getManagerId();
        ManagerEvent event = eventRepository.findByIdAndManagerId(eventId, managerId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (event.getStatus() != ManagerEventStatus.PENDING) {
            throw new IllegalStateException("Event is not pending");
        }
        
        if (event.getExpiresAt() != null && event.getExpiresAt().isBefore(LocalDateTime.now())) {
            event.setStatus(ManagerEventStatus.EXPIRED);
            eventRepository.save(event);
            throw new IllegalStateException("Event has expired");
        }

        List<DecisionOptionResponse> validOptions = getOptionsForType(event.getType());
        boolean isValid = validOptions.stream().anyMatch(opt -> opt.code().equals(decisionCode));
        if (!isValid) {
            throw new IllegalArgumentException("Invalid decision code for this event type");
        }

        event.setSelectedDecision(decisionCode);
        event.setStatus(ManagerEventStatus.DECIDED);
        eventRepository.save(event);

        return resolveEventInternal(event);
    }

    @Transactional
    public ManagerEventResponse resolveEventInternal(ManagerEvent event) {
        if (event.getStatus() != ManagerEventStatus.DECIDED) {
            throw new IllegalStateException("Event must be decided before resolution");
        }

        String resolutionText = consequenceService.applyDecisionConsequences(event);
        
        event.setResolutionText(resolutionText);
        event.setStatus(ManagerEventStatus.RESOLVED);
        event.setResolvedAt(LocalDateTime.now());
        
        return mapToResponse(eventRepository.save(event));
    }
    
    @Transactional
    public void generateEventIfNotExists(ManagerEvent event) {
        if (eventRepository.findByManagerIdAndContextId(event.getManager().getId(), event.getContextId()).isEmpty()) {
            eventRepository.save(event);
            eventRepository.save(event);
        }
    }

    @Transactional
    public void resolvePendingEventsForAi(com.aditya.worldcup.managers.entity.Manager manager) {
        List<ManagerEvent> pendingEvents = eventRepository.findByManagerIdAndStatusOrderByCreatedAtDesc(
                manager.getId(), ManagerEventStatus.PENDING);
                
        for (ManagerEvent event : pendingEvents) {
            if (event.getExpiresAt() != null && event.getExpiresAt().isBefore(LocalDateTime.now())) {
                event.setStatus(ManagerEventStatus.EXPIRED);
                eventRepository.save(event);
                continue;
            }
            
            List<DecisionOptionResponse> validOptions = getOptionsForType(event.getType());
            if (!validOptions.isEmpty()) {
                // AI picks the first option arbitrarily to keep it simple, or random in a real engine
                String decision = validOptions.get(0).code();
                event.setSelectedDecision(decision);
                event.setStatus(ManagerEventStatus.DECIDED);
                eventRepository.save(event);
                resolveEventInternal(event);
            }
        }
    }

    private ManagerEventResponse mapToResponse(ManagerEvent event) {
        return new ManagerEventResponse(
                event.getId(),
                event.getType(),
                event.getTitle(),
                event.getDescription(),
                event.getStatus(),
                event.getSelectedDecision(),
                event.getResolutionText(),
                event.getCreatedAt(),
                event.getExpiresAt(),
                event.getResolvedAt(),
                event.getRelatedPlayer() != null ? event.getRelatedPlayer().getId() : null,
                event.getRelatedMatch() != null ? event.getRelatedMatch().getId() : null,
                event.getRelatedTournament() != null ? event.getRelatedTournament().getId() : null,
                event.getStatus() == ManagerEventStatus.PENDING ? getOptionsForType(event.getType()) : List.of()
        );
    }

    public List<DecisionOptionResponse> getOptionsForType(ManagerEventType type) {
        List<DecisionOptionResponse> options = new ArrayList<>();
        switch (type) {
            case PLAYER_FATIGUE_WARNING -> {
                options.add(new DecisionOptionResponse("REST_PLAYER", "Rest Player immediately", "Player rests and recovers fitness."));
                options.add(new DecisionOptionResponse("REDUCE_INTENSITY", "Reduce training intensity", "Modest recovery without completely stopping."));
                options.add(new DecisionOptionResponse("PLAY_THROUGH", "Push through it", "Player continues but risks severe fatigue/injury."));
            }
            case PLAYER_TRAINING_BREAKTHROUGH -> {
                options.add(new DecisionOptionResponse("PRAISE_PUBLICLY", "Praise publicly", "Boosts morale and reputation, accelerates short-term growth."));
                options.add(new DecisionOptionResponse("FOCUS_DEVELOPMENT", "Focus on core skills", "Adds long term development focus."));
            }
            case FEDERATION_RESOURCE_GRANT -> {
                options.add(new DecisionOptionResponse("ALLOCATE_TRAINING", "Invest in Training", "Gain +500 Training Allocation."));
                options.add(new DecisionOptionResponse("ALLOCATE_MEDICAL", "Invest in Medical", "Gain +500 Medical Allocation."));
                options.add(new DecisionOptionResponse("ALLOCATE_SCOUTING", "Invest in Scouting", "Gain +500 Scouting Allocation."));
            }
            case SQUAD_SELECTION_CONFLICT -> {
                options.add(new DecisionOptionResponse("BACK_VETERAN", "Back the veteran", "Maintains squad stability."));
                options.add(new DecisionOptionResponse("PROMOTE_YOUTH", "Promote young talent", "Boosts youth development."));
            }
            case TACTICAL_PREPARATION_DILEMMA -> {
                options.add(new DecisionOptionResponse("DEFENSIVE_PREP", "Focus on defense", "Boosts defensive readiness."));
                options.add(new DecisionOptionResponse("ATTACKING_PREP", "Focus on attack", "Boosts attacking readiness."));
            }
        }
        return options;
    }
}
