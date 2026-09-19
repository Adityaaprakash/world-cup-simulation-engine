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
import com.aditya.worldcup.saves.entity.SaveSlot;
import com.aditya.worldcup.saves.repository.SaveSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerContractServiceTest {

    @Mock
    private PlayerContractRepository playerContractRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private ManagerService managerService;
    @Mock
    private SaveSlotRepository saveSlotRepository;
    @Mock
    private CareerTimelineEventRepository careerTimelineEventRepository;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private PlayerContractService playerContractService;

    private Manager manager;
    private Player player;

    @BeforeEach
    void setUp() {
        manager = new Manager();
        manager.setId(1L);

        player = new Player();
        player.setId(10L);
        player.setName("Kylian Mbappe");
        player.setActive(true);
        player.setRetired(false);
    }

    @Test
    void createContract_activePlayer_success() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(playerContractRepository.findByPlayerIdAndManagerIdAndStatusIn(10L, 1L, List.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING)))
                .thenReturn(Optional.empty());

        SaveSlot saveSlot = new SaveSlot();
        saveSlot.setCurrentSeason(2028);
        when(saveSlotRepository.findByManagerIdAndActiveTrue(1L)).thenReturn(List.of(saveSlot));

        when(playerContractRepository.save(any(PlayerContract.class))).thenAnswer(i -> {
            PlayerContract pc = i.getArgument(0);
            pc.setId(100L);
            return pc;
        });

        PlayerContractResponse res = playerContractService.createContract(10L, authentication);

        assertThat(res.id()).isEqualTo(100L);
        assertThat(res.playerId()).isEqualTo(10L);
        assertThat(res.managerId()).isEqualTo(1L);
        assertThat(res.status()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(res.startSeason()).isEqualTo(2028);
        assertThat(res.expirySeason()).isEqualTo(2032);
        assertThat(res.commitmentLevel()).isEqualTo(CommitmentLevel.FULL_CYCLE);
        assertThat(res.renewalCount()).isEqualTo(0);

        ArgumentCaptor<CareerTimelineEvent> timelineCaptor = ArgumentCaptor.forClass(CareerTimelineEvent.class);
        verify(careerTimelineEventRepository).save(timelineCaptor.capture());
        CareerTimelineEvent event = timelineCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo(TimelineEventType.PLAYER_CONTRACT_CREATED);
        assertThat(event.getTitle()).contains("Signed Kylian Mbappe");
        assertThat(event.getDescription()).contains("until 2032");
    }

    @Test
    void createContract_inactivePlayer_throwsException() {
        player.setActive(false);
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));

        assertThrows(IllegalStateException.class, () -> playerContractService.createContract(10L, authentication));
    }

    @Test
    void createContract_retiredPlayer_throwsException() {
        player.setRetired(true);
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));

        assertThrows(IllegalStateException.class, () -> playerContractService.createContract(10L, authentication));
    }
    
    @Test
    void createContract_duplicateActive_throwsException() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(playerContractRepository.findByPlayerIdAndManagerIdAndStatusIn(10L, 1L, List.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING)))
                .thenReturn(Optional.of(new PlayerContract()));

        assertThrows(IllegalStateException.class, () -> playerContractService.createContract(10L, authentication));
    }

    @Test
    void createContract_missingPlayer_throwsException() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        when(playerRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> playerContractService.createContract(10L, authentication));
    }
    
    @Test
    void createContract_fallbackSeason_success() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(playerContractRepository.findByPlayerIdAndManagerIdAndStatusIn(10L, 1L, List.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING)))
                .thenReturn(Optional.empty());

        when(saveSlotRepository.findByManagerIdAndActiveTrue(1L)).thenReturn(List.of()); // No active slots

        when(playerContractRepository.save(any(PlayerContract.class))).thenAnswer(i -> {
            PlayerContract pc = i.getArgument(0);
            pc.setId(101L);
            return pc;
        });

        PlayerContractResponse res = playerContractService.createContract(10L, authentication);
        assertThat(res.startSeason()).isEqualTo(LocalDateTime.now().getYear());
    }

    @Test
    void renewContract_activeContract_success() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        
        PlayerContract contract = new PlayerContract();
        contract.setId(100L);
        contract.setPlayer(player);
        contract.setManager(manager);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setExpirySeason(2032);
        contract.setRenewalCount(1);
        
        when(playerContractRepository.findById(100L)).thenReturn(Optional.of(contract));
        when(playerContractRepository.save(any())).thenReturn(contract);

        PlayerContractResponse res = playerContractService.renewContract(100L, authentication);

        assertThat(res.expirySeason()).isEqualTo(2036);
        assertThat(res.renewalCount()).isEqualTo(2);
        assertThat(res.status()).isEqualTo(ContractStatus.ACTIVE);

        ArgumentCaptor<CareerTimelineEvent> timelineCaptor = ArgumentCaptor.forClass(CareerTimelineEvent.class);
        verify(careerTimelineEventRepository).save(timelineCaptor.capture());
        assertThat(timelineCaptor.getValue().getEventType()).isEqualTo(TimelineEventType.PLAYER_CONTRACT_RENEWED);
    }
    
    @Test
    void renewContract_expiringContract_success() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        
        PlayerContract contract = new PlayerContract();
        contract.setId(100L);
        contract.setPlayer(player);
        contract.setManager(manager);
        contract.setStatus(ContractStatus.EXPIRING);
        contract.setExpirySeason(2028);
        contract.setRenewalCount(0);
        
        when(playerContractRepository.findById(100L)).thenReturn(Optional.of(contract));
        when(playerContractRepository.save(any())).thenReturn(contract);

        PlayerContractResponse res = playerContractService.renewContract(100L, authentication);

        assertThat(res.expirySeason()).isEqualTo(2032);
        assertThat(res.renewalCount()).isEqualTo(1);
        assertThat(res.status()).isEqualTo(ContractStatus.ACTIVE);
    }

    @Test
    void renewContract_terminatedContract_throwsException() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        PlayerContract contract = new PlayerContract();
        contract.setId(100L);
        contract.setPlayer(player);
        contract.setManager(manager);
        contract.setStatus(ContractStatus.TERMINATED);
        when(playerContractRepository.findById(100L)).thenReturn(Optional.of(contract));

        assertThrows(IllegalStateException.class, () -> playerContractService.renewContract(100L, authentication));
    }

    @Test
    void renewContract_expiredContract_throwsException() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        PlayerContract contract = new PlayerContract();
        contract.setId(100L);
        contract.setPlayer(player);
        contract.setManager(manager);
        contract.setStatus(ContractStatus.EXPIRED);
        when(playerContractRepository.findById(100L)).thenReturn(Optional.of(contract));

        assertThrows(IllegalStateException.class, () -> playerContractService.renewContract(100L, authentication));
    }

    @Test
    void renewContract_inactivePlayer_throwsException() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        
        player.setActive(false);
        PlayerContract contract = new PlayerContract();
        contract.setId(100L);
        contract.setPlayer(player);
        contract.setManager(manager);
        contract.setStatus(ContractStatus.ACTIVE);
        when(playerContractRepository.findById(100L)).thenReturn(Optional.of(contract));

        assertThrows(IllegalStateException.class, () -> playerContractService.renewContract(100L, authentication));
    }
    
    @Test
    void renewContract_anotherManager_throwsException() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        
        Manager otherManager = new Manager();
        otherManager.setId(2L);
        
        PlayerContract contract = new PlayerContract();
        contract.setId(100L);
        contract.setManager(otherManager);
        
        when(playerContractRepository.findById(100L)).thenReturn(Optional.of(contract));
        
        assertThrows(IllegalStateException.class, () -> playerContractService.renewContract(100L, authentication));
    }

    @Test
    void terminateContract_validContract_success() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        
        PlayerContract contract = new PlayerContract();
        contract.setId(100L);
        contract.setPlayer(player);
        contract.setManager(manager);
        contract.setStatus(ContractStatus.ACTIVE);
        
        when(playerContractRepository.findById(100L)).thenReturn(Optional.of(contract));
        when(playerContractRepository.save(any())).thenReturn(contract);

        PlayerContractResponse res = playerContractService.terminateContract(100L, authentication);

        assertThat(res.status()).isEqualTo(ContractStatus.TERMINATED);
        assertThat(contract.getTerminatedAt()).isNotNull();

        ArgumentCaptor<CareerTimelineEvent> timelineCaptor = ArgumentCaptor.forClass(CareerTimelineEvent.class);
        verify(careerTimelineEventRepository).save(timelineCaptor.capture());
        assertThat(timelineCaptor.getValue().getEventType()).isEqualTo(TimelineEventType.PLAYER_CONTRACT_TERMINATED);
    }
    
    @Test
    void terminateContract_alreadyTerminated_throwsException() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        
        PlayerContract contract = new PlayerContract();
        contract.setId(100L);
        contract.setPlayer(player);
        contract.setManager(manager);
        contract.setStatus(ContractStatus.TERMINATED);
        
        when(playerContractRepository.findById(100L)).thenReturn(Optional.of(contract));

        assertThrows(IllegalStateException.class, () -> playerContractService.terminateContract(100L, authentication));
    }

    @Test
    void terminateContract_expired_throwsException() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        
        PlayerContract contract = new PlayerContract();
        contract.setId(100L);
        contract.setPlayer(player);
        contract.setManager(manager);
        contract.setStatus(ContractStatus.EXPIRED);
        
        when(playerContractRepository.findById(100L)).thenReturn(Optional.of(contract));

        assertThrows(IllegalStateException.class, () -> playerContractService.terminateContract(100L, authentication));
    }

    @Test
    void evaluateExpiries_contractBecomesExpiring() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        
        SaveSlot saveSlot = new SaveSlot();
        saveSlot.setCurrentSeason(2028);
        when(saveSlotRepository.findByManagerIdAndActiveTrue(1L)).thenReturn(List.of(saveSlot));
        
        PlayerContract contract = new PlayerContract();
        contract.setId(100L);
        contract.setPlayer(player);
        contract.setManager(manager);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setExpirySeason(2028); // exactly current season
        
        when(playerContractRepository.findByManagerId(1L)).thenReturn(List.of(contract));

        playerContractService.evaluateExpiries(authentication);

        verify(playerContractRepository, times(1)).save(contract);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.EXPIRING);
        // Expiration event not yet created, since it's just EXPIRING
        verify(careerTimelineEventRepository, never()).save(any());
    }

    @Test
    void evaluateExpiries_contractBecomesExpired() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        
        SaveSlot saveSlot = new SaveSlot();
        saveSlot.setCurrentSeason(2029);
        when(saveSlotRepository.findByManagerIdAndActiveTrue(1L)).thenReturn(List.of(saveSlot));
        
        PlayerContract contract = new PlayerContract();
        contract.setId(100L);
        contract.setPlayer(player);
        contract.setManager(manager);
        contract.setStatus(ContractStatus.EXPIRING);
        contract.setExpirySeason(2028); // one season behind
        
        when(playerContractRepository.findByManagerId(1L)).thenReturn(List.of(contract));

        playerContractService.evaluateExpiries(authentication);

        verify(playerContractRepository, times(1)).save(contract);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.EXPIRED);
        
        ArgumentCaptor<CareerTimelineEvent> timelineCaptor = ArgumentCaptor.forClass(CareerTimelineEvent.class);
        verify(careerTimelineEventRepository).save(timelineCaptor.capture());
        assertThat(timelineCaptor.getValue().getEventType()).isEqualTo(TimelineEventType.PLAYER_CONTRACT_EXPIRED);
    }
    
    @Test
    void evaluateExpiries_activeIgnoredIfFuture() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        SaveSlot saveSlot = new SaveSlot();
        saveSlot.setCurrentSeason(2028);
        when(saveSlotRepository.findByManagerIdAndActiveTrue(1L)).thenReturn(List.of(saveSlot));
        
        PlayerContract contract = new PlayerContract();
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setExpirySeason(2032); 
        
        when(playerContractRepository.findByManagerId(1L)).thenReturn(List.of(contract));

        playerContractService.evaluateExpiries(authentication);

        verify(playerContractRepository, never()).save(any());
    }
    
    @Test
    void getManagerContracts_returnsMappedResponses() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        PlayerContract c = new PlayerContract();
        c.setId(1L);
        c.setPlayer(player);
        c.setManager(manager);
        when(playerContractRepository.findByManagerId(1L)).thenReturn(List.of(c));
        
        List<PlayerContractResponse> res = playerContractService.getManagerContracts(authentication);
        
        assertThat(res).hasSize(1);
        assertThat(res.get(0).id()).isEqualTo(1L);
    }

    @Test
    void getExpiringContracts_returnsMappedResponses() {
        when(managerService.getOrCreateManager(authentication)).thenReturn(manager);
        PlayerContract c = new PlayerContract();
        c.setId(1L);
        c.setPlayer(player);
        c.setManager(manager);
        when(playerContractRepository.findByManagerIdAndStatus(1L, ContractStatus.EXPIRING)).thenReturn(List.of(c));
        
        List<PlayerContractResponse> res = playerContractService.getExpiringContracts(authentication);
        
        assertThat(res).hasSize(1);
        assertThat(res.get(0).id()).isEqualTo(1L);
    }

    @Test
    void getPlayerContracts_returnsMappedResponses() {
        PlayerContract c = new PlayerContract();
        c.setId(1L);
        c.setPlayer(player);
        c.setManager(manager);
        when(playerContractRepository.findByPlayerId(10L)).thenReturn(List.of(c));
        
        List<PlayerContractResponse> res = playerContractService.getPlayerContracts(10L);
        
        assertThat(res).hasSize(1);
        assertThat(res.get(0).id()).isEqualTo(1L);
    }
}
