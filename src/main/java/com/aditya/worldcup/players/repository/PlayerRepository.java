package com.aditya.worldcup.players.repository;

import com.aditya.worldcup.players.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long>, JpaSpecificationExecutor<Player> {

    @Override
    @EntityGraph(attributePaths = "country")
    Page<Player> findAll(Specification<Player> specification, Pageable pageable);

    List<Player> findByCountryId(Long countryId);

    List<Player> findByCountryIdAndActiveTrueAndRetiredFalse(Long countryId);

    long countByActiveTrueAndRetiredFalse();

    long countByActiveFalseAndRetiredFalse();

    long countByRetiredTrue();

}
