package com.aditya.worldcup.saves.service;

import com.aditya.worldcup.managers.entity.*;
import com.aditya.worldcup.saves.dto.SaveExportResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SaveSerializationTest {

    @Test
    void testManagerEconomySerialization() {
        LocalDateTime now = LocalDateTime.now();
        
        ManagerEconomy economy = ManagerEconomy.builder()
                .id(1L)
                .manager(Manager.builder().id(2L).build())
                .balance(500000)
                .trainingAllocation(1000)
                .medicalAllocation(500)
                .scoutingAllocation(250)
                .updatedAt(now)
                .build();
                
        SaveExportResponse.ManagerEconomySnapshot snap = new SaveExportResponse.ManagerEconomySnapshot(
                economy.getId(),
                economy.getManager().getId(),
                economy.getBalance(),
                economy.getTrainingAllocation(),
                economy.getMedicalAllocation(),
                economy.getScoutingAllocation(),
                economy.getUpdatedAt());
                
        assertThat(snap.balance()).isEqualTo(500000);
        assertThat(snap.trainingAllocation()).isEqualTo(1000);
        assertThat(snap.medicalAllocation()).isEqualTo(500);
        assertThat(snap.scoutingAllocation()).isEqualTo(250);
        assertThat(snap.updatedAt()).isEqualTo(now);
    }
    
    @Test
    void testManagerObjectiveSerialization() {
        LocalDateTime now = LocalDateTime.now();
        
        ManagerObjective objective = ManagerObjective.builder()
                .id(100L)
                .manager(Manager.builder().id(2L).build())
                .type(ObjectiveType.WIN_MATCHES)
                .description("Win next match")
                .targetValue(1)
                .currentValue(0)
                .status(ObjectiveStatus.ACTIVE)
                .rewardAmount(10000)
                .createdAt(now)
                .build();
                
        SaveExportResponse.ManagerObjectiveSnapshot snap = new SaveExportResponse.ManagerObjectiveSnapshot(
                objective.getId(),
                objective.getManager().getId(),
                objective.getType(),
                objective.getDescription(),
                objective.getTargetValue(),
                objective.getCurrentValue(),
                objective.getStatus(),
                null,
                objective.getRewardAmount(),
                objective.getCreatedAt(),
                objective.getCompletedAt()
        );
        
        assertThat(snap.objectiveId()).isEqualTo(100L);
        assertThat(snap.type()).isEqualTo(ObjectiveType.WIN_MATCHES);
        assertThat(snap.description()).isEqualTo("Win next match");
        assertThat(snap.targetValue()).isEqualTo(1);
        assertThat(snap.currentValue()).isEqualTo(0);
        assertThat(snap.status()).isEqualTo(ObjectiveStatus.ACTIVE);
        assertThat(snap.rewardAmount()).isEqualTo(10000);
        assertThat(snap.createdAt()).isEqualTo(now);
    }
    
    @Test
    void testResourceTransactionSerialization() {
        LocalDateTime now = LocalDateTime.now();
        
        ResourceTransaction tx = ResourceTransaction.builder()
                .id(50L)
                .manager(Manager.builder().id(2L).build())
                .amount(500)
                .reason("Reward")
                .transactionDate(now)
                .idempotencyKey("TX-123")
                .build();
                
        SaveExportResponse.ResourceTransactionSnapshot snap = new SaveExportResponse.ResourceTransactionSnapshot(
                tx.getId(),
                tx.getManager().getId(),
                tx.getAmount(),
                tx.getReason(),
                tx.getTransactionDate(),
                tx.getIdempotencyKey()
        );
        
        assertThat(snap.amount()).isEqualTo(500);
        assertThat(snap.reason()).isEqualTo("Reward");
        assertThat(snap.idempotencyKey()).isEqualTo("TX-123");
    }
}
