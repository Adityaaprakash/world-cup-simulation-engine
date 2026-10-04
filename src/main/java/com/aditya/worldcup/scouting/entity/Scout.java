package com.aditya.worldcup.scouting.entity;

import com.aditya.worldcup.managers.entity.Manager;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer evaluationSkill;

    @Column(nullable = false)
    private Integer potentialEvaluationSkill;

    @Column(nullable = false)
    private Integer tacticalKnowledge;

    @Column(length = 100)
    private String regionSpecialization;
}
