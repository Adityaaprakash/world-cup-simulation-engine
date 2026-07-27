package com.aditya.worldcup.managers.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "managers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Manager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 80)
    private String nationality;

    @Column(nullable = false, length = 50)
    private String favoriteFormation;

    @Column(nullable = false, length = 80)
    private String favoriteTacticalProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CoachingStyle coachingStyle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ManagerReputation reputation;

    @Column(nullable = false)
    private Integer experiencePoints;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
