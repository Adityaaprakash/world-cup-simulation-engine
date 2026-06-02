package com.aditya.worldcup.standings.entity;

import com.aditya.worldcup.groups.entity.Group;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.tournaments.entity.Tournament;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "standings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Standing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Tournament tournament;

    @ManyToOne
    private Group group;

    @ManyToOne
    private Team team;

    private Integer played;
    private Integer won;
    private Integer drawn;
    private Integer lost;

    private Integer goalsFor;
    private Integer goalsAgainst;
    private Integer goalDifference;

    private Integer points;
}