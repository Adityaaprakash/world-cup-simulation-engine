package com.aditya.worldcup.tournaments.service;

import com.aditya.worldcup.teams.entity.Team;

public class TeamTournamentProfile {

    private final Team team;
    private final TeamReputation reputation;
    private String form = "";
    private int goalsFor;
    private int goalsAgainst;
    private int winningStreak;
    private int unbeatenStreak;
    private int cleanSheetStreak;
    private int scoringStreak;
    private int lossStreak;
    private int longestWinningStreak;
    private int longestUnbeatenStreak;
    private int longestCleanSheetStreak;
    private int longestLossStreak;
    private double momentum;

    public TeamTournamentProfile(Team team, TeamReputation reputation) {
        this.team = team;
        this.reputation = reputation;
    }

    public Team getTeam() {
        return team;
    }

    public TeamReputation getReputation() {
        return reputation;
    }

    public String getForm() {
        return form;
    }

    public void addResult(char result) {
        form = (form + result);
        if (form.length() > 5) {
            form = form.substring(form.length() - 5);
        }
    }

    public int getGoalsFor() {
        return goalsFor;
    }

    public void addGoalsFor(int goalsFor) {
        this.goalsFor += goalsFor;
    }

    public int getGoalsAgainst() {
        return goalsAgainst;
    }

    public void addGoalsAgainst(int goalsAgainst) {
        this.goalsAgainst += goalsAgainst;
    }

    public int getWinningStreak() {
        return winningStreak;
    }

    public int getUnbeatenStreak() {
        return unbeatenStreak;
    }

    public int getCleanSheetStreak() {
        return cleanSheetStreak;
    }

    public int getScoringStreak() {
        return scoringStreak;
    }

    public int getLossStreak() {
        return lossStreak;
    }

    public int getLongestWinningStreak() {
        return longestWinningStreak;
    }

    public int getLongestUnbeatenStreak() {
        return longestUnbeatenStreak;
    }

    public int getLongestCleanSheetStreak() {
        return longestCleanSheetStreak;
    }

    public int getLongestLossStreak() {
        return longestLossStreak;
    }

    public double getMomentum() {
        return momentum;
    }

    public void addMomentum(double value) {
        momentum = Math.max(-3.0, Math.min(3.0, momentum + value));
    }

    public void applyResult(int goalsFor, int goalsAgainst, double ratingDifference) {
        addGoalsFor(goalsFor);
        addGoalsAgainst(goalsAgainst);
        if (goalsFor > goalsAgainst) {
            addResult('W');
            winningStreak++;
            unbeatenStreak++;
            lossStreak = 0;
            addMomentum(0.35 + Math.max(0, goalsFor - goalsAgainst - 1) * 0.15);
            if (ratingDifference < -6) {
                addMomentum(0.65);
            }
        } else if (goalsFor == goalsAgainst) {
            addResult('D');
            winningStreak = 0;
            unbeatenStreak++;
            lossStreak = 0;
            addMomentum(0.05);
        } else {
            addResult('L');
            winningStreak = 0;
            unbeatenStreak = 0;
            lossStreak++;
            addMomentum(-0.35 - Math.max(0, goalsAgainst - goalsFor - 1) * 0.2);
        }

        if (goalsAgainst == 0) {
            cleanSheetStreak++;
        } else {
            cleanSheetStreak = 0;
        }
        if (goalsFor > 0) {
            scoringStreak++;
        } else {
            scoringStreak = 0;
        }
        longestWinningStreak = Math.max(longestWinningStreak, winningStreak);
        longestUnbeatenStreak = Math.max(longestUnbeatenStreak, unbeatenStreak);
        longestCleanSheetStreak = Math.max(longestCleanSheetStreak, cleanSheetStreak);
        longestLossStreak = Math.max(longestLossStreak, lossStreak);
    }
}
