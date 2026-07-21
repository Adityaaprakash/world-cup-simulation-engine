package com.aditya.worldcup.tactics.service;

import com.aditya.worldcup.tactics.dto.TacticalProfileUpdateRequest;
import com.aditya.worldcup.tactics.entity.TacticalProfile;
import com.aditya.worldcup.tactics.repository.TacticalProfileRepository;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.teams.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TacticalProfileService {

    private final TacticalProfileRepository tacticalProfileRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public TacticalProfile getOrCreateProfile(Team team) {
        return tacticalProfileRepository.findByTeamId(team.getId())
                .orElseGet(() -> tacticalProfileRepository.save(
                        TacticalProfile.balanced(team)));
    }

    @Transactional
    public TacticalProfile updateProfile(Long teamId,
                                         TacticalProfileUpdateRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));
        TacticalProfile profile = getOrCreateProfile(team);
        profile.setAttackWidth(request.attackWidth());
        profile.setDefensiveWidth(request.defensiveWidth());
        profile.setDefensiveLine(request.defensiveLine());
        profile.setPressingIntensity(request.pressingIntensity());
        profile.setBuildUpStyle(request.buildUpStyle());
        profile.setChanceCreation(request.chanceCreation());
        profile.setAttackingWidth(request.attackingWidth());
        profile.setCrossFrequency(request.crossFrequency());
        profile.setLongBallFrequency(request.longBallFrequency());
        profile.setPassingRisk(request.passingRisk());
        profile.setCounterAttack(request.counterAttack());
        profile.setHighPress(request.highPress());
        profile.setOffsideTrap(request.offsideTrap());
        profile.setTimeWasting(request.timeWasting());
        return tacticalProfileRepository.save(profile);
    }
}
