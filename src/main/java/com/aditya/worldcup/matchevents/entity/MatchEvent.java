package com.aditya.worldcup.matchevents.entity;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.players.entity.Player;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "match_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    private Integer minute;

    @Enumerated(EnumType.STRING)
    private MatchEventType eventType;

    private String description;
}