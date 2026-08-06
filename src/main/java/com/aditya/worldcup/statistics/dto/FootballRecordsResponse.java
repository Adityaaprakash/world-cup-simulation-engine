package com.aditya.worldcup.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FootballRecordsResponse {
    
    private PlayerRecords playerRecords;
    private TeamRecords teamRecords;
    private MatchRecords matchRecords;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerRecords {
        private List<RecordEntry> topScorers;
        private List<RecordEntry> topAssisters;
        private List<RecordEntry> mostAppearances;
        private List<RecordEntry> mostCleanSheets;
        private List<RecordEntry> highestAverageRatings;
        private List<RecordEntry> mostYellowCards;
        private List<RecordEntry> mostRedCards;
        private List<RecordEntry> youngestScorers;
        private List<RecordEntry> oldestScorers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamRecords {
        private List<RecordEntry> longestWinningStreaks;
        private List<RecordEntry> longestUnbeatenStreaks;
        private List<RecordEntry> longestCleanSheetStreaks;
        private List<RecordEntry> biggestVictories;
        private List<RecordEntry> biggestDefeats;
        private List<RecordEntry> mostTitles;
        private List<RecordEntry> mostFinals;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchRecords {
        private List<RecordEntry> highestScoringMatches;
        private List<RecordEntry> biggestComebacks;
        private List<RecordEntry> longestPenaltyShootouts;
        private List<RecordEntry> mostCards;
        private List<RecordValueEntry> fastestGoals;
        private List<RecordValueEntry> latestGoals;
        private List<RecordEntry> mostSubstitutions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordEntry {
        private Long id;
        private String name;
        private String detail;
        private long value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordValueEntry {
        private Long id;
        private String name;
        private String detail;
        private double value;
    }
}
