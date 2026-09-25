package com.aditya.worldcup.contracts.controller;

import com.aditya.worldcup.contracts.dto.PlayerContractResponse;
import com.aditya.worldcup.contracts.service.PlayerContractService;
import com.aditya.worldcup.contracts.service.PlayerLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Player Contracts.
 */
@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
public class PlayerContractController {

    private final PlayerContractService playerContractService;
    private final PlayerLifecycleService playerLifecycleService;

    /**
     * Get all manager contracts.
     * @param authentication the user auth
     * @return list of contracts
     */
    @GetMapping("/contracts")
    public ResponseEntity<List<PlayerContractResponse>> getAllContracts(Authentication authentication) {
        return ResponseEntity.ok(playerContractService.getManagerContracts(authentication));
    }

    /**
     * Get all expiring contracts.
     * @param authentication the user auth
     * @return list of expiring contracts
     */
    @GetMapping("/contracts/expiring")
    public ResponseEntity<List<PlayerContractResponse>> getExpiringContracts(Authentication authentication) {
        return ResponseEntity.ok(playerContractService.getExpiringContracts(authentication));
    }

    /**
     * Get Player contracts by player id.
     * @param playerId the player id
     * @return list of contracts
     */
    @GetMapping("/players/{playerId}/contracts")
    public ResponseEntity<List<PlayerContractResponse>> getPlayerContracts(@PathVariable Long playerId) {
        return ResponseEntity.ok(playerContractService.getPlayerContracts(playerId));
    }

    /**
     * Create a contract.
     * @param playerId the player
     * @param authentication auth
     * @return the contract response
     */
    @PostMapping("/players/{playerId}/contracts")
    public ResponseEntity<PlayerContractResponse> createContract(
            @PathVariable Long playerId,
            Authentication authentication) {
        return ResponseEntity.ok(playerContractService.createContract(playerId, authentication));
    }

    /**
     * Renew an existing contract.
     * @param contractId the contract
     * @param authentication auth
     * @return the renewed contract
     */
    @PostMapping("/contracts/{contractId}/renew")
    public ResponseEntity<PlayerContractResponse> renewContract(
            @PathVariable Long contractId,
            Authentication authentication) {
        return ResponseEntity.ok(playerContractService.renewContract(contractId, authentication));
    }

    /**
     * Terminate an existing contract.
     * @param contractId the contract
     * @param authentication auth
     * @return the terminated contract
     */
    @PostMapping("/contracts/{contractId}/terminate")
    public ResponseEntity<PlayerContractResponse> terminateContract(
            @PathVariable Long contractId,
            Authentication authentication) {
        return ResponseEntity.ok(playerContractService.terminateContract(contractId, authentication));
    }

    /**
     * Retire a player.
     * @param playerId the player
     * @param authentication auth
     * @return ok
     */
    @PostMapping("/players/{playerId}/lifecycle/retire")
    public ResponseEntity<Void> retirePlayer(
            @PathVariable Long playerId,
            Authentication authentication) {
        playerLifecycleService.retirePlayer(playerId, authentication);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Reactivate a player.
     * @param playerId the player
     * @param authentication auth
     * @return ok
     */
    @PostMapping("/players/{playerId}/lifecycle/reactivate")
    public ResponseEntity<Void> reactivatePlayer(
            @PathVariable Long playerId,
            Authentication authentication) {
        playerLifecycleService.reactivatePlayer(playerId, authentication);
        return ResponseEntity.ok().build();
    }
}
