package com.aditya.worldcup.players.entity;

import com.aditya.worldcup.managers.entity.Manager;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Builder.Default
    @Column(nullable = false)
    private Integer currentForm = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer confidence = 50;

    @Builder.Default
    @Column(nullable = false)
    private Integer fitness = 100;

    @Builder.Default
    @Column(nullable = false)
    private Integer fatigue = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer workload = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer morale = 50;

    @Builder.Default
    @Column(nullable = false)
    private Integer yellowCards = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer redCardSuspension = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InjuryStatus injuryStatus = InjuryStatus.HEALTHY;

    @Builder.Default
    @Column(nullable = false)
    private Integer injuryMatchesRemaining = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer developmentRating = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer progressionTracker = 0;

    // --- Phase 14C: Isolated Capability Deltas ---
    @Builder.Default
    @Column(nullable = false)
    private Integer paceDelta = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer shootingDelta = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer passingDelta = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer dribblingDelta = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer defendingDelta = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer physicalDelta = 0;
}
