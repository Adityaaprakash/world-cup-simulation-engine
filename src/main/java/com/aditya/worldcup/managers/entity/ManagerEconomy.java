package com.aditya.worldcup.managers.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "manager_economy")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerEconomy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "manager_id", nullable = false, unique = true)
    private Manager manager;

    @Column(nullable = false)
    private Integer balance;

    @Column(nullable = false)
    private Integer trainingAllocation;

    @Column(nullable = false)
    private Integer medicalAllocation;

    @Column(nullable = false)
    private Integer scoutingAllocation;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
