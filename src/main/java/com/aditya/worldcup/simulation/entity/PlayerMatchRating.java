package com.aditya.worldcup.simulation.entity;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.players.entity.Player;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_match_ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerMatchRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    private Double rating;
}
