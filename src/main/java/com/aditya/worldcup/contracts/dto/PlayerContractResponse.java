package com.aditya.worldcup.contracts.dto;

import com.aditya.worldcup.contracts.entity.CommitmentLevel;
import com.aditya.worldcup.contracts.entity.ContractStatus;

public record PlayerContractResponse(
        Long id,
        Long playerId,
        Long managerId,
        ContractStatus status,
        Integer startSeason,
        Integer expirySeason,
        CommitmentLevel commitmentLevel,
        Integer renewalCount
) {}
