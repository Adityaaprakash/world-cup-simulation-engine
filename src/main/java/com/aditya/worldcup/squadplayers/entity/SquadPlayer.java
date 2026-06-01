package com.aditya.worldcup.squadplayers.entity;

import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.squads.entity.Squad;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "squad_players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SquadPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "squad_id", nullable = false)
    private Squad squad;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private String positionSlot;

    @Column(nullable = false)
    private Boolean startingXi;

    @Column(nullable = false)
    private Boolean captain;
}