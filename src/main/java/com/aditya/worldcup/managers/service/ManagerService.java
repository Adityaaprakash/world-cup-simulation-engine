package com.aditya.worldcup.managers.service;

import com.aditya.worldcup.managers.dto.ManagerResponse;
import com.aditya.worldcup.managers.entity.CoachingStyle;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.entity.ManagerReputation;
import com.aditya.worldcup.managers.repository.ManagerRepository;
import com.aditya.worldcup.users.entity.User;
import com.aditya.worldcup.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ManagerService {

    private static final String DEFAULT_NATIONALITY = "Unknown";
    private static final String DEFAULT_FORMATION = "4-3-3";
    private static final String DEFAULT_TACTICAL_PROFILE = "Balanced";

    private final ManagerRepository managerRepository;
    private final UserRepository userRepository;
    private final CareerProgressionService careerProgressionService;

    @Transactional
    public Manager getOrCreateManager(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }

        return getOrCreateManager(authentication.getName());
    }

    @Transactional
    public Manager getOrCreateManager(String username) {
        return managerRepository.findByUsername(username)
                .orElseGet(() -> createManager(username));
    }

    @Transactional
    public ManagerResponse getCurrentManager(Authentication authentication) {
        Manager manager = getOrCreateManager(authentication);
        return mapToResponse(manager);
    }

    @Transactional
    public ProgressionResult addExperience(Manager manager, int experience) {
        ManagerReputation previousReputation = manager.getReputation();
        int previousLevel = manager.getLevel();

        if (experience <= 0) {
            return new ProgressionResult(
                    previousLevel,
                    previousLevel,
                    previousReputation,
                    previousReputation
            );
        }

        manager.setExperiencePoints(manager.getExperiencePoints() + experience);
        manager.setLevel(careerProgressionService.calculateLevel(
                manager.getExperiencePoints()));
        manager.setReputation(careerProgressionService.calculateReputation(
                manager.getLevel()));
        manager.setUpdatedAt(LocalDateTime.now());
        managerRepository.save(manager);

        return new ProgressionResult(
                previousLevel,
                manager.getLevel(),
                previousReputation,
                manager.getReputation()
        );
    }

    public ManagerResponse mapToResponse(Manager manager) {
        return new ManagerResponse(
                manager.getId(),
                manager.getUsername(),
                manager.getDisplayName(),
                manager.getNationality(),
                manager.getFavoriteFormation(),
                manager.getFavoriteTacticalProfile(),
                manager.getCoachingStyle(),
                manager.getReputation(),
                manager.getExperiencePoints(),
                manager.getLevel(),
                manager.getCreatedAt(),
                manager.getUpdatedAt()
        );
    }

    private Manager createManager(String username) {
        LocalDateTime now = LocalDateTime.now();
        String displayName = userRepository.findByEmail(username)
                .map(User::getEmail)
                .map(this::displayNameFromEmail)
                .orElseGet(() -> displayNameFromEmail(username));

        Manager manager = Manager.builder()
                .username(username)
                .displayName(displayName)
                .nationality(DEFAULT_NATIONALITY)
                .favoriteFormation(DEFAULT_FORMATION)
                .favoriteTacticalProfile(DEFAULT_TACTICAL_PROFILE)
                .coachingStyle(CoachingStyle.BALANCED)
                .reputation(ManagerReputation.AMATEUR)
                .experiencePoints(0)
                .level(1)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return managerRepository.save(manager);
    }

    private String displayNameFromEmail(String username) {
        if (username == null || username.isBlank()) {
            return "Manager";
        }

        int atIndex = username.indexOf('@');
        if (atIndex <= 0) {
            return username;
        }

        return username.substring(0, atIndex);
    }

    public record ProgressionResult(
            int previousLevel,
            int currentLevel,
            ManagerReputation previousReputation,
            ManagerReputation currentReputation
    ) {

        public boolean levelChanged() {
            return currentLevel > previousLevel;
        }

        public boolean reputationChanged() {
            return currentReputation != previousReputation;
        }
    }
}
