package com.aditya.worldcup.contracts.repository;

import com.aditya.worldcup.contracts.entity.ContractStatus;
import com.aditya.worldcup.contracts.entity.PlayerContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerContractRepository extends JpaRepository<PlayerContract, Long> {
    
    Optional<PlayerContract> findByPlayerIdAndManagerIdAndStatusIn(Long playerId, Long managerId, List<ContractStatus> statuses);
    
    List<PlayerContract> findByManagerId(Long managerId);
    
    List<PlayerContract> findByManagerIdAndStatus(Long managerId, ContractStatus status);

    List<PlayerContract> findByPlayerId(Long playerId);
    
    List<PlayerContract> findByManagerIdAndExpirySeason(Long managerId, Integer expirySeason);
}
