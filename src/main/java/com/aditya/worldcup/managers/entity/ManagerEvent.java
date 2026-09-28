package com.aditya.worldcup.managers.entity;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.tournaments.entity.Tournament;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "manager_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ManagerEventType type;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "context_id", nullable = false, length = 100)
    private String contextId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ManagerEventStatus status;

    @Column(length = 50)
    private String selectedDecision;

    @Column(length = 500)
    private String resolutionText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_player_id")
    private Player relatedPlayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_match_id")
    private Match relatedMatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_tournament_id")
    private Tournament relatedTournament;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;
    
    private LocalDateTime resolvedAt;
}
