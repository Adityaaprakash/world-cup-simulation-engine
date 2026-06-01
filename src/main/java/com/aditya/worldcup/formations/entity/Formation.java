package com.aditya.worldcup.formations.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "formations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private Integer defenders;

    @Column(nullable = false)
    private Integer midfielders;

    @Column(nullable = false)
    private Integer attackers;
}