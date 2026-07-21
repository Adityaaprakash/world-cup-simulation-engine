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

    @Builder.Default private Integer attackWidth = 50;
    @Builder.Default private Integer defensiveWidth = 50;
    @Builder.Default private Integer defensiveLine = 50;
    @Builder.Default private Integer pressingIntensity = 50;
    @Builder.Default @Enumerated(EnumType.STRING)
    private BuildUpStyle buildUpStyle = BuildUpStyle.BALANCED;
    @Builder.Default @Enumerated(EnumType.STRING)
    private ChanceCreation chanceCreation = ChanceCreation.BALANCED;
    @Builder.Default private Integer attackingWidth = 50;
    @Builder.Default private Integer crossFrequency = 50;
    @Builder.Default private Integer longBallFrequency = 50;
    @Builder.Default private Integer passingRisk = 50;
    @Builder.Default private Boolean counterAttack = false;
    @Builder.Default private Boolean highPress = false;
    @Builder.Default private Boolean offsideTrap = false;
    @Builder.Default private Boolean timeWasting = false;

    public static TacticalProfile balanced(Team team) {
        return TacticalProfile.builder().team(team).build();
    }
}
