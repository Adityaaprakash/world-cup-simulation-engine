package com.aditya.worldcup.contracts.entity;

import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.players.entity.Player;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "player_contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContractStatus status;

    @Column(nullable = false)
    private Integer startSeason;

    @Column(nullable = false)
    private Integer expirySeason;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommitmentLevel commitmentLevel;

    @Builder.Default
    @Column(nullable = false)
    private Integer renewalCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime terminatedAt;
}
