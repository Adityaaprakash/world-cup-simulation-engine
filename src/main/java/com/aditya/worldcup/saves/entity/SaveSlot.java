package com.aditya.worldcup.saves.entity;

import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.entity.ManagerReputation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "save_slots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_save_slot_manager_slot_number",
                columnNames = {"manager_id", "slot_number"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;

    @Column(name = "slot_name", nullable = false, length = 100)
    private String slotName;

    @Column(name = "slot_number", nullable = false)
    private Integer slotNumber;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaveType saveType;

    @Column
    private Long currentTournamentId;

    @Column(nullable = false)
    private Integer currentSeason;

    @Column(nullable = false, length = 80)
    private String currentStage;

    @Column(nullable = false)
    private Long totalPlayTime;

    @Column(nullable = false, length = 20)
    private String formatVersion;

    @Column(nullable = false)
    private Integer managerLevel;

    @Column(nullable = false)
    private Integer managerExperiencePoints;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ManagerReputation reputation;

    @Column(nullable = false)
    private Integer tournamentsPlayed;

    @Column(nullable = false)
    private Integer trophies;

    @Column(nullable = false, length = 120)
    private String currentTeam;

    @Column(nullable = false, length = 120)
    private String currentTournament;

    @Column(nullable = false)
    private Double progressPercentage;

    @Column(nullable = false)
    private LocalDateTime latestSaveTimestamp;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private LocalDateTime lastPlayedAt;

    @Column(nullable = false)
    private Boolean autosave;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private Boolean backupAvailable;

    @Column
    private LocalDateTime backupCreatedAt;

    @Column(length = 500)
    private String backupDescription;
}
