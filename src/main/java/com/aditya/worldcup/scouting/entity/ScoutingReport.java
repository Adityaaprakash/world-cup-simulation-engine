package com.aditya.worldcup.scouting.entity;

import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.players.entity.Player;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "scouting_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoutingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scout_id", nullable = false)
    private Scout scout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReportStatus status;

    @Column(nullable = false)
    @Builder.Default
    private Integer progress = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private KnowledgeLevel knowledgeLevel;

    private Integer estimatedOverallMin;
    private Integer estimatedOverallMax;

    private Integer estimatedPotentialMin;
    private Integer estimatedPotentialMax;

    @Column(nullable = false)
    @Builder.Default
    private Integer confidence = 0;

    private Integer tacticalCompatibility;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ScoutRecommendation recommendation;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}
