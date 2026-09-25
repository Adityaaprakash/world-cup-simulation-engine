package com.aditya.worldcup.tactics.entity;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.squads.entity.Squad;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "match_plans", uniqueConstraints = 
    @UniqueConstraint(columnNames = {"match_id", "squad_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id", nullable = false)
    private Squad squad;

    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private TacticalApproach tacticalApproach = TacticalApproach.BALANCED;

    @Builder.Default
    private Integer pressingIntensity = 50;

    @Builder.Default
    private Integer tempo = 50;

    @Builder.Default
    private Integer defensiveLine = 50;

    @Builder.Default
    private Integer attackingWidth = 50;

    @Builder.Default
    private Boolean counterAttack = false;

    @Builder.Default
    private Boolean offsideTrap = false;
}
