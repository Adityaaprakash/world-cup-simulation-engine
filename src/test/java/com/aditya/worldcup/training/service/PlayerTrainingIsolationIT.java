package com.aditya.worldcup.training.service;

import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.repository.ManagerRepository;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.players.service.PlayerStateService;
import com.aditya.worldcup.saves.context.SaveContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlayerTrainingIsolationIT {

    @Autowired
    private PlayerStateService playerStateService;

    @Autowired
    private ManagerRepository managerRepository;

    @Autowired
    private PlayerRepository playerRepository;

    private Manager managerA;
    private Manager managerB;
    private Player commonPlayer;

    @BeforeEach
    void setUp() {
        managerA = managerRepository.save(Manager.builder()
                .username("managerA")
                .level(1)
                .build());

        managerB = managerRepository.save(Manager.builder()
                .username("managerB")
                .level(1)
                .build());

        commonPlayer = playerRepository.findAll().stream().findFirst()
                .orElseGet(() -> playerRepository.save(Player.builder()
                        .name("Isolated Player")
                        .overallRating(80)
                        .position(PlayerPosition.CM)
                        .build()));
    }

    @AfterEach
    void tearDown() {
        SaveContextHolder.clear();
    }

    @Test
    void testCrossManagerIsolation() {
        // Manager A context
        SaveContextHolder.setManagerId(managerA.getId());
        PlayerState stateA = playerStateService.getOrCreateState(commonPlayer);
        stateA.setDevelopmentRating(5);
        stateA.setFatigue(30);

        // Manager B context
        SaveContextHolder.setManagerId(managerB.getId());
        PlayerState stateB = playerStateService.getOrCreateState(commonPlayer);
        
        // Assert Manager B sees a fresh state, not Manager A's
        assertThat(stateB.getDevelopmentRating()).isEqualTo(0);
        assertThat(stateB.getFatigue()).isEqualTo(0);
        
        // Manager B modifies state
        stateB.setDevelopmentRating(-2);
        stateB.setFatigue(100);

        // Switch back to Manager A
        SaveContextHolder.setManagerId(managerA.getId());
        PlayerState stateAReloaded = playerStateService.getOrCreateState(commonPlayer);
        
        // Assert Manager A's state is preserved and not leaked from Manager B
        assertThat(stateAReloaded.getDevelopmentRating()).isEqualTo(5);
        assertThat(stateAReloaded.getFatigue()).isEqualTo(30);
    }
}
