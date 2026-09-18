package com.aditya.worldcup.squadplayers.service;

import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.service.PlayerStateService;
import com.aditya.worldcup.squadplayers.dto.SquadAnalysisResponse;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.users.repository.UserRepository;
import com.aditya.worldcup.players.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SquadPlayerServiceTest {

    private final SquadPlayerRepository squadPlayerRepository = mock(SquadPlayerRepository.class);
    private final SquadRepository squadRepository = mock(SquadRepository.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PlayerStateService playerStateService = mock(PlayerStateService.class);

    private final SquadPlayerService service = new SquadPlayerService(
            squadPlayerRepository, squadRepository, playerRepository, userRepository, playerStateService);

    @Test
    void squadAnalysisReturnsGoalkeeperLimitation() {
        Player p1 = new Player(); p1.setPosition(PlayerPosition.GK); p1.setName("GK1");
        Player p2 = new Player(); p2.setPosition(PlayerPosition.ST); p2.setName("ST1");

        SquadPlayer sp1 = new SquadPlayer(); sp1.setPlayer(p1);
        SquadPlayer sp2 = new SquadPlayer(); sp2.setPlayer(p2);

        when(squadPlayerRepository.findBySquadId(1L)).thenReturn(List.of(sp1, sp2));
        when(playerStateService.getOrCreateState(any())).thenReturn(new PlayerState());
        when(playerStateService.isAvailable(any())).thenReturn(true);

        SquadAnalysisResponse response = service.getSquadAnalysis(1L);

        assertThat(response.goalkeeperCount()).isEqualTo(1);
        assertThat(response.attackerCount()).isEqualTo(1);
        assertThat(response.recommendation()).isEqualTo("Goalkeeper depth is limited.");
    }
}
