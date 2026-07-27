package com.aditya.worldcup.managers.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "career_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false, unique = true)
    private Manager manager;

    @Column(nullable = false)
    private Integer tournamentsManaged;

    @Column(nullable = false)
    private Integer matchesManaged;

    @Column(nullable = false)
    private Integer wins;

    @Column(nullable = false)
    private Integer draws;

    @Column(nullable = false)
    private Integer losses;

    @Column(nullable = false)
    private Integer goalsScored;

    @Column(nullable = false)
    private Integer goalsConceded;

    @Column(nullable = false)
    private Integer cleanSheets;

    @Column(nullable = false)
    private Integer trophiesWon;

    @Column(nullable = false)
    private Integer finalsReached;

    @Column(nullable = false)
    private Integer semiFinalsReached;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
