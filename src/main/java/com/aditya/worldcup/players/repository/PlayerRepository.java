package com.aditya.worldcup.players.repository;

import com.aditya.worldcup.players.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByCountryId(Long countryId);

}