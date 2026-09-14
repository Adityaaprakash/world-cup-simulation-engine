package com.aditya.worldcup.transfers.service;

import com.aditya.worldcup.countries.entity.Country;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.users.entity.User;
import com.aditya.worldcup.users.repository.UserRepository;
import com.aditya.worldcup.transfers.dto.TransferRequest;
import com.aditya.worldcup.transfers.dto.TransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerTransferServiceTest {

    @Mock private SquadRepository squadRepository;
    @Mock private SquadPlayerRepository squadPlayerRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private UserRepository userRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private Authentication authentication;

    @InjectMocks
    private PlayerTransferService playerTransferService;

    private User user;
    private Squad sourceSquad;
    private Squad destSquad;
    private Player player;
    private SquadPlayer existingSp;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("manager@test.com");

        Country country = new Country();
        country.setId(100L);

        Team team = new Team();
        team.setId(10L);
        team.setCountry(country);

        sourceSquad = new Squad();
        sourceSquad.setId(20L);
        sourceSquad.setUser(user);
        sourceSquad.setTeam(team);

        destSquad = new Squad();
        destSquad.setId(30L);
        destSquad.setUser(user);
        destSquad.setTeam(team);

        player = new Player();
        player.setId(50L);
        player.setName("Messi");
        player.setCountry(country);

        existingSp = new SquadPlayer();
        existingSp.setId(500L);
        existingSp.setPlayer(player);
        existingSp.setSquad(sourceSquad);
        existingSp.setStartingXi(false);
        existingSp.setCaptain(false);

        lenient().when(authentication.getName()).thenReturn("manager@test.com");
        lenient().when(userRepository.findByEmail("manager@test.com")).thenReturn(Optional.of(user));
    }

    @Test
    void testSuccessfulTransferBetweenTwoValidSquads() {
        when(squadRepository.findById(20L)).thenReturn(Optional.of(sourceSquad));
        when(squadRepository.findById(30L)).thenReturn(Optional.of(destSquad));
        when(matchRepository.existsActiveMatchForTeam(any())).thenReturn(false);
        when(playerRepository.findById(50L)).thenReturn(Optional.of(player));
        when(squadPlayerRepository.findBySquadId(20L)).thenReturn(List.of(existingSp));
        when(squadPlayerRepository.existsBySquadIdAndPlayerId(30L, 50L)).thenReturn(false);
        when(squadPlayerRepository.countBySquadId(30L)).thenReturn(25L);

        TransferRequest req = new TransferRequest(50L, 20L, 30L);
        TransferResponse res = playerTransferService.transferPlayer(req, authentication);

        assertThat(res.playerId()).isEqualTo(50L);

        verify(squadPlayerRepository).delete(existingSp);
        
        ArgumentCaptor<SquadPlayer> captor = ArgumentCaptor.forClass(SquadPlayer.class);
        verify(squadPlayerRepository).save(captor.capture());
        
        SquadPlayer newSp = captor.getValue();
        assertThat(newSp.getSquad().getId()).isEqualTo(30L);
        assertThat(newSp.getPlayer().getId()).isEqualTo(50L);
        assertThat(newSp.getStartingXi()).isFalse();
        assertThat(newSp.getCaptain()).isFalse();
    }

    @Test
    void duplicateRegistrationIsRejected() {
        when(squadRepository.findById(20L)).thenReturn(Optional.of(sourceSquad));
        when(squadRepository.findById(30L)).thenReturn(Optional.of(destSquad));
        when(playerRepository.findById(50L)).thenReturn(Optional.of(player));
        when(squadPlayerRepository.findBySquadId(20L)).thenReturn(List.of(existingSp));
        when(squadPlayerRepository.existsBySquadIdAndPlayerId(30L, 50L)).thenReturn(true);

        TransferRequest req = new TransferRequest(50L, 20L, 30L);
        
        Exception ex = assertThrows(IllegalArgumentException.class, () -> 
            playerTransferService.transferPlayer(req, authentication));
            
        assertThat(ex.getMessage()).contains("Player already exists in the destination squad");
    }

    @Test
    void transferFromUnregisteredSquadIsRejected() {
        when(squadRepository.findById(20L)).thenReturn(Optional.of(sourceSquad));
        when(squadRepository.findById(30L)).thenReturn(Optional.of(destSquad));
        when(playerRepository.findById(50L)).thenReturn(Optional.of(player));
        when(squadPlayerRepository.findBySquadId(20L)).thenReturn(List.of()); // Not registered

        TransferRequest req = new TransferRequest(50L, 20L, 30L);
        
        Exception ex = assertThrows(IllegalArgumentException.class, () -> 
            playerTransferService.transferPlayer(req, authentication));
            
        assertThat(ex.getMessage()).contains("Player is not registered in the source squad");
    }

    @Test
    void transferToSameSquadIsRejected() {
        TransferRequest req = new TransferRequest(50L, 20L, 20L);
        Exception ex = assertThrows(IllegalArgumentException.class, () -> 
            playerTransferService.transferPlayer(req, authentication));
            
        assertThat(ex.getMessage()).contains("cannot be the same");
    }

    @Test
    void transferToFullSquadIsRejected() {
        when(squadRepository.findById(20L)).thenReturn(Optional.of(sourceSquad));
        when(squadRepository.findById(30L)).thenReturn(Optional.of(destSquad));
        when(playerRepository.findById(50L)).thenReturn(Optional.of(player));
        when(squadPlayerRepository.findBySquadId(20L)).thenReturn(List.of(existingSp));
        when(squadPlayerRepository.existsBySquadIdAndPlayerId(30L, 50L)).thenReturn(false);
        when(squadPlayerRepository.countBySquadId(30L)).thenReturn(26L); // FULL!

        TransferRequest req = new TransferRequest(50L, 20L, 30L);
        
        Exception ex = assertThrows(IllegalStateException.class, () -> 
            playerTransferService.transferPlayer(req, authentication));
            
        assertThat(ex.getMessage()).contains("maximum capacity of 26 players");
    }

    @Test
    void unauthorizedTransferIsRejected() {
        User otherUser = new User();
        otherUser.setId(99L);
        sourceSquad.setUser(otherUser);

        when(squadRepository.findById(20L)).thenReturn(Optional.of(sourceSquad));
        when(squadRepository.findById(30L)).thenReturn(Optional.of(destSquad));

        TransferRequest req = new TransferRequest(50L, 20L, 30L);
        
        Exception ex = assertThrows(AccessDeniedException.class, () -> 
            playerTransferService.transferPlayer(req, authentication));
            
        assertThat(ex.getMessage()).contains("own both the source and destination squads");
    }
    
    @Test
    void activeMatchRestrictionIsEnforced() {
        when(squadRepository.findById(20L)).thenReturn(Optional.of(sourceSquad));
        when(squadRepository.findById(30L)).thenReturn(Optional.of(destSquad));
        when(matchRepository.existsActiveMatchForTeam(any())).thenReturn(true);

        TransferRequest req = new TransferRequest(50L, 20L, 30L);
        
        Exception ex = assertThrows(IllegalStateException.class, () -> 
            playerTransferService.transferPlayer(req, authentication));
            
        assertThat(ex.getMessage()).contains("active match");
    }
    
    @Test
    void transferOfStarterHandledSafely() {
        existingSp.setStartingXi(true);
        existingSp.setCaptain(true);
        
        when(squadRepository.findById(20L)).thenReturn(Optional.of(sourceSquad));
        when(squadRepository.findById(30L)).thenReturn(Optional.of(destSquad));
        when(playerRepository.findById(50L)).thenReturn(Optional.of(player));
        when(squadPlayerRepository.findBySquadId(20L)).thenReturn(List.of(existingSp));
        when(squadPlayerRepository.existsBySquadIdAndPlayerId(30L, 50L)).thenReturn(false);

        TransferRequest req = new TransferRequest(50L, 20L, 30L);
        playerTransferService.transferPlayer(req, authentication);
        
        verify(squadPlayerRepository).delete(existingSp);
        
        ArgumentCaptor<SquadPlayer> captor = ArgumentCaptor.forClass(SquadPlayer.class);
        verify(squadPlayerRepository).save(captor.capture());
        
        SquadPlayer newSp = captor.getValue();
        // Destination squad registration is cleared of startingXi and captaincy
        assertThat(newSp.getStartingXi()).isFalse();
        assertThat(newSp.getCaptain()).isFalse();
    }
}
