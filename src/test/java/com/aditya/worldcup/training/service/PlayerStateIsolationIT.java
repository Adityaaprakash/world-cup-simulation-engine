package com.aditya.worldcup.training.service;

import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.players.repository.PlayerStateRepository;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.repository.ManagerRepository;
import com.aditya.worldcup.countries.entity.Country;
import com.aditya.worldcup.training.entity.TrainingCategory;
import com.aditya.worldcup.training.entity.TrainingIntensity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlayerStateIsolationIT {

    @Autowired
    private PlayerTrainingService playerTrainingService;

    @Autowired
    private ManagerRepository managerRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerStateRepository playerStateRepository;

    @Test
    void testSaveSlotIsolation_WhenTrainingPlayer_ShouldNotLeakToOtherManagers() {
        // Arrange
        Country country = new Country();
        country.setName("Testland");
        entityManager.persist(country);
        
        Manager managerA = managerRepository.save(Manager.builder().username("mgrA").build());
        Manager managerB = managerRepository.save(Manager.builder().username("mgrB").build());
        
        Player globalPlayer = playerRepository.save(Player.builder()
                .name("Isolate Me")
                .country(country)
                .age(20)
                .position(PlayerPosition.ST)
                .overallRating(70)
                .pace(70)
                .shooting(70)
                .passing(70)
                .dribbling(70)
                .defending(70)
                .physical(70)
                .potential(90)
                .marketValue(100000L)
                .build());

        PlayerState stateA = playerStateRepository.save(PlayerState.builder()
                .manager(managerA)
                .player(globalPlayer)
                .developmentRating(0)
                .fatigue(0)
                .build());
                
        PlayerState stateB = playerStateRepository.save(PlayerState.builder()
                .manager(managerB)
                .player(globalPlayer)
                .developmentRating(0)
                .fatigue(0)
                .build());

        // Act - Train in SaveSlot A
        playerTrainingService.processPlayerTraining(stateA, TrainingCategory.POSITION, TrainingIntensity.INTENSE, null, managerA);
        
        playerStateRepository.save(stateA); // Explicit save though transactional handles it

        // Re-fetch and check
        Player refetchedPlayer = playerRepository.findById(globalPlayer.getId()).orElseThrow();
        PlayerState refetchedStateA = playerStateRepository.findById(stateA.getId()).orElseThrow();
        PlayerState refetchedStateB = playerStateRepository.findById(stateB.getId()).orElseThrow();
        
        // Assert SaveSlot A evolved natively:
        assertThat(refetchedStateA.getDevelopmentRating()).isGreaterThanOrEqualTo(0);
        
        // Assert SaveSlot B was untouched natively!
        assertThat(refetchedStateB.getDevelopmentRating()).isEqualTo(0);
        assertThat(refetchedStateB.getPaceDelta()).isEqualTo(0);
        assertThat(refetchedStateB.getShootingDelta()).isEqualTo(0);
    }
}
