package com.aditya.worldcup.matches.entity;

import com.aditya.worldcup.groups.entity.Group;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.tournaments.entity.Tournament;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    @ManyToOne
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;

    private Integer homeScore;

    private Integer awayScore;

    private LocalDateTime matchDate;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;
}