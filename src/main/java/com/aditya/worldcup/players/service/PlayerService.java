package com.aditya.worldcup.players.service;

import com.aditya.worldcup.players.dto.PlayerResponse;
import com.aditya.worldcup.players.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;

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
}