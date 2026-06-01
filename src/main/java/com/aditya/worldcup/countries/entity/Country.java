package com.aditya.worldcup.countries.entity;

import com.aditya.worldcup.players.entity.Player;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "countries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true, length = 3)
    private String fifaCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Continent continent;

    @Column(nullable = false)
    private Integer fifaRanking;

    @Column(nullable = false)
    private Integer overallRating;

    @OneToMany(mappedBy = "country")
    private List<Player> players;
}