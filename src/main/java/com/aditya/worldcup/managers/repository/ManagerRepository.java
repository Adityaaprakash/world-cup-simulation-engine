package com.aditya.worldcup.managers.repository;

import com.aditya.worldcup.managers.entity.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ManagerRepository extends JpaRepository<Manager, Long>, JpaSpecificationExecutor<Manager> {

    Optional<Manager> findByUsername(String username);
}
