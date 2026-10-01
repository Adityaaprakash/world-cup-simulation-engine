package com.aditya.worldcup.tactics.entity;

import com.aditya.worldcup.teams.entity.Team;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tactical_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TacticalProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false, unique = true)
    private Team team;

    @Builder.Default @Enumerated(EnumType.STRING)
    private PressingIntensity pressingIntensity = PressingIntensity.BALANCED;
    @Builder.Default @Enumerated(EnumType.STRING)
    private DefensiveLine defensiveLine = DefensiveLine.BALANCED;
    @Builder.Default @Enumerated(EnumType.STRING)
    private Tempo tempo = Tempo.BALANCED;
    @Builder.Default @Enumerated(EnumType.STRING)
    private Width width = Width.BALANCED;
    @Builder.Default @Enumerated(EnumType.STRING)
    private PassingStyle passingStyle = PassingStyle.MIXED;
    @Builder.Default @Enumerated(EnumType.STRING)
    private AttackingApproach attackingApproach = AttackingApproach.BALANCED;
    @Builder.Default @Enumerated(EnumType.STRING)
    private BuildUpStyle buildUpStyle = BuildUpStyle.BALANCED;
    @Builder.Default @Enumerated(EnumType.STRING)
    private DefensiveBlock defensiveBlock = DefensiveBlock.MID_BLOCK;

    public static TacticalProfile balanced(Team team) {
        return TacticalProfile.builder().team(team).build();
    }
}
