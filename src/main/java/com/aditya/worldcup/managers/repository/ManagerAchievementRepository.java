package com.aditya.worldcup.managers.repository;

import com.aditya.worldcup.managers.entity.AchievementCode;
import com.aditya.worldcup.managers.entity.ManagerAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManagerAchievementRepository
        extends JpaRepository<ManagerAchievement, Long> {

    List<ManagerAchievement> findByManagerIdOrderByUnlockedAtDesc(Long managerId);

    boolean existsByManagerIdAndAchievementCode(
            Long managerId,
            AchievementCode achievementCode
    );
}
