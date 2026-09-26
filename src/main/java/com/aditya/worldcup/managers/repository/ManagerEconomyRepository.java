package com.aditya.worldcup.managers.repository;

import com.aditya.worldcup.managers.entity.ManagerEconomy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ManagerEconomyRepository extends JpaRepository<ManagerEconomy, Long> {
    Optional<ManagerEconomy> findByManagerId(Long managerId);
}
