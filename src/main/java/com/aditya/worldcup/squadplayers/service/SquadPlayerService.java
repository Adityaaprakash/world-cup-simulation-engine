package com.aditya.worldcup.squadplayers.service;

import com.aditya.worldcup.players.dto.PlayerResponse;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.squadplayers.dto.AddPlayerRequest;
import com.aditya.worldcup.squadplayers.dto.StartingXiRequest;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.users.entity.User;
import com.aditya.worldcup.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import com.aditya.worldcup.squadplayers.dto.CaptainRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SquadPlayerService {

    private final SquadPlayerRepository squadPlayerRepository;
    private final SquadRepository squadRepository;
    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;

    public void addPlayer(
            Long squadId,
            AddPlayerRequest request,
            Authentication authentication
    ) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Squad squad = squadRepository.findById(squadId)
                .orElseThrow(() ->
                        new RuntimeException("Squad not found"));

        if (!squad.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not your squad");
        }

        Player player = playerRepository.findById(request.playerId())
                .orElseThrow(() ->
                        new RuntimeException("Player not found"));

        if (!player.getCountry().getId()
                .equals(squad.getTeam().getCountry().getId())) {

            throw new RuntimeException(
                    "Player does not belong to selected country");
        }

        if (squadPlayerRepository.existsBySquadIdAndPlayerId(
                squadId,
                player.getId())) {

            throw new RuntimeException(
                    "Player already exists in squad");
        }

        if (squadPlayerRepository.countBySquadId(squadId) >= 26) {
            throw new RuntimeException(
                    "Squad already contains 26 players");
        }

        SquadPlayer squadPlayer = SquadPlayer.builder()
                .squad(squad)
                .player(player)
                .positionSlot("RESERVE")
                .startingXi(false)
                .captain(false)
                .build();

        squadPlayerRepository.save(squadPlayer);
    }

    public List<PlayerResponse> getSquadPlayers(Long squadId) {

        return squadPlayerRepository.findBySquadId(squadId)
                .stream()
                .map(sp -> new PlayerResponse(
                        sp.getPlayer().getId(),
                        sp.getPlayer().getName(),
                        sp.getPlayer().getPosition().name(),
                        sp.getPlayer().getOverallRating()
                ))
                .toList();
    }

    public void removePlayer(
            Long squadId,
            Long playerId,
            Authentication authentication
    ) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Squad squad = squadRepository.findById(squadId)
                .orElseThrow(() ->
                        new RuntimeException("Squad not found"));

        if (!squad.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not your squad");
        }

        SquadPlayer squadPlayer =
                squadPlayerRepository.findBySquadId(squadId)
                        .stream()
                        .filter(sp ->
                                sp.getPlayer().getId().equals(playerId))
                        .findFirst()
                        .orElseThrow(() ->
                                new RuntimeException("Player not in squad"));

        squadPlayerRepository.delete(squadPlayer);
    }

    public void setCaptain(
            Long squadId,
            CaptainRequest request,
            Authentication authentication
    ) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Squad squad = squadRepository.findById(squadId)
                .orElseThrow(() ->
                        new RuntimeException("Squad not found"));

        if (!squad.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not your squad");
        }

        List<SquadPlayer> squadPlayers =
                squadPlayerRepository.findBySquadId(squadId);

        SquadPlayer selectedPlayer = squadPlayers.stream()
                .filter(sp ->
                        sp.getPlayer().getId()
                                .equals(request.playerId()))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(
                                "Player not found in squad"));

        squadPlayers.forEach(sp -> {
            sp.setCaptain(false);
            squadPlayerRepository.save(sp);
        });

        selectedPlayer.setCaptain(true);
        squadPlayerRepository.save(selectedPlayer);
    }

    public void setStartingXi(
            Long squadId,
            StartingXiRequest request,
            Authentication authentication
    ) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Squad squad = squadRepository.findById(squadId)
                .orElseThrow(() ->
                        new RuntimeException("Squad not found"));

        if (!squad.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not your squad");
        }

        if (request.playerIds() == null ||
                request.playerIds().size() != 11) {

            throw new RuntimeException(
                    "Starting XI must contain exactly 11 players");
        }

        List<SquadPlayer> squadPlayers =
                squadPlayerRepository.findBySquadId(squadId);

        for (Long playerId : request.playerIds()) {

            boolean exists = squadPlayers.stream()
                    .anyMatch(sp ->
                            sp.getPlayer().getId().equals(playerId));

            if (!exists) {
                throw new RuntimeException(
                        "Player does not belong to squad");
            }
        }

        squadPlayers.forEach(sp -> sp.setStartingXi(false));

        squadPlayers.stream()
                .filter(sp ->
                        request.playerIds()
                                .contains(sp.getPlayer().getId()))
                .forEach(sp -> sp.setStartingXi(true));

        squadPlayerRepository.saveAll(squadPlayers);
    }

    public List<PlayerResponse> getStartingXi(Long squadId) {

        return squadPlayerRepository
                .findBySquadIdAndStartingXiTrue(squadId)
                .stream()
                .map(sp -> new PlayerResponse(
                        sp.getPlayer().getId(),
                        sp.getPlayer().getName(),
                        sp.getPlayer().getPosition().name(),
                        sp.getPlayer().getOverallRating()
                ))
                .toList();
    }
}