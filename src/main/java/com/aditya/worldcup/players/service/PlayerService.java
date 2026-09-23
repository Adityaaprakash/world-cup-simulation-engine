package com.aditya.worldcup.players.service;

import com.aditya.worldcup.players.dto.PlayerResponse;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.managers.service.ManagerService;
import com.aditya.worldcup.contracts.repository.PlayerLifecycleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final com.aditya.worldcup.players.service.PlayerStateService playerStateService;
    private final ManagerService managerService;
    private final PlayerLifecycleRepository playerLifecycleRepository;

    public List<PlayerResponse> getAllPlayers() {

        return playerRepository.findAll()
                .stream()
                .map(player -> new PlayerResponse(
                        player.getId(),
                        player.getName(),
                        player.getPosition().name(),
                        player.getOverallRating()
                ))
                .toList();
    }

    public Page<PlayerResponse> getPlayerPage(Pageable pageable) {

        return playerRepository.findAll(pageable)
                .map(player -> new PlayerResponse(
                        player.getId(),
                        player.getName(),
                        player.getPosition().name(),
                        player.getOverallRating()
                ));
    }

    public List<PlayerResponse> getPlayersByCountry(
            Long countryId
    ) {

        return playerRepository.findByCountryId(countryId)
                .stream()
                .map(player -> new PlayerResponse(
                        player.getId(),
                        player.getName(),
                        player.getPosition().name(),
                        player.getOverallRating()
                ))
                .toList();
    }

    public com.aditya.worldcup.players.dto.PlayerDetailsResponse getPlayerDetails(Long id) {
        com.aditya.worldcup.players.entity.Player player = playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        return buildDetailsResponse(player);
    }

    public List<com.aditya.worldcup.players.dto.PlayerDetailsResponse> comparePlayers(List<Long> playerIds) {
        return playerIds.stream()
                .map(this::getPlayerDetails)
                .toList();
    }

    private com.aditya.worldcup.players.dto.PlayerDetailsResponse buildDetailsResponse(com.aditya.worldcup.players.entity.Player player) {
        com.aditya.worldcup.players.entity.PlayerState state = playerStateService.getOrCreateState(player);
        boolean available = playerStateService.isAvailable(state);

        boolean active = player.getActive();
        boolean retired = player.getRetired();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            try {
                com.aditya.worldcup.managers.entity.Manager manager = managerService.getOrCreateManager(auth);
                java.util.Optional<com.aditya.worldcup.contracts.entity.PlayerLifecycle> lifecycleOpt =
                    playerLifecycleRepository.findByManagerIdAndPlayerId(manager.getId(), player.getId());
                if (lifecycleOpt.isPresent()) {
                    active = lifecycleOpt.get().getActive();
                    retired = lifecycleOpt.get().getRetired();
                }
            } catch (Exception e) {
                // Ignore and fallback to global state if manager cannot be resolved
            }
        }

        return new com.aditya.worldcup.players.dto.PlayerDetailsResponse(
                player.getId(),
                player.getName(),
                player.getCountry().getName(),
                player.getPosition().name(),
                player.getAge(),
                player.getOverallRating(),
                player.getPotential(),
                player.getPace(),
                player.getShooting(),
                player.getPassing(),
                player.getDribbling(),
                player.getDefending(),
                player.getPhysical(),
                player.getPreferredFoot(),
                active,
                retired,
                state.getCurrentForm(),
                state.getFitness(),
                state.getFatigue(),
                state.getInjuryStatus(),
                available
        );
    }
}
