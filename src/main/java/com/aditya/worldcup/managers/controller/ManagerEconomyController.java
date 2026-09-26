package com.aditya.worldcup.managers.controller;

import com.aditya.worldcup.saves.context.SaveContextHolder;
import com.aditya.worldcup.managers.dto.ManagerEconomyAllocateRequest;
import com.aditya.worldcup.managers.dto.ManagerEconomyResponse;
import com.aditya.worldcup.managers.entity.ManagerEconomy;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.service.ManagerEconomyService;
import com.aditya.worldcup.managers.repository.ManagerRepository;
import com.aditya.worldcup.managers.repository.ResourceTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/manager/economy")
@RequiredArgsConstructor
public class ManagerEconomyController {

    private final ManagerEconomyService managerEconomyService;
    private final ResourceTransactionRepository resourceTransactionRepository;
    private final ManagerRepository managerRepository;

    @GetMapping
    public ResponseEntity<ManagerEconomyResponse> getEconomy() {
        Manager manager = managerRepository.findById(SaveContextHolder.getManagerId())
                .orElseThrow(() -> new IllegalArgumentException("Manager not found"));
        ManagerEconomy economy = managerEconomyService.getOrCreateEconomy(manager);

        var transactions = resourceTransactionRepository.findByManagerIdOrderByTransactionDateDesc(manager.getId())
                .stream()
                .limit(20)
                .map(tx -> ManagerEconomyResponse.ResourceTransactionDto.builder()
                        .amount(tx.getAmount())
                        .reason(tx.getReason())
                        .date(tx.getTransactionDate().toString())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ManagerEconomyResponse.builder()
                .balance(economy.getBalance())
                .trainingAllocation(economy.getTrainingAllocation())
                .medicalAllocation(economy.getMedicalAllocation())
                .scoutingAllocation(economy.getScoutingAllocation())
                .recentTransactions(transactions)
                .build());
    }

    @PostMapping("/allocate")
    public ResponseEntity<Void> allocateResources(@RequestBody ManagerEconomyAllocateRequest request) {
        Manager manager = managerRepository.findById(SaveContextHolder.getManagerId())
                .orElseThrow(() -> new IllegalArgumentException("Manager not found"));

        managerEconomyService.allocateResources(
                manager,
                request.getTrainingAllocation() != null ? request.getTrainingAllocation() : 0,
                request.getMedicalAllocation() != null ? request.getMedicalAllocation() : 0,
                request.getScoutingAllocation() != null ? request.getScoutingAllocation() : 0
        );

        return ResponseEntity.ok().build();
    }
}
