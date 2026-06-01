package com.aditya.worldcup.players.entity;

import com.aditya.worldcup.countries.entity.Country;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerPosition position;

    @Column(nullable = false)
    private Integer overallRating;

    @Column(nullable = false)
    private Integer pace;

    @Column(nullable = false)
    private Integer shooting;

    @Column(nullable = false)
    private Integer passing;

    @Column(nullable = false)
    private Integer dribbling;

    @Column(nullable = false)
    private Integer defending;

    @Column(nullable = false)
    private Integer physical;

    @Column(nullable = false)
    private Integer potential;

    @Column(nullable = false)
    private Long marketValue;
}