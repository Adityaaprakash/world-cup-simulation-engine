package com.aditya.worldcup.ai.service;

import com.aditya.worldcup.formations.entity.Formation;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.service.PlayerEffectiveRatingService;
import com.aditya.worldcup.players.service.PlayerStateService;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.squads.entity.Squad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LineupSelectionService {

    private final SquadPlayerRepository squadPlayerRepository;
    private final PlayerEffectiveRatingService playerEffectiveRatingService;
    private final PlayerStateService playerStateService;
    private final RotationService rotationService;

    @Transactional
    public void selectStartingXi(Squad squad, Formation formation) {
        List<SquadPlayer> squadPlayers = squadPlayerRepository.findBySquadId(squad.getId());
        List<SquadPlayer> available = squadPlayers.stream()
                .filter(player -> rotationService.isAvailable(player.getPlayer()))
                .toList();
        List<SquadPlayer> preferred = available.stream()
                .filter(player -> !rotationService.shouldRest(player.getPlayer()))
                .toList();
        List<SquadPlayer> candidates = preferred.size() >= 11 ? preferred : available;
        Set<Long> selected = new HashSet<>();

        select(candidates, selected, Set.of(PlayerPosition.GK), 1);
        select(candidates, selected, Set.of(PlayerPosition.RB, PlayerPosition.CB, PlayerPosition.LB),
                formation.getDefenders());
        select(candidates, selected, Set.of(PlayerPosition.CDM, PlayerPosition.CM, PlayerPosition.CAM),
                formation.getMidfielders());
        select(candidates, selected, Set.of(PlayerPosition.RW, PlayerPosition.LW, PlayerPosition.ST),
                formation.getAttackers());
        select(candidates, selected, EnumSet.allOf(PlayerPosition.class), 11 - selected.size());

        Map<Long, String> slots = assignSlots(candidates, selected, formation);
        squadPlayers.forEach(player -> {
            boolean starts = selected.contains(player.getPlayer().getId());
            player.setStartingXi(starts);
            player.setPositionSlot(starts ? slots.get(player.getPlayer().getId()) : "RESERVE");
            if (!starts) {
                player.setCaptain(false);
                player.setViceCaptain(false);
            }
        });
        squad.setFormation(formation);
        squadPlayerRepository.saveAll(squadPlayers);
    }

    private void select(List<SquadPlayer> candidates, Set<Long> selected,
                        Set<PlayerPosition> positions, int count) {
        if (count <= 0) {
            return;
        }
        candidates.stream()
                .filter(player -> positions.contains(player.getPlayer().getPosition()))
                .filter(player -> !selected.contains(player.getPlayer().getId()))
                .sorted(Comparator.comparingDouble(this::selectionScore).reversed())
                .limit(count)
                .map(player -> player.getPlayer().getId())
                .forEach(selected::add);
    }

    private Map<Long, String> assignSlots(List<SquadPlayer> candidates, Set<Long> selected,
                                           Formation formation) {
        List<SquadPlayer> starters = candidates.stream()
                .filter(player -> selected.contains(player.getPlayer().getId()))
                .toList();
        Map<Long, String> slots = new HashMap<>();
        assign(starters, slots, Set.of(PlayerPosition.GK), List.of("GK"));
        assign(starters, slots, Set.of(PlayerPosition.RB, PlayerPosition.CB, PlayerPosition.LB),
                defensiveSlots(formation.getDefenders()));
        assign(starters, slots, Set.of(PlayerPosition.CDM, PlayerPosition.CM, PlayerPosition.CAM),
                Collections.nCopies(formation.getMidfielders(), "CM"));
        assign(starters, slots, Set.of(PlayerPosition.RW, PlayerPosition.LW, PlayerPosition.ST),
                Collections.nCopies(formation.getAttackers(), "ST"));
        List<String> fallbackSlots = new ArrayList<>();
        fallbackSlots.addAll(defensiveSlots(formation.getDefenders()));
        fallbackSlots.addAll(Collections.nCopies(formation.getMidfielders(), "CM"));
        fallbackSlots.addAll(Collections.nCopies(formation.getAttackers(), "ST"));
        slots.values().forEach(fallbackSlots::remove);
        starters.stream().filter(player -> !slots.containsKey(player.getPlayer().getId()))
                .forEach(player -> slots.put(player.getPlayer().getId(), fallbackSlots.remove(0)));
        return slots;
    }

    private void assign(List<SquadPlayer> starters, Map<Long, String> slots,
                        Set<PlayerPosition> positions, List<String> availableSlots) {
        List<String> slotsToUse = new ArrayList<>(availableSlots);
        starters.stream()
                .filter(player -> positions.contains(player.getPlayer().getPosition()))
                .filter(player -> !slotsToUse.isEmpty())
                .forEach(player -> slots.put(player.getPlayer().getId(), slotsToUse.remove(0)));
    }

    private List<String> defensiveSlots(int defenders) {
        List<String> slots = new ArrayList<>();
        if (defenders > 0) slots.add("LB");
        for (int i = 1; i < defenders - 1; i++) slots.add("CB");
        if (defenders > 1) slots.add("RB");
        return slots;
    }

    private double selectionScore(SquadPlayer squadPlayer) {
        Player player = squadPlayer.getPlayer();
        PlayerState state = playerStateService.getOrCreateState(player);
        return playerEffectiveRatingService.calculate(player)
                + state.getCurrentForm() * 0.35
                + state.getConfidence() * 0.02
                + state.getMorale() * 0.015
                + rotationService.availabilityScore(player);
    }
}
