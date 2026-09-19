package com.aditya.worldcup.contracts.service;

import com.aditya.worldcup.managers.entity.CareerTimelineEvent;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.entity.TimelineEventType;
import com.aditya.worldcup.managers.repository.CareerTimelineEventRepository;
import com.aditya.worldcup.managers.service.ManagerService;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.repository.PlayerRepository;
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
class PlayerLifecycleServiceTest {

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private ManagerService managerService;
    @Mock
    private CareerTimelineEventRepository careerTimelineEventRepository;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private PlayerLifecycleService playerLifecycleService;

    private Manager manager;
    private Player player;

    @BeforeEach
    void setUp() {
        manager = new Manager();
        manager.setId(1L);

        player = new Player();
        player.setId(10L);
        player.setName("Luka Modric");
        player.setActive(true);
        player.setRetired(false);
    }

    @Test
    void retirePlayer_activePlayer_success() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(playerRepository.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));

        Player result = playerLifecycleService.retirePlayer(10L, authentication);

        assertThat(result.getRetired()).isTrue();
        assertThat(result.getActive()).isFalse();

        ArgumentCaptor<CareerTimelineEvent> timelineCaptor = ArgumentCaptor.forClass(CareerTimelineEvent.class);
        verify(careerTimelineEventRepository).save(timelineCaptor.capture());
        CareerTimelineEvent event = timelineCaptor.getValue();
        
        assertThat(event.getEventType()).isEqualTo(TimelineEventType.PLAYER_RETIRED);
        assertThat(event.getTitle()).isEqualTo("Player Retired");
        assertThat(event.getDescription()).contains("has retired");
    }

    @Test
    void retirePlayer_alreadyRetired_throwsException() {
        player.setRetired(true);
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));

        assertThrows(IllegalStateException.class, () -> playerLifecycleService.retirePlayer(10L, authentication));
    }

    @Test
    void retirePlayer_missingPlayer_throwsException() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        when(playerRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> playerLifecycleService.retirePlayer(10L, authentication));
    }

    @Test
    void reactivatePlayer_retiredPlayer_success() {
        player.setRetired(true);
        player.setActive(false);
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(playerRepository.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));

        Player result = playerLifecycleService.reactivatePlayer(10L, authentication);

        assertThat(result.getRetired()).isFalse();
        assertThat(result.getActive()).isTrue();

        ArgumentCaptor<CareerTimelineEvent> timelineCaptor = ArgumentCaptor.forClass(CareerTimelineEvent.class);
        verify(careerTimelineEventRepository).save(timelineCaptor.capture());
        CareerTimelineEvent event = timelineCaptor.getValue();
        
        assertThat(event.getEventType()).isEqualTo(TimelineEventType.PLAYER_REACTIVATED);
        assertThat(event.getTitle()).isEqualTo("Player Reactivated");
        assertThat(event.getDescription()).contains("come out of retirement");
    }

    @Test
    void reactivatePlayer_notRetired_throwsException() {
        player.setRetired(false);
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));

        assertThrows(IllegalStateException.class, () -> playerLifecycleService.reactivatePlayer(10L, authentication));
    }
}
