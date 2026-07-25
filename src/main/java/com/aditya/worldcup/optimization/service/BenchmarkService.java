package com.aditya.worldcup.optimization.service;

import com.aditya.worldcup.fixtures.service.FixtureGenerationService;
import com.aditya.worldcup.groups.service.GroupService;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.simulation.dto.KnockoutSimulationResponse;
import com.aditya.worldcup.simulation.dto.MatchSimulationRequest;
import com.aditya.worldcup.simulation.dto.MatchSimulationResponse;
import com.aditya.worldcup.simulation.service.GroupStageSimulationService;
import com.aditya.worldcup.simulation.service.KnockoutSimulationService;
import com.aditya.worldcup.simulation.service.MatchSimulationService;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.teams.repository.TeamRepository;
import com.aditya.worldcup.tournaments.dto.CreateTournamentRequest;
import com.aditya.worldcup.tournaments.dto.TournamentResponse;
import com.aditya.worldcup.tournaments.service.TournamentAwardsService;
import com.aditya.worldcup.tournaments.service.TournamentService;
import com.aditya.worldcup.tournamentteams.dto.RegisterTeamRequest;
import com.aditya.worldcup.tournamentteams.service.TournamentTeamService;
import com.aditya.worldcup.optimization.config.OptimizationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BenchmarkService {

    private final MatchSimulationService matchSimulationService;
    private final TournamentService tournamentService;
    private final TournamentTeamService tournamentTeamService;
    private final GroupService groupService;
    private final FixtureGenerationService fixtureGenerationService;
    private final GroupStageSimulationService groupStageSimulationService;
    private final KnockoutSimulationService knockoutSimulationService;
    private final TournamentAwardsService tournamentAwardsService;
    private final SquadRepository squadRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final PlatformTransactionManager transactionManager;
    private final OptimizationProperties properties;
    private final SimulationMetricsService simulationMetricsService;

    private final Random random = new Random();

    public record BenchmarkReport(
            BenchmarkSummary matchBenchmark,
            BenchmarkSummary tournamentBenchmark
    ) {}

    public record BenchmarkSummary(
            int iterations,
            double averageRuntimeMs,
            double medianRuntimeMs,
            double maxRuntimeMs,
            double minRuntimeMs,
            String averageScoreline,
            Map<Integer, Double> goalDistribution, // goal total -> percentage
            double homeWinPercentage,
            double drawPercentage,
            double awayWinPercentage
    ) {}

    public BenchmarkReport runBenchmark(Integer matchIterationsOpt, Integer tournamentIterationsOpt) {
        int matchIterations = matchIterationsOpt != null ? matchIterationsOpt : properties.getBenchmarkIterations();
        int tournamentIterations = tournamentIterationsOpt != null
                ? tournamentIterationsOpt
                : properties.getTournamentBenchmarkIterations();

        log.info("Benchmark execution started: simulating {} matches and {} tournaments", matchIterations, tournamentIterations);
        long benchmarkStart = System.currentTimeMillis();

        BenchmarkSummary matchSummary = benchmarkMatches(matchIterations);
        BenchmarkSummary tournamentSummary = benchmarkTournaments(tournamentIterations);

        long benchmarkDuration = System.currentTimeMillis() - benchmarkStart;
        log.info("Benchmark execution completed in {} ms", benchmarkDuration);
        
        simulationMetricsService.recordExecutionTime("benchmark-execution", benchmarkDuration);

        return new BenchmarkReport(matchSummary, tournamentSummary);
    }

    private BenchmarkSummary benchmarkMatches(int iterations) {
        if (iterations <= 0) {
            return new BenchmarkSummary(0, 0, 0, 0, 0, "0.00 - 0.00", Collections.emptyMap(), 0, 0, 0);
        }

        List<Squad> squads = squadRepository.findAll();
        if (squads.size() < 2) {
            throw new IllegalStateException("At least 2 squads must exist matching home/away configuration to run match benchmarks");
        }

        List<Long> runtimes = new ArrayList<>(iterations);
        int totalGoals = 0;
        int totalHomeGoals = 0;
        int totalAwayGoals = 0;
        int homeWins = 0;
        int draws = 0;
        int awayWins = 0;
        Map<Integer, Integer> goalsCountMap = new HashMap<>();

        for (int i = 0; i < iterations; i++) {
            // Select two distinct squads
            Squad home = squads.get(random.nextInt(squads.size()));
            Squad away;
            do {
                away = squads.get(random.nextInt(squads.size()));
            } while (home.getId().equals(away.getId()));

            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TransactionStatus status = transactionManager.getTransaction(def);

            long start = System.currentTimeMillis();
            try {
                MatchSimulationResponse response = matchSimulationService.simulate(
                        new MatchSimulationRequest(home.getId(), away.getId()), null
                );
                long duration = System.currentTimeMillis() - start;
                runtimes.add(duration);

                int matchGoals = response.homeGoals() + response.awayGoals();
                totalGoals += matchGoals;
                totalHomeGoals += response.homeGoals();
                totalAwayGoals += response.awayGoals();
                
                goalsCountMap.put(matchGoals, goalsCountMap.getOrDefault(matchGoals, 0) + 1);

                if (response.winner().equalsIgnoreCase(home.getName())) {
                    homeWins++;
                } else if (response.winner().equalsIgnoreCase("DRAW")) {
                    draws++;
                } else {
                    awayWins++;
                }
            } catch (Exception e) {
                log.error("Error simulating match in benchmark", e);
            } finally {
                transactionManager.rollback(status);
            }
        }

        return buildSummary(iterations, runtimes, totalHomeGoals, totalAwayGoals, homeWins, draws, awayWins, goalsCountMap);
    }

    private BenchmarkSummary benchmarkTournaments(int iterations) {
        if (iterations <= 0) {
            return new BenchmarkSummary(0, 0, 0, 0, 0, null, Collections.emptyMap(), 0, 0, 0);
        }

        List<Team> allTeams = teamRepository.findAll();
        if (allTeams.size() < 16) { // Min teams needed; group generation shuffles and assigns teams.
            throw new IllegalStateException("At least 16 teams must exist in the database to benchmark tournaments");
        }

        List<Long> runtimes = new ArrayList<>(iterations);
        int totalHomeGoals = 0;
        int totalAwayGoals = 0;
        int homeWins = 0;
        int draws = 0;
        int awayWins = 0;
        Map<Integer, Integer> goalsCountMap = new HashMap<>();

        for (int i = 0; i < iterations; i++) {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TransactionStatus status = transactionManager.getTransaction(def);

            long start = System.currentTimeMillis();
            try {
                // 1. Create tournament
                TournamentResponse tournament = tournamentService.createTournament(
                        new CreateTournamentRequest("Benchmark tournament " + i, 2026, "Benchmark Host")
                );

                // 2. Register 32 teams (or allTeams size if smaller, up to 32)
                int numTeamsToRegister = Math.min(32, allTeams.size());
                // Shuffle teams
                List<Team> teamsSelection = new ArrayList<>(allTeams);
                Collections.shuffle(teamsSelection);
                
                for (int t = 0; t < numTeamsToRegister; t++) {
                    tournamentTeamService.registerTeam(
                            tournament.id(), new RegisterTeamRequest(teamsSelection.get(t).getId())
                    );
                }

                // 3. Generate Groups
                groupService.generateGroups(tournament.id());

                // 4. Generate Fixtures
                fixtureGenerationService.generateFixtures(tournament.id());

                // 5. Simulate Groups
                groupStageSimulationService.simulate(tournament.id());

                // 6. Simulate Knockouts
                KnockoutSimulationResponse knockoutResp = knockoutSimulationService.simulate(tournament.id());

                // 7. Calculate Awards
                tournamentAwardsService.calculateAwards(tournament.id());
                List<Match> completedMatches = matchRepository.findByTournamentIdOrderById(tournament.id())
                        .stream()
                        .filter(match -> match.getStatus() == MatchStatus.FINISHED)
                        .filter(match -> match.getHomeScore() != null && match.getAwayScore() != null)
                        .toList();
                for (Match match : completedMatches) {
                    totalHomeGoals += match.getHomeScore();
                    totalAwayGoals += match.getAwayScore();
                    int goals = match.getHomeScore() + match.getAwayScore();
                    goalsCountMap.put(goals, goalsCountMap.getOrDefault(goals, 0) + 1);
                    if (match.getHomeScore() > match.getAwayScore()) {
                        homeWins++;
                    } else if (match.getHomeScore().equals(match.getAwayScore())) {
                        draws++;
                    } else {
                        awayWins++;
                    }
                }

                long duration = System.currentTimeMillis() - start;
                runtimes.add(duration);
                log.info("Benchmarked tournament {} execution: champion {}", i + 1, knockoutResp.champion());

            } catch (Exception e) {
                log.error("Error simulating tournament in benchmark", e);
            } finally {
                transactionManager.rollback(status);
            }
        }

        return buildSummary(iterations, runtimes, totalHomeGoals, totalAwayGoals,
                homeWins, draws, awayWins, goalsCountMap);
    }

    private BenchmarkSummary buildSummary(
            int iterations,
            List<Long> runtimes,
            int totalHomeGoals,
            int totalAwayGoals,
            int homeWins,
            int draws,
            int awayWins,
            Map<Integer, Integer> goalsCountMap
    ) {
        if (runtimes.isEmpty()) {
            return new BenchmarkSummary(0, 0, 0, 0, 0, null, Collections.emptyMap(), 0, 0, 0);
        }

        double totalRuntime = runtimes.stream().mapToLong(Long::longValue).sum();
        double minRuntime = runtimes.stream().mapToLong(Long::longValue).min().orElse(0);
        double maxRuntime = runtimes.stream().mapToLong(Long::longValue).max().orElse(0);
        double avgRuntime = totalRuntime / runtimes.size();

        Collections.sort(runtimes);
        double medianRuntime;
        int size = runtimes.size();
        if (size % 2 == 0) {
            medianRuntime = (runtimes.get(size / 2 - 1) + runtimes.get(size / 2)) / 2.0;
        } else {
            medianRuntime = runtimes.get(size / 2);
        }

        int resultCount = homeWins + draws + awayWins;
        int footballSampleSize = resultCount == 0 ? size : resultCount;
        String avgScoreline = String.format("%.2f - %.2f",
                (double) totalHomeGoals / footballSampleSize,
                (double) totalAwayGoals / footballSampleSize);

        Map<Integer, Double> goalDistribution = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> entry : goalsCountMap.entrySet()) {
            goalDistribution.put(entry.getKey(), (double) entry.getValue() / footballSampleSize * 100.0);
        }

        return new BenchmarkSummary(
                iterations,
                avgRuntime,
                medianRuntime,
                maxRuntime,
                minRuntime,
                avgScoreline,
                goalDistribution,
                (double) homeWins / footballSampleSize * 100.0,
                (double) draws / footballSampleSize * 100.0,
                (double) awayWins / footballSampleSize * 100.0
        );
    }
}
