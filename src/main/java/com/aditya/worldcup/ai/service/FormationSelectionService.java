package com.aditya.worldcup.ai.service;

import com.aditya.worldcup.formations.entity.Formation;
import com.aditya.worldcup.formations.repository.FormationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FormationSelectionService {

    private final FormationRepository formationRepository;

    public Formation selectFormation(int squadQuality, int opponentQuality,
                                     Formation currentFormation) {
        List<Formation> formations = formationRepository.findAll();
        if (formations.isEmpty()) {
            return currentFormation;
        }

        int targetDefenders;
        int targetMidfielders;
        int targetAttackers;
        int difference = squadQuality - opponentQuality;
        if (difference >= 5) {
            targetDefenders = 4;
            targetMidfielders = 3;
            targetAttackers = 3;
        } else if (difference <= -5) {
            targetDefenders = 5;
            targetMidfielders = 4;
            targetAttackers = 1;
        } else {
            targetDefenders = 4;
            targetMidfielders = 4;
            targetAttackers = 2;
        }

        return formations.stream()
                .min(Comparator.comparingInt(formation ->
                        Math.abs(formation.getDefenders() - targetDefenders)
                                + Math.abs(formation.getMidfielders() - targetMidfielders)
                                + Math.abs(formation.getAttackers() - targetAttackers)))
                .orElse(currentFormation);
    }
}
