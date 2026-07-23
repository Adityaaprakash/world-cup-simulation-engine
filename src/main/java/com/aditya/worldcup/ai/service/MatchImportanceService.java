package com.aditya.worldcup.ai.service;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchRound;
import org.springframework.stereotype.Service;

@Service
public class MatchImportanceService {

    public MatchImportance determine(Match match) {
        if (match == null || match.getRound() == null) {
            return MatchImportance.GROUP_STAGE;
        }
        return determine(match.getRound());
    }

    public MatchImportance determine(MatchRound round) {
        return switch (round) {
            case FINAL -> MatchImportance.FINAL;
            case SEMI_FINALS -> MatchImportance.SEMI_FINAL;
            case ROUND_OF_16, QUARTER_FINALS -> MatchImportance.KNOCKOUT;
            case GROUP_STAGE -> MatchImportance.GROUP_STAGE;
        };
    }
}
