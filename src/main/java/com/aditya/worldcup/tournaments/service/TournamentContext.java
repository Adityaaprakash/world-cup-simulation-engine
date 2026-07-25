package com.aditya.worldcup.tournaments.service;

import com.aditya.worldcup.matches.entity.MatchRound;
import com.aditya.worldcup.teams.entity.Team;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TournamentContext {

    private MatchRound currentStage;
    private long remainingFixtures;
    private boolean knockoutStage;
    private List<Team> favourites = List.of();
    private List<Team> darkHorses = List.of();
    private TournamentUpset biggestUpset;
    private Team highestScoringTeam;
    private Team bestDefensiveTeam;
    private Team longestWinningStreakTeam;
    private Team longestUnbeatenStreakTeam;
    private Team longestCleanSheetStreakTeam;
    private Team longestLossStreakTeam;
    private Map<Long, TeamTournamentProfile> teamProfiles = new LinkedHashMap<>();

    public MatchRound getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(MatchRound currentStage) {
        this.currentStage = currentStage;
    }

    public long getRemainingFixtures() {
        return remainingFixtures;
    }

    public void setRemainingFixtures(long remainingFixtures) {
        this.remainingFixtures = remainingFixtures;
    }

    public boolean isKnockoutStage() {
        return knockoutStage;
    }

    public void setKnockoutStage(boolean knockoutStage) {
        this.knockoutStage = knockoutStage;
    }

    public List<Team> getFavourites() {
        return favourites;
    }

    public void setFavourites(List<Team> favourites) {
        this.favourites = favourites;
    }

    public List<Team> getDarkHorses() {
        return darkHorses;
    }

    public void setDarkHorses(List<Team> darkHorses) {
        this.darkHorses = darkHorses;
    }

    public TournamentUpset getBiggestUpset() {
        return biggestUpset;
    }

    public void setBiggestUpset(TournamentUpset biggestUpset) {
        this.biggestUpset = biggestUpset;
    }

    public Team getHighestScoringTeam() {
        return highestScoringTeam;
    }

    public void setHighestScoringTeam(Team highestScoringTeam) {
        this.highestScoringTeam = highestScoringTeam;
    }

    public Team getBestDefensiveTeam() {
        return bestDefensiveTeam;
    }

    public void setBestDefensiveTeam(Team bestDefensiveTeam) {
        this.bestDefensiveTeam = bestDefensiveTeam;
    }

    public Team getLongestWinningStreakTeam() {
        return longestWinningStreakTeam;
    }

    public void setLongestWinningStreakTeam(Team longestWinningStreakTeam) {
        this.longestWinningStreakTeam = longestWinningStreakTeam;
    }

    public Team getLongestUnbeatenStreakTeam() {
        return longestUnbeatenStreakTeam;
    }

    public void setLongestUnbeatenStreakTeam(Team longestUnbeatenStreakTeam) {
        this.longestUnbeatenStreakTeam = longestUnbeatenStreakTeam;
    }

    public Team getLongestCleanSheetStreakTeam() {
        return longestCleanSheetStreakTeam;
    }

    public void setLongestCleanSheetStreakTeam(Team longestCleanSheetStreakTeam) {
        this.longestCleanSheetStreakTeam = longestCleanSheetStreakTeam;
    }

    public Team getLongestLossStreakTeam() {
        return longestLossStreakTeam;
    }

    public void setLongestLossStreakTeam(Team longestLossStreakTeam) {
        this.longestLossStreakTeam = longestLossStreakTeam;
    }

    public Map<Long, TeamTournamentProfile> getTeamProfiles() {
        return teamProfiles;
    }

    public void setTeamProfiles(Map<Long, TeamTournamentProfile> teamProfiles) {
        this.teamProfiles = teamProfiles;
    }
}
