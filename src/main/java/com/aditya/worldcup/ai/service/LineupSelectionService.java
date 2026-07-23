package com.aditya.worldcup.ai.service;

import com.aditya.worldcup.formations.entity.Formation;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.squads.entity.Squad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LineupSelectionService {

    private final SquadPlayerRepository squadPlayerRepository;
    private final RotationService rotationService;
    private final PlayerEvaluationService playerEvaluationService;

    public List<SquadPlayer> selectMatchSquad(Squad squad) {
        return selectMatchSquad(squad, MatchImportance.GROUP_STAGE);
    }

    public List<SquadPlayer> selectMatchSquad(Squad squad, MatchImportance importance) {
        return squadPlayerRepository.findBySquadId(squad.getId()).stream()
                .filter(player -> rotationService.isAvailable(player.getPlayer()))
                .sorted(Comparator.<SquadPlayer>comparingDouble(player ->
                        selectionScore(player, importance)).reversed())
                .toList();
    }

    public List<SquadPlayer> selectBench(Squad squad) {
        return selectBench(squad, MatchImportance.GROUP_STAGE);
    }

    public List<SquadPlayer> selectBench(Squad squad, MatchImportance importance) {
        List<SquadPlayer> squadPlayers = squadPlayerRepository.findBySquadId(squad.getId());
        List<SquadPlayer> bench = squadPlayers.stream()
                .filter(player -> !player.getStartingXi())
                .filter(player -> rotationService.isAvailable(player.getPlayer()))
                .sorted(Comparator.<SquadPlayer>comparingDouble(player ->
                        selectionScore(player, importance)).reversed())
                .toList();
        List<SquadPlayer> balancedBench = new ArrayList<>();
        addBenchPlayers(bench, balancedBench, Set.of(PlayerPosition.GK), 1);
        addBenchPlayers(bench, balancedBench, Set.of(PlayerPosition.RB, PlayerPosition.CB, PlayerPosition.LB), 2);
        addBenchPlayers(bench, balancedBench, Set.of(PlayerPosition.CDM, PlayerPosition.CM, PlayerPosition.CAM), 2);
        addBenchPlayers(bench, balancedBench, Set.of(PlayerPosition.RW, PlayerPosition.LW, PlayerPosition.ST), 2);
        bench.stream()
                .filter(player -> !balancedBench.contains(player))
                .limit(Math.max(0, 12 - balancedBench.size()))
                .forEach(balancedBench::add);
        return balancedBench;
    }

    @Transactional
    public void selectStartingXi(Squad squad, Formation formation) {
        selectStartingXi(squad, formation, MatchImportance.GROUP_STAGE);
    }

    @Transactional
    public void selectStartingXi(Squad squad, Formation formation, MatchImportance importance) {
        List<SquadPlayer> squadPlayers = squadPlayerRepository.findBySquadId(squad.getId());
        List<SquadPlayer> available = squadPlayers.stream()
                .filter(player -> rotationService.isAvailable(player.getPlayer()))
                .toList();
        List<SquadPlayer> preferred = available.stream()
                .filter(player -> !rotationService.shouldRest(player.getPlayer(), importance))
                .toList();
        List<SquadPlayer> candidates = preferred.size() >= 11 ? preferred : available;
        Set<Long> selected = new HashSet<>();

        select(candidates, selected, Set.of(PlayerPosition.GK), 1, importance);
        select(candidates, selected, Set.of(PlayerPosition.RB, PlayerPosition.CB, PlayerPosition.LB),
                formation.getDefenders(), importance);
        select(candidates, selected, Set.of(PlayerPosition.CDM, PlayerPosition.CM, PlayerPosition.CAM),
                formation.getMidfielders(), importance);
        select(candidates, selected, Set.of(PlayerPosition.RW, PlayerPosition.LW, PlayerPosition.ST),
                formation.getAttackers(), importance);
        select(candidates, selected, EnumSet.allOf(PlayerPosition.class),
                11 - selected.size(), importance);

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
                        Set<PlayerPosition> positions, int count,
                        MatchImportance importance) {
        if (count <= 0) {
            return;
        }
        candidates.stream()
                .filter(player -> positions.contains(player.getPlayer().getPosition()))
                .filter(player -> !selected.contains(player.getPlayer().getId()))
                .sorted(Comparator.<SquadPlayer>comparingDouble(player ->
                        selectionScore(player, importance)).reversed())
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

    private void addBenchPlayers(List<SquadPlayer> bench, List<SquadPlayer> balancedBench,
                                 Set<PlayerPosition> positions, int count) {
        bench.stream()
                .filter(player -> positions.contains(player.getPlayer().getPosition()))
                .filter(player -> !balancedBench.contains(player))
                .limit(count)
                .forEach(balancedBench::add);
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
        return selectionScore(squadPlayer, MatchImportance.GROUP_STAGE);
    }

    private double selectionScore(SquadPlayer squadPlayer, MatchImportance importance) {
        return playerEvaluationService.evaluatePlayer(squadPlayer.getPlayer())
                * importance.qualityWeight()
                + rotationService.availabilityScore(squadPlayer.getPlayer(), importance);
    }
}
