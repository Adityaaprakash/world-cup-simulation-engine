package com.aditya.worldcup.players.service;

import com.aditya.worldcup.players.dto.PlayerResponse;
import com.aditya.worldcup.players.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final com.aditya.worldcup.players.service.PlayerStateService playerStateService;

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
                player.getActive(),
                player.getRetired(),
                state.getCurrentForm(),
                state.getFitness(),
                state.getFatigue(),
                state.getInjuryStatus(),
                available
        );
    }
}
