package com.aditya.worldcup.tournamentteams.entity;

import com.aditya.worldcup.groups.entity.Group;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.tournaments.entity.Tournament;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tournament_teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = true)
    private Group group;

    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private Integer seed;
}
