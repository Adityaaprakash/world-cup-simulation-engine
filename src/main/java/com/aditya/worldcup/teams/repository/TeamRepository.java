package com.aditya.worldcup.teams.repository;

import com.aditya.worldcup.teams.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface TeamRepository extends JpaRepository<Team, Long>, JpaSpecificationExecutor<Team> {

    @Override
    @EntityGraph(attributePaths = "country")
    Page<Team> findAll(Specification<Team> specification, Pageable pageable);

    long countByActiveTrue();

    long countByActiveFalse();
}
