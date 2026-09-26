package com.aditya.worldcup.managers.service;

import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.entity.ManagerEconomy;
import com.aditya.worldcup.managers.entity.ResourceTransaction;
import com.aditya.worldcup.managers.repository.ManagerEconomyRepository;
import com.aditya.worldcup.managers.repository.ManagerRepository;
import com.aditya.worldcup.managers.repository.ResourceTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ManagerEconomyService {

    private final ManagerEconomyRepository managerEconomyRepository;
    private final ResourceTransactionRepository resourceTransactionRepository;

    @Transactional
    public ManagerEconomy getOrCreateEconomy(Manager manager) {
        return managerEconomyRepository.findByManagerId(manager.getId())
                .orElseGet(() -> {
                    ManagerEconomy economy = ManagerEconomy.builder()
                            .manager(manager)
                            .balance(100)
                            .trainingAllocation(0)
                            .medicalAllocation(0)
                            .scoutingAllocation(0)
                            .updatedAt(LocalDateTime.now())
                            .build();
                    economy = managerEconomyRepository.save(economy);
                    recordTransaction(manager, 100, "Initial Federation Budget", null);
                    return economy;
                });
    }

    @Transactional
    public void addFunds(Manager manager, int amount, String reason, String idempotencyKey) {
        if (amount <= 0) return;
        if (idempotencyKey != null && resourceTransactionRepository.existsByManagerIdAndIdempotencyKey(manager.getId(), idempotencyKey)) {
            return; // Duplicate tournament reward or double spend
        }
        ManagerEconomy economy = getOrCreateEconomy(manager);
        economy.setBalance(economy.getBalance() + amount);
        economy.setUpdatedAt(LocalDateTime.now());
        managerEconomyRepository.save(economy);
        recordTransaction(manager, amount, reason, idempotencyKey);
    }

    @Transactional
    public void allocateResources(Manager manager, int training, int medical, int scouting) {
        ManagerEconomy economy = getOrCreateEconomy(manager);
        
        if (training < 0 || medical < 0 || scouting < 0) {
            throw new IllegalArgumentException("Allocations cannot be negative");
        }
        
        int totalAllocation = training + medical + scouting;
        if (totalAllocation > economy.getBalance()) {
            throw new IllegalArgumentException("Insufficient resource balance");
        }
        
        economy.setTrainingAllocation(training);
        economy.setMedicalAllocation(medical);
        economy.setScoutingAllocation(scouting);
        economy.setUpdatedAt(LocalDateTime.now());
        
        managerEconomyRepository.save(economy);
    }

    private void recordTransaction(Manager manager, int amount, String reason, String idempotencyKey) {
        ResourceTransaction tx = ResourceTransaction.builder()
                .manager(manager)
                .amount(amount)
                .reason(reason)
                .idempotencyKey(idempotencyKey)
                .transactionDate(LocalDateTime.now())
                .build();
        resourceTransactionRepository.save(tx);
    }
}
