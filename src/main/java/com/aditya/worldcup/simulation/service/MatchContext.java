package com.aditya.worldcup.simulation.service;

public class MatchContext {

    private double homeMomentum;
    private double awayMomentum;
    private double homePressure;
    private double awayPressure;
    private double matchIntensity;
    private double homeAttackingIntent;
    private double awayAttackingIntent;
    private double homeDefensiveConfidence;
    private double awayDefensiveConfidence;
    private MatchPhase currentPhase;
    private WeatherCondition weather;
    private int homeConsecutiveAttacks;
    private int awayConsecutiveAttacks;
    private boolean extraTime;

    public MatchContext(WeatherCondition weather) {
        this.weather = weather;
        this.currentPhase = MatchPhase.OPENING;
        this.homeMomentum = 0.4;
        this.awayMomentum = 0.0;
        this.homePressure = 0.2;
        this.awayPressure = 0.3;
        this.matchIntensity = 1.0;
        this.homeAttackingIntent = 1.0;
        this.awayAttackingIntent = 1.0;
        this.homeDefensiveConfidence = 1.0;
        this.awayDefensiveConfidence = 1.0;
    }

    public double getHomeMomentum() {
        return homeMomentum;
    }

    public void setHomeMomentum(double homeMomentum) {
        this.homeMomentum = homeMomentum;
    }

    public double getAwayMomentum() {
        return awayMomentum;
    }

    public void setAwayMomentum(double awayMomentum) {
        this.awayMomentum = awayMomentum;
    }

    public double getHomePressure() {
        return homePressure;
    }

    public void setHomePressure(double homePressure) {
        this.homePressure = homePressure;
    }

    public double getAwayPressure() {
        return awayPressure;
    }

    public void setAwayPressure(double awayPressure) {
        this.awayPressure = awayPressure;
    }

    public double getMatchIntensity() {
        return matchIntensity;
    }

    public void setMatchIntensity(double matchIntensity) {
        this.matchIntensity = matchIntensity;
    }

    public double getHomeAttackingIntent() {
        return homeAttackingIntent;
    }

    public void setHomeAttackingIntent(double homeAttackingIntent) {
        this.homeAttackingIntent = homeAttackingIntent;
    }

    public double getAwayAttackingIntent() {
        return awayAttackingIntent;
    }

    public void setAwayAttackingIntent(double awayAttackingIntent) {
        this.awayAttackingIntent = awayAttackingIntent;
    }

    public double getHomeDefensiveConfidence() {
        return homeDefensiveConfidence;
    }

    public void setHomeDefensiveConfidence(double homeDefensiveConfidence) {
        this.homeDefensiveConfidence = homeDefensiveConfidence;
    }

    public double getAwayDefensiveConfidence() {
        return awayDefensiveConfidence;
    }

    public void setAwayDefensiveConfidence(double awayDefensiveConfidence) {
        this.awayDefensiveConfidence = awayDefensiveConfidence;
    }

    public MatchPhase getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(MatchPhase currentPhase) {
        this.currentPhase = currentPhase;
    }

    public WeatherCondition getWeather() {
        return weather;
    }

    public int getHomeConsecutiveAttacks() {
        return homeConsecutiveAttacks;
    }

    public void setHomeConsecutiveAttacks(int homeConsecutiveAttacks) {
        this.homeConsecutiveAttacks = homeConsecutiveAttacks;
    }

    public int getAwayConsecutiveAttacks() {
        return awayConsecutiveAttacks;
    }

    public void setAwayConsecutiveAttacks(int awayConsecutiveAttacks) {
        this.awayConsecutiveAttacks = awayConsecutiveAttacks;
    }

    public boolean isExtraTime() {
        return extraTime;
    }

    public void setExtraTime(boolean extraTime) {
        this.extraTime = extraTime;
    }
}
