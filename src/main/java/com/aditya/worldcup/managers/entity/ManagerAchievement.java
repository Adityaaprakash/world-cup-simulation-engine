package com.aditya.worldcup.managers.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "manager_achievements",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_manager_achievement_code",
                columnNames = {"manager_id", "achievement_code"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;

    @Enumerated(EnumType.STRING)
    @Column(name = "achievement_code", nullable = false, length = 50)
    private AchievementCode achievementCode;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ManagerBadge badge;

    @Column(nullable = false)
    private LocalDateTime unlockedAt;
}
