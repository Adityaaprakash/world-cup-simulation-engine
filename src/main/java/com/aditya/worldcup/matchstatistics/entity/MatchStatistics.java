package com.aditya.worldcup.matchstatistics.entity;

import com.aditya.worldcup.matches.entity.Match;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "match_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "match_id", unique = true, nullable = false)
    private Match match;

    private Integer homePossession;
    private Integer awayPossession;
    private Integer homeShots;
    private Integer awayShots;
    private Integer homeShotsOnTarget;
    private Integer awayShotsOnTarget;
    private Integer homePasses;
    private Integer awayPasses;
    private Integer homePassAccuracy;
    private Integer awayPassAccuracy;
    private Integer homeCorners;
    private Integer awayCorners;
    private Integer homeFouls;
    private Integer awayFouls;
    private Integer homeOffsides;
    private Integer awayOffsides;
    private Integer homeYellowCards;
    private Integer awayYellowCards;
    private Integer homeRedCards;
    private Integer awayRedCards;
    private Integer homeSaves;
    private Integer awaySaves;
    private Double homeExpectedGoals;
    private Double awayExpectedGoals;
}
