package com.aditya.worldcup.contracts.service;

import com.aditya.worldcup.contracts.dto.PlayerContractResponse;
import com.aditya.worldcup.contracts.entity.CommitmentLevel;
import com.aditya.worldcup.contracts.entity.ContractStatus;
import com.aditya.worldcup.contracts.entity.PlayerContract;
import com.aditya.worldcup.contracts.repository.PlayerContractRepository;
import com.aditya.worldcup.managers.entity.CareerTimelineEvent;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.entity.TimelineEventType;
import com.aditya.worldcup.managers.repository.CareerTimelineEventRepository;
import com.aditya.worldcup.managers.service.ManagerService;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.saves.repository.SaveSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service to manage player contracts.
 */
@Service
@RequiredArgsConstructor
public class PlayerContractService {

    private final PlayerContractRepository playerContractRepository;
    private final PlayerRepository playerRepository;
    private final ManagerService managerService;
    private final SaveSlotRepository saveSlotRepository;
    private final CareerTimelineEventRepository careerTimelineEventRepository;
    private final com.aditya.worldcup.contracts.repository.PlayerLifecycleRepository playerLifecycleRepository;

    /**
     * Create a new contract for a player.
     * @param playerId the player to contract
     * @param authentication the user session
     * @return a contract snapshot
     */
    @Transactional
    public PlayerContractResponse createContract(Long playerId, Authentication authentication) {
        Manager manager = managerService.getOrCreateManager(authentication);
        Player player = getPlayerOrThrow(playerId);

        boolean active = true;
        boolean retired = false;
        
        java.util.Optional<com.aditya.worldcup.contracts.entity.PlayerLifecycle> lifecycleOpt = 
            playerLifecycleRepository.findByManagerIdAndPlayerId(manager.getId(), player.getId());
        if (lifecycleOpt.isPresent()) {
            active = lifecycleOpt.get().getActive();
            retired = lifecycleOpt.get().getRetired();
        }
        
        if (!active || retired) {
            throw new IllegalStateException("Cannot create contract for inactive or retired player.");
        }

        Optional<PlayerContract> existing = playerContractRepository.findByPlayerIdAndManagerIdAndStatusIn(
                playerId, manager.getId(), List.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING));
        if (existing.isPresent()) {
            throw new IllegalStateException("Player already has an active or expiring contract with this manager.");
        }

        Integer currentSeason = getCurrentSeason(manager);

        PlayerContract contract = PlayerContract.builder()
                .player(player)
                .manager(manager)
                .status(ContractStatus.ACTIVE)
                .startSeason(currentSeason)
                .expirySeason(currentSeason + 4)
                .commitmentLevel(CommitmentLevel.FULL_CYCLE)
                .renewalCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        contract = playerContractRepository.save(contract);
        recordEvent(manager, TimelineEventType.PLAYER_CONTRACT_CREATED, "Signed " + player.getName(), "Contract signed with " + player.getName() + " until " + contract.getExpirySeason());
        return mapToResponse(contract);
    }

    /**
     * Renews an existing contract.
     * @param contractId the contract DB id
     * @param authentication the manager session
     * @return the renewed contract
     */
    @Transactional
    public PlayerContractResponse renewContract(Long contractId, Authentication authentication) {
        Manager manager = managerService.getOrCreateManager(authentication);
        PlayerContract contract = getContractOrThrow(contractId, manager);
        
        Player player = contract.getPlayer();
        
        boolean active = true;
        boolean retired = false;
        
        java.util.Optional<com.aditya.worldcup.contracts.entity.PlayerLifecycle> lifecycleOpt = 
            playerLifecycleRepository.findByManagerIdAndPlayerId(manager.getId(), player.getId());
        if (lifecycleOpt.isPresent()) {
            active = lifecycleOpt.get().getActive();
            retired = lifecycleOpt.get().getRetired();
        }

        if (!active || retired) {
            throw new IllegalStateException("Cannot renew contract for inactive or retired player.");
        }

        if (contract.getStatus() != ContractStatus.ACTIVE && contract.getStatus() != ContractStatus.EXPIRING) {
            throw new IllegalStateException("Only active or expiring contracts can be renewed.");
        }

        contract.setExpirySeason(contract.getExpirySeason() + 4);
        contract.setRenewalCount(contract.getRenewalCount() + 1);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setUpdatedAt(LocalDateTime.now());
        contract = playerContractRepository.save(contract);

        recordEvent(manager, TimelineEventType.PLAYER_CONTRACT_RENEWED, "Renewed " + player.getName(), "Contract renewed for " + player.getName() + " until " + contract.getExpirySeason());

        return mapToResponse(contract);
    }

    /**
     * Terminates a contract immediately.
     * @param contractId the contract
     * @param authentication the user session
     * @return the terminated contract
     */
    @Transactional
    public PlayerContractResponse terminateContract(Long contractId, Authentication authentication) {
        Manager manager = managerService.getOrCreateManager(authentication);
        PlayerContract contract = getContractOrThrow(contractId, manager);

        if (contract.getStatus() == ContractStatus.TERMINATED || contract.getStatus() == ContractStatus.EXPIRED) {
            throw new IllegalStateException("Contract is already inactive.");
        }

        contract.setStatus(ContractStatus.TERMINATED);
        contract.setTerminatedAt(LocalDateTime.now());
        contract.setUpdatedAt(LocalDateTime.now());
        contract = playerContractRepository.save(contract);

        recordEvent(manager, TimelineEventType.PLAYER_CONTRACT_TERMINATED, "Terminated " + contract.getPlayer().getName(), "Contract terminated for " + contract.getPlayer().getName());

        return mapToResponse(contract);
    }
    
    @Transactional
    public void evaluateExpiries(Authentication authentication) {
        Manager manager = managerService.getOrCreateManager(authentication);
        Integer currentSeason = getCurrentSeason(manager);
        
        List<PlayerContract> managerContracts = playerContractRepository.findByManagerId(manager.getId());
        for (PlayerContract contract : managerContracts) {
            if (contract.getStatus() == ContractStatus.ACTIVE || contract.getStatus() == ContractStatus.EXPIRING) {
                if (currentSeason > contract.getExpirySeason()) {
                    contract.setStatus(ContractStatus.EXPIRED);
                    contract.setUpdatedAt(LocalDateTime.now());
                    playerContractRepository.save(contract);
                    recordEvent(manager, TimelineEventType.PLAYER_CONTRACT_EXPIRED, "Contract Expired", contract.getPlayer().getName() + "'s contract expired.");
                } else if (currentSeason.equals(contract.getExpirySeason())) {
                    if (contract.getStatus() != ContractStatus.EXPIRING) {
                        contract.setStatus(ContractStatus.EXPIRING);
                        contract.setUpdatedAt(LocalDateTime.now());
                        playerContractRepository.save(contract);
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<PlayerContractResponse> getManagerContracts(Authentication authentication) {
        Manager manager = managerService.getOrCreateManager(authentication);
        return playerContractRepository.findByManagerId(manager.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlayerContractResponse> getExpiringContracts(Authentication authentication) {
        Manager manager = managerService.getOrCreateManager(authentication);
        return playerContractRepository.findByManagerIdAndStatus(manager.getId(), ContractStatus.EXPIRING)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlayerContractResponse> getPlayerContracts(Long playerId) {
        return playerContractRepository.findByPlayerId(playerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Player getPlayerOrThrow(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + id));
    }

    private PlayerContract getContractOrThrow(Long id, Manager manager) {
        PlayerContract contract = playerContractRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + id));
        if (!contract.getManager().getId().equals(manager.getId())) {
            throw new IllegalStateException("Contract does not belong to this manager.");
        }
        return contract;
    }

    private Integer getCurrentSeason(Manager manager) {
        var slots = saveSlotRepository.findByManagerIdAndActiveTrue(manager.getId());
        if (slots.isEmpty()) {
            return LocalDateTime.now().getYear(); // fallback
        }
        return slots.get(0).getCurrentSeason();
    }

    private void recordEvent(Manager manager, TimelineEventType type, String title, String description) {
        careerTimelineEventRepository.save(CareerTimelineEvent.builder()
                .manager(manager)
                .eventType(type)
                .title(title)
                .description(description)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    public PlayerContractResponse mapToResponse(PlayerContract contract) {
        return new PlayerContractResponse(
                contract.getId(),
                contract.getPlayer().getId(),
                contract.getManager().getId(),
                contract.getStatus(),
                contract.getStartSeason(),
                contract.getExpirySeason(),
                contract.getCommitmentLevel(),
                contract.getRenewalCount()
        );
    }
}
