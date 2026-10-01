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
        profile.setPressingIntensity(request.pressingIntensity());
        profile.setDefensiveLine(request.defensiveLine());
        profile.setTempo(request.tempo());
        profile.setWidth(request.width());
        profile.setPassingStyle(request.passingStyle());
        profile.setAttackingApproach(request.attackingApproach());
        profile.setBuildUpStyle(request.buildUpStyle());
        profile.setDefensiveBlock(request.defensiveBlock());
        return tacticalProfileRepository.save(profile);
    }

    @Transactional
    public TacticalProfile saveProfile(TacticalProfile profile) {
        return tacticalProfileRepository.save(profile);
    }
}
