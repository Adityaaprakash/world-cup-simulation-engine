package com.aditya.worldcup.players.service;

import com.aditya.worldcup.players.dto.PlayerDetailsResponse;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PlayerServiceTest {

    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final PlayerStateService playerStateService = mock(PlayerStateService.class);
    private final PlayerService service = new PlayerService(playerRepository, playerStateService);

    @Test
    void comparePlayersReturnsMappedResponses() {
        Player p1 = new Player();
        p1.setId(1L);
        p1.setName("Lionel Messi");
        p1.setOverallRating(94);
        p1.setPosition(PlayerPosition.RW);
        com.aditya.worldcup.countries.entity.Country c1 = new com.aditya.worldcup.countries.entity.Country();
        c1.setName("Argentina");
        p1.setCountry(c1);
        
        Player p2 = new Player();
        p2.setId(2L);
        p2.setName("Kylian Mbappe");
        p2.setOverallRating(93);
        p2.setPosition(PlayerPosition.LW);
        com.aditya.worldcup.countries.entity.Country c2 = new com.aditya.worldcup.countries.entity.Country();
        c2.setName("France");
        p2.setCountry(c2);

        when(playerRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(playerRepository.findById(2L)).thenReturn(Optional.of(p2));
        when(playerStateService.getOrCreateState(any())).thenReturn(new PlayerState());
        when(playerStateService.isAvailable(any())).thenReturn(true);

        List<PlayerDetailsResponse> result = service.comparePlayers(List.of(1L, 2L));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Lionel Messi");
        assertThat(result.get(1).name()).isEqualTo("Kylian Mbappe");
    }
}
