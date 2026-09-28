package com.aditya.worldcup.managers.service;

import com.aditya.worldcup.managers.dto.ManagerEventResponse;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.entity.ManagerEvent;
import com.aditya.worldcup.managers.entity.ManagerEventStatus;
import com.aditya.worldcup.managers.entity.ManagerEventType;
import com.aditya.worldcup.managers.repository.ManagerEventRepository;
import com.aditya.worldcup.saves.context.SaveContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerEventServiceTest {

    @Mock
    private ManagerEventRepository eventRepository;

    @Mock
    private ManagerConsequenceService consequenceService;

    @InjectMocks
    private ManagerEventService eventService;

    private Manager manager;
    private ManagerEvent defaultEvent;

    @BeforeEach
    void setUp() {
        SaveContextHolder.setManagerId(1L);
        manager = Manager.builder().id(1L).build();
        
        defaultEvent = ManagerEvent.builder()
                .id(100L)
                .manager(manager)
                .type(ManagerEventType.FEDERATION_RESOURCE_GRANT)
                .status(ManagerEventStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        SaveContextHolder.clear();
    }

    @Test
    void testMakeDecision_Success() {
        when(eventRepository.findByIdAndManagerId(100L, 1L)).thenReturn(Optional.of(defaultEvent));
        when(eventRepository.save(any(ManagerEvent.class))).thenReturn(defaultEvent);
        when(consequenceService.applyDecisionConsequences(defaultEvent)).thenReturn("Added funds");

        ManagerEventResponse response = eventService.makeDecision(100L, "ALLOCATE_TRAINING");

        assertThat(response.status()).isEqualTo(ManagerEventStatus.RESOLVED);
        assertThat(response.selectedDecision()).isEqualTo("ALLOCATE_TRAINING");
        assertThat(response.resolutionText()).isEqualTo("Added funds");
        verify(eventRepository, times(2)).save(defaultEvent); // Once for deciding, once for resolving
    }

    @Test
    void testMakeDecision_RejectsInvalidDecision() {
        when(eventRepository.findByIdAndManagerId(100L, 1L)).thenReturn(Optional.of(defaultEvent));

        assertThatThrownBy(() -> eventService.makeDecision(100L, "INVALID_CODE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid decision code");
                
        verify(eventRepository, never()).save(any());
    }

    @Test
    void testMakeDecision_RejectsAlreadyResolved() {
        defaultEvent.setStatus(ManagerEventStatus.RESOLVED);
        when(eventRepository.findByIdAndManagerId(100L, 1L)).thenReturn(Optional.of(defaultEvent));

        assertThatThrownBy(() -> eventService.makeDecision(100L, "ALLOCATE_TRAINING"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not pending");
    }

    @Test
    void testMakeDecision_RejectsExpired() {
        defaultEvent.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(eventRepository.findByIdAndManagerId(100L, 1L)).thenReturn(Optional.of(defaultEvent));

        assertThatThrownBy(() -> eventService.makeDecision(100L, "ALLOCATE_TRAINING"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
                
        assertThat(defaultEvent.getStatus()).isEqualTo(ManagerEventStatus.EXPIRED);
    }
    
    @Test
    void testManagerIsolation_CannotDecideOtherManagersEvent() {
        when(eventRepository.findByIdAndManagerId(100L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.makeDecision(100L, "ALLOCATE_TRAINING"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event not found");
    }
}
