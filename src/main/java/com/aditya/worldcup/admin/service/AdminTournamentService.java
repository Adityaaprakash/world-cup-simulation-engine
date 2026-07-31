package com.aditya.worldcup.admin.service;

import com.aditya.worldcup.tournaments.dto.TournamentResponse;
import com.aditya.worldcup.tournaments.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTournamentService {

    private static final String ENTITY_TYPE_TOURNAMENT = "TOURNAMENT";

    private final TournamentService tournamentService;
    private final AdminAuditService adminAuditService;

    public List<TournamentResponse> listTournaments() {
        return tournamentService.getAllTournaments();
    }

    @Transactional
    public TournamentResponse archiveTournament(
            Long tournamentId,
            Authentication authentication) {

        TournamentResponse response = tournamentService
                .archiveTournament(tournamentId);
        audit(authentication, "ARCHIVE_TOURNAMENT", tournamentId);
        return response;
    }

    @Transactional
    public TournamentResponse reopenTournament(
            Long tournamentId,
            Authentication authentication) {

        TournamentResponse response = tournamentService
                .reopenArchivedTournament(tournamentId);
        audit(authentication, "REOPEN_TOURNAMENT", tournamentId);
        return response;
    }

    @Transactional
    public TournamentResponse resetTournament(
            Long tournamentId,
            Authentication authentication) {

        TournamentResponse response = tournamentService
                .resetTournament(tournamentId);
        audit(authentication, "RESET_TOURNAMENT", tournamentId);
        return response;
    }

    @Transactional
    public void deleteTournament(
            Long tournamentId,
            Authentication authentication) {

        tournamentService.deleteInactiveTournament(tournamentId);
        audit(authentication, "DELETE_TOURNAMENT", tournamentId);
    }

    private void audit(
            Authentication authentication,
            String action,
            Long tournamentId) {

        adminAuditService.log(
                authentication == null ? "unknown" : authentication.getName(),
                action,
                ENTITY_TYPE_TOURNAMENT,
                tournamentId
        );
    }
}
