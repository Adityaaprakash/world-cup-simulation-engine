package com.aditya.worldcup.players.entity;

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

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false, unique = true)
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
}
