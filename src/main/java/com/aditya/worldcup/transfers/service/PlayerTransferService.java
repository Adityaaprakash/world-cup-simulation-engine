package com.aditya.worldcup.transfers.service;

import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.shared.exception.PlayerNotFoundException;
import com.aditya.worldcup.shared.exception.SquadNotFoundException;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.users.entity.User;
import com.aditya.worldcup.users.repository.UserRepository;
import com.aditya.worldcup.transfers.dto.TransferRequest;
import com.aditya.worldcup.transfers.dto.TransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerTransferService {

    private final SquadRepository squadRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;

    @Transactional
    public TransferResponse transferPlayer(
            TransferRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.sourceSquadId().equals(request.destinationSquadId())) {
            throw new IllegalArgumentException("Source and destination squads cannot be the same");
        }

        Squad sourceSquad = squadRepository.findById(request.sourceSquadId())
                .orElseThrow(() -> new SquadNotFoundException("Source squad not found"));

        Squad destSquad = squadRepository.findById(request.destinationSquadId())
                .orElseThrow(() -> new SquadNotFoundException("Destination squad not found"));

        if (!sourceSquad.getUser().getId().equals(user.getId()) || !destSquad.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You must own both the source and destination squads to transfer");
        }

        if (matchRepository.existsActiveMatchForTeam(sourceSquad.getTeam().getId()) || 
            matchRepository.existsActiveMatchForTeam(destSquad.getTeam().getId())) {
            throw new IllegalStateException("Cannot transfer players while either squad is in an active match");
        }

        Player player = playerRepository.findById(request.playerId())
                .orElseThrow(() -> new PlayerNotFoundException("Player not found"));

        if (!player.getCountry().getId().equals(destSquad.getTeam().getCountry().getId())) {
            throw new IllegalArgumentException("Player does not belong to the destination squad's country");
        }

        SquadPlayer existingSp = squadPlayerRepository.findBySquadId(sourceSquad.getId())
                .stream()
                .filter(sp -> sp.getPlayer().getId().equals(player.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player is not registered in the source squad"));

        if (squadPlayerRepository.existsBySquadIdAndPlayerId(destSquad.getId(), player.getId())) {
            throw new IllegalArgumentException("Player already exists in the destination squad");
        }

        long destCount = squadPlayerRepository.countBySquadId(destSquad.getId());
        if (destCount >= 26) {
            throw new IllegalStateException("Destination squad is already at the maximum capacity of 26 players");
        }

        // 13. removing a player updates starting XI state safely. 
        // We delete from source and add to dest as a reserve.
        squadPlayerRepository.delete(existingSp);

        SquadPlayer newSquadPlayer = SquadPlayer.builder()
                .squad(destSquad)
                .player(player)
                .positionSlot("RESERVE")
                .startingXi(false)
                .captain(false)
                .viceCaptain(false)
                .build();

        squadPlayerRepository.save(newSquadPlayer);

        return new TransferResponse(
                player.getId(),
                player.getName(),
                sourceSquad.getId(),
                destSquad.getId(),
                "Transfer completed successfully"
        );
    }
}
