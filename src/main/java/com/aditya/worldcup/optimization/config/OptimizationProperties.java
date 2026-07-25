package com.aditya.worldcup.optimization.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "simulation.optimization")
@Getter
@Setter
public class OptimizationProperties {
    private int benchmarkIterations = 100;
    private int tournamentBenchmarkIterations = 3;
    private long analyticsCacheDurationMs = 60000; // 60 seconds
    private long cacheDurationMs = 60000;
    private int metricsRetention = 1000;
}
