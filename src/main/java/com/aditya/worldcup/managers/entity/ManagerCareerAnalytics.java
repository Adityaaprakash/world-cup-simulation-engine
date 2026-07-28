package com.aditya.worldcup.managers.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "manager_career_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerCareerAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false, unique = true)
    private Manager manager;

    @Column(nullable = false)
    private Double winPercentage;

    @Column(nullable = false)
    private Double averageGoalsScored;

    @Column(nullable = false)
    private Double averageGoalsConceded;

    @Column(nullable = false)
    private Double averagePossession;

    @Column(nullable = false, length = 50)
    private String favoriteFormation;

    @Column(nullable = false, length = 80)
    private String favoriteTactics;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CoachingStyle tacticalProfile;

    @Column(nullable = false, length = 500)
    private String mostUsedLineup;

    @Column(nullable = false, length = 120)
    private String mostSelectedCaptain;

    @Column(nullable = false, length = 500)
    private String mostTrustedPlayers;

    @Column(nullable = false)
    private Integer longestUnbeatenStreak;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
