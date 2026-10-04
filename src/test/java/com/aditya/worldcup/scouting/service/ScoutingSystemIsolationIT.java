package com.aditya.worldcup.scouting.service;

import com.aditya.worldcup.WorldcupApplication;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.repository.ManagerRepository;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.scouting.entity.ReportStatus;
import com.aditya.worldcup.scouting.entity.Scout;
import com.aditya.worldcup.scouting.entity.ScoutingReport;
import com.aditya.worldcup.scouting.repository.ScoutRepository;
import com.aditya.worldcup.scouting.repository.ScoutingReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = WorldcupApplication.class)
@ActiveProfiles("test")
@Transactional
public class ScoutingSystemIsolationIT {

    @Autowired
    private ManagerRepository managerRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private ScoutRepository scoutRepository;
    @Autowired
    private ScoutingReportRepository reportRepository;
    @Autowired
    private ScoutingService scoutingService;

    private Manager managerA;
    private Manager managerB;
    private Player testPlayer;
    private Scout scoutA;
    private Scout scoutB;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    void setUp() {
        managerA = managerRepository.save(Manager.builder()
                .username("mgrA")
                .displayName("Mgr A")
                .nationality("England")
                .favoriteFormation("4-4-2")
                .favoriteTacticalProfile("Balanced")
                .coachingStyle(com.aditya.worldcup.managers.entity.CoachingStyle.BALANCED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .experiencePoints(0)
                .level(1)
                .reputation(com.aditya.worldcup.managers.entity.ManagerReputation.AMATEUR)
                .build());

        managerB = managerRepository.save(Manager.builder()
                .username("mgrB")
                .displayName("Mgr B")
                .nationality("Spain")
                .favoriteFormation("4-3-3")
                .favoriteTacticalProfile("Attacking")
                .coachingStyle(com.aditya.worldcup.managers.entity.CoachingStyle.ATTACKING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .experiencePoints(0)
                .level(1)
                .reputation(com.aditya.worldcup.managers.entity.ManagerReputation.AMATEUR)
                .build());

        com.aditya.worldcup.countries.entity.Country country = new com.aditya.worldcup.countries.entity.Country();
        country.setName("Testland");
        country.setFifaCode("TST");
        country.setContinent(com.aditya.worldcup.countries.entity.Continent.EUROPE);
        country.setOverallRating(80);
        country.setFifaRanking(1);
        entityManager.persist(country);

        testPlayer = playerRepository.save(Player.builder()
                .name("Hidden Gem")
                .position(com.aditya.worldcup.players.entity.PlayerPosition.ST)
                .overallRating(70)
                .potential(90)
                .marketValue(1000L)
                .active(true)
                .retired(false)
                .pace(70).shooting(70).passing(70).dribbling(70).defending(70).physical(70)
                .age(18).country(country)
                .build());
    }

    private void mockAuth(String username) {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(username, "pass")
        );
    }

    @Test
    void testScoutingIsolation_CannotSeeOtherManagersAssignments() {
        // Manager A creates a scout and scopes a player
        mockAuth("mgrA");
        scoutA = scoutingService.createScout(managerA, "Scout A", 50, 50, 50, "EU");
        ScoutingReport reportA = scoutingService.assignScout(scoutA.getId(), testPlayer.getId());
        scoutingService.forceCompleteAssignment(reportA.getId());
        
        // Report A should estimate potential terribly because scout has 50 evaluation
        ScoutingReport completedA = reportRepository.findById(reportA.getId()).orElseThrow();
        assertThat(completedA.getManager().getId()).isEqualTo(managerA.getId());
        assertThat(completedA.getStatus()).isEqualTo(ReportStatus.COMPLETED);
        
        // Manager B cannot see it
        mockAuth("mgrB");
        assertThat(reportRepository.findByManagerOrderByCreatedAtDesc(managerB)).isEmpty();
        
        // Manager B hires a fantastic scout
        scoutB = scoutingService.createScout(managerB, "Scout B", 95, 95, 95, "EU");
        ScoutingReport reportB = scoutingService.assignScout(scoutB.getId(), testPlayer.getId());
        scoutingService.forceCompleteAssignment(reportB.getId());
        
        ScoutingReport completedB = reportRepository.findById(reportB.getId()).orElseThrow();
        assertThat(completedB.getManager().getId()).isEqualTo(managerB.getId());
        
        // Comparing uncertainties
        int rangeA = completedA.getEstimatedPotentialMax() - completedA.getEstimatedPotentialMin();
        int rangeB = completedB.getEstimatedPotentialMax() - completedB.getEstimatedPotentialMin();
        
        // Because scout B is way better, their uncertainty interval should be tighter
        assertThat(rangeB).isLessThan(rangeA);
    }
}
