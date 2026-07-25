package com.aditya.worldcup.tournaments.service;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.matchevents.repository.MatchEventRepository;
import com.aditya.worldcup.matchstatistics.entity.MatchStatistics;
import com.aditya.worldcup.matchstatistics.repository.MatchStatisticsRepository;
import com.aditya.worldcup.tournaments.dto.MatchNarrativeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchNarrativeService {

    private final MatchStatisticsRepository matchStatisticsRepository;
    private final MatchEventRepository matchEventRepository;

    @Transactional(readOnly = true)
    public MatchNarrativeResponse generate(Match match) {
        MatchStatistics statistics = matchStatisticsRepository.findByMatchId(match.getId())
                .orElse(null);
        String headline = headline(match);
        String narrative = narrative(match, statistics);
        return new MatchNarrativeResponse(match.getId(), headline, narrative);
    }

    private String headline(Match match) {
        if (isUpset(match)) {
            return winner(match).getName() + " complete a stunning upset";
        }
        if (goalTotal(match) >= 5) {
            return match.getHomeTeam().getName() + " and "
                    + match.getAwayTeam().getName() + " deliver a thriller";
        }
        if (match.getHomeScore().equals(match.getAwayScore())) {
            return "Nothing separates " + match.getHomeTeam().getName()
                    + " and " + match.getAwayTeam().getName();
        }
        return winner(match).getName() + " edge past " + loser(match).getName();
    }

    private String narrative(Match match, MatchStatistics statistics) {
        if (statistics == null) {
            return scoreline(match) + " decided the match.";
        }
        if (isUpset(match)) {
            return winner(match).getName() + " beat a higher-rated "
                    + loser(match).getName() + " side by staying clinical under pressure.";
        }
        if (lateGoal(match)) {
            return "Late pressure shaped the result as " + winner(match).getName()
                    + " found a decisive moment after 75 minutes.";
        }
        if (dominantPossessionWithoutWin(match, statistics)) {
            return possessionLeader(match, statistics).getName()
                    + " dominated possession but failed to convert enough chances.";
        }
        if (goalTotal(match) >= 5) {
            return "Both teams traded momentum in an open match with "
                    + goalTotal(match) + " goals.";
        }
        if (cleanSheetWin(match)) {
            return winner(match).getName()
                    + " controlled the defensive rhythm and protected a clean sheet.";
        }
        return "The match was decided by small margins, with tactical balance keeping chances limited.";
    }

    private boolean lateGoal(Match match) {
        return matchEventRepository.findByMatchId(match.getId())
                .stream()
                .anyMatch(event -> event.getEventType() == MatchEventType.GOAL
                        && event.getMinute() != null
                        && event.getMinute() >= 76);
    }

    private boolean dominantPossessionWithoutWin(Match match, MatchStatistics statistics) {
        if (match.getHomeScore().equals(match.getAwayScore())) {
            return Math.abs(statistics.getHomePossession() - statistics.getAwayPossession()) >= 12;
        }
        boolean homeWon = match.getHomeScore() > match.getAwayScore();
        return homeWon
                ? statistics.getAwayPossession() >= 58
                : statistics.getHomePossession() >= 58;
    }

    private com.aditya.worldcup.teams.entity.Team possessionLeader(Match match,
                                                                   MatchStatistics statistics) {
        return statistics.getHomePossession() >= statistics.getAwayPossession()
                ? match.getHomeTeam()
                : match.getAwayTeam();
    }

    private boolean isUpset(Match match) {
        if (match.getHomeScore().equals(match.getAwayScore())) {
            return false;
        }
        return loser(match).getOverallRating() - winner(match).getOverallRating() >= 8;
    }

    private boolean cleanSheetWin(Match match) {
        return (match.getHomeScore() > match.getAwayScore() && match.getAwayScore() == 0)
                || (match.getAwayScore() > match.getHomeScore() && match.getHomeScore() == 0);
    }

    private int goalTotal(Match match) {
        return match.getHomeScore() + match.getAwayScore();
    }

    private String scoreline(Match match) {
        return match.getHomeTeam().getName() + " " + match.getHomeScore()
                + "-" + match.getAwayScore() + " " + match.getAwayTeam().getName();
    }

    private com.aditya.worldcup.teams.entity.Team winner(Match match) {
        return match.getHomeScore() >= match.getAwayScore()
                ? match.getHomeTeam()
                : match.getAwayTeam();
    }

    private com.aditya.worldcup.teams.entity.Team loser(Match match) {
        return winner(match).getId().equals(match.getHomeTeam().getId())
                ? match.getAwayTeam()
                : match.getHomeTeam();
    }
}
