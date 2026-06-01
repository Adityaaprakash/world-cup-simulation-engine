package com.aditya.worldcup.teams.entity;

import com.aditya.worldcup.countries.entity.Country;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(
            name = "country_id",
            nullable = false,
            unique = true
    )
    private Country country;

    @Column(nullable = false)
    private String name;

    private String manager;

    @Column(nullable = false)
    private Integer overallRating;
}