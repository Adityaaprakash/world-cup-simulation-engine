package com.aditya.worldcup.managers.service;

import com.aditya.worldcup.managers.entity.ManagerReputation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CareerProgressionService {

    private final int winExperience;
    private final int drawExperience;
    private final int knockoutExperience;
    private final int finalExperience;
    private final int tournamentVictoryExperience;
    private final int awardExperience;
    private final int experiencePerLevel;

    public CareerProgressionService(
            @Value("${manager.progression.win-experience:30}")
            int winExperience,
            @Value("${manager.progression.draw-experience:10}")
            int drawExperience,
            @Value("${manager.progression.knockout-experience:40}")
            int knockoutExperience,
            @Value("${manager.progression.final-experience:100}")
            int finalExperience,
            @Value("${manager.progression.tournament-victory-experience:250}")
            int tournamentVictoryExperience,
            @Value("${manager.progression.award-experience:25}")
            int awardExperience,
            @Value("${manager.progression.experience-per-level:100}")
            int experiencePerLevel
    ) {
        this.winExperience = winExperience;
        this.drawExperience = drawExperience;
        this.knockoutExperience = knockoutExperience;
        this.finalExperience = finalExperience;
        this.tournamentVictoryExperience = tournamentVictoryExperience;
        this.awardExperience = awardExperience;
        this.experiencePerLevel = Math.max(1, experiencePerLevel);
    }

    public int calculateExperience(
            boolean victory,
            boolean draw,
            boolean reachedKnockout,
            boolean reachedFinal,
            boolean tournamentVictory,
            int awardsWon
    ) {
        int experience = 0;

        if (victory) {
            experience += winExperience;
        }

        if (draw) {
            experience += drawExperience;
        }

        if (reachedKnockout) {
            experience += knockoutExperience;
        }

        if (reachedFinal) {
            experience += finalExperience;
        }

        if (tournamentVictory) {
            experience += tournamentVictoryExperience;
        }

        experience += Math.max(0, awardsWon) * awardExperience;

        return experience;
    }

    public int calculateLevel(int experiencePoints) {
        return Math.max(1, (Math.max(0, experiencePoints) / experiencePerLevel) + 1);
    }

    public ManagerReputation calculateReputation(int level) {
        if (level >= 25) {
            return ManagerReputation.LEGENDARY;
        }
        if (level >= 18) {
            return ManagerReputation.WORLD_CLASS;
        }
        if (level >= 10) {
            return ManagerReputation.ELITE;
        }
        if (level >= 4) {
            return ManagerReputation.PROFESSIONAL;
        }
        return ManagerReputation.AMATEUR;
    }
}
