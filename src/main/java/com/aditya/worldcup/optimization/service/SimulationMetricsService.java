package com.aditya.worldcup.optimization.service;

import com.aditya.worldcup.simulation.dto.MatchSimulationResponse;
import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.simulation.dto.MatchStatisticsResponse;
import com.aditya.worldcup.optimization.config.OptimizationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationMetricsService {

    private final OptimizationProperties properties;

    private final AtomicLong totalSimulations = new AtomicLong(0);
    private final Queue<MatchMetric> matchMetricsQueue = new ConcurrentLinkedQueue<>();

    private final AtomicLong fastestSimulation = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong slowestSimulation = new AtomicLong(0);
    private final AtomicLong totalDuration = new AtomicLong(0);

    // Performance measurements for other operations
    private final ConcurrentHashMap<String, LongSummary> executionStats = new ConcurrentHashMap<>();

    public record MatchMetric(
            long durationMs,
            int goals,
            int cards,
            int substitutions,
            double possession,
            Double xG,
            boolean shootout,
            boolean extraTime
    ) {}

    public static class LongSummary {
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong total = new AtomicLong(0);
        private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong max = new AtomicLong(0);

        public void record(long value) {
            count.incrementAndGet();
            total.addAndGet(value);
            
            long currentMin;
            while (value < (currentMin = min.get())) {
                if (min.compareAndSet(currentMin, value)) {
                    break;
                }
            }

            long currentMax;
            while (value > (currentMax = max.get())) {
                if (max.compareAndSet(currentMax, value)) {
                    break;
                }
            }
        }

        public long getCount() { return count.get(); }
        public long getTotal() { return total.get(); }
        public long getMin() { return count.get() == 0 ? 0 : min.get(); }
        public long getMax() { return max.get(); }
        public double getAvg() { return count.get() == 0 ? 0.0 : (double) total.get() / count.get(); }
    }

    public void recordMatchSimulation(long durationMs, MatchSimulationResponse response, boolean extraTime, boolean shootout) {
        totalSimulations.incrementAndGet();
        totalDuration.addAndGet(durationMs);

        // Update fastest/slowest
        long currentFastest;
        while (durationMs < (currentFastest = fastestSimulation.get())) {
            if (fastestSimulation.compareAndSet(currentFastest, durationMs)) {
                break;
            }
        }

        long currentSlowest;
        while (durationMs > (currentSlowest = slowestSimulation.get())) {
            if (slowestSimulation.compareAndSet(currentSlowest, durationMs)) {
                break;
            }
        }

        // Record execution stats
        recordExecutionTime("match-simulation", durationMs);

        if (response == null) {
            return;
        }

        // Collect match details
        int goals = response.homeGoals() + response.awayGoals();
        
        int cards = 0;
        int substitutions = 0;
        if (response.events() != null) {
            for (MatchEventResponse event : response.events()) {
                String type = event.eventType();
                if ("YELLOW_CARD".equals(type) || "RED_CARD".equals(type)) {
                    cards++;
                } else if ("SUBSTITUTION".equals(type)) {
                    substitutions++;
                }
            }
        }

        double possession = 50.0;
        Double xG = null;
        if (response.statistics() != null) {
            MatchStatisticsResponse stats = response.statistics();
            if (stats.homeTeam() != null && stats.awayTeam() != null) {
                possession = (stats.homeTeam().possession() + stats.awayTeam().possession()) / 2.0;
                if (stats.homeTeam().expectedGoals() != null && stats.awayTeam().expectedGoals() != null) {
                    xG = stats.homeTeam().expectedGoals() + stats.awayTeam().expectedGoals();
                }
            }
        }

        MatchMetric metric = new MatchMetric(
                durationMs, goals, cards, substitutions, possession, xG, shootout, extraTime
        );

        matchMetricsQueue.offer(metric);

        // Keep size within retention property
        int retentionLimit = properties.getMetricsRetention();
        while (matchMetricsQueue.size() > retentionLimit && retentionLimit > 0) {
            matchMetricsQueue.poll();
        }
    }

    public void recordExecutionTime(String operation, long durationMs) {
        executionStats.computeIfAbsent(operation, k -> new LongSummary()).record(durationMs);
    }

    public Map<String, Object> getAggregatedStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long total = totalSimulations.get();
        stats.put("totalSimulations", total);
        
        long fastest = fastestSimulation.get();
        stats.put("fastestSimulationMs", fastest == Long.MAX_VALUE ? 0 : fastest);
        stats.put("slowestSimulationMs", slowestSimulation.get());
        stats.put("averageSimulationDurationMs", total == 0 ? 0.0 : (double) totalDuration.get() / total);

        List<MatchMetric> currentMetrics = new ArrayList<>(matchMetricsQueue);
        int count = currentMetrics.size();

        if (count > 0) {
            double totalGoals = 0;
            double totalCards = 0;
            double totalSubs = 0;
            double totalPossession = 0;
            double totalXG = 0;
            int xGCount = 0;
            int shootoutsCount = 0;
            int extraTimeCount = 0;

            for (MatchMetric m : currentMetrics) {
                totalGoals += m.goals();
                totalCards += m.cards();
                totalSubs += m.substitutions();
                totalPossession += m.possession();
                if (m.xG() != null) {
                    totalXG += m.xG();
                    xGCount++;
                }
                if (m.shootout()) {
                    shootoutsCount++;
                }
                if (m.extraTime()) {
                    extraTimeCount++;
                }
            }

            stats.put("averageGoalsPerMatch", totalGoals / count);
            stats.put("averageCardsPerMatch", totalCards / count);
            stats.put("averageSubstitutionsPerMatch", totalSubs / count);
            stats.put("averagePossession", totalPossession / count);
            stats.put("averageXGPerMatch", xGCount == 0 ? null : totalXG / xGCount);
            stats.put("penaltyShootoutFrequency", (double) shootoutsCount / count);
            stats.put("extraTimeFrequency", (double) extraTimeCount / count);
        } else {
            stats.put("averageGoalsPerMatch", 0.0);
            stats.put("averageCardsPerMatch", 0.0);
            stats.put("averageSubstitutionsPerMatch", 0.0);
            stats.put("averagePossession", 0.0);
            stats.put("averageXGPerMatch", null);
            stats.put("penaltyShootoutFrequency", 0.0);
            stats.put("extraTimeFrequency", 0.0);
        }

        // Add performance timings
        Map<String, Map<String, Object>> timings = new LinkedHashMap<>();
        for (Map.Entry<String, LongSummary> entry : executionStats.entrySet()) {
            LongSummary sum = entry.getValue();
            Map<String, Object> opStats = new LinkedHashMap<>();
            opStats.put("count", sum.getCount());
            opStats.put("averageMs", sum.getAvg());
            opStats.put("minMs", sum.getMin());
            opStats.put("maxMs", sum.getMax());
            timings.put(entry.getKey(), opStats);
        }
        stats.put("executionTimings", timings);

        return stats;
    }
}
