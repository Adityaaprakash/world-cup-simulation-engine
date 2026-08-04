package com.aditya.worldcup.admin.service;

import com.aditya.worldcup.admin.dto.DatasetHealthResponse;
import com.aditya.worldcup.admin.dto.ValidationMessage;
import com.aditya.worldcup.admin.dto.ValidationResponse;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.teams.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DatasetHealthService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final DatasetValidationService datasetValidationService;

    @Transactional(readOnly = true)
    public DatasetHealthResponse health() {
        ValidationResponse validation = datasetValidationService.validateDataset();
        long duplicatePlayers = count(validation, "DUPLICATE_PLAYER");
        long invalidRatings = count(validation, "INVALID_RATING");
        long invalidSquads = count(validation, "INVALID_SQUAD");

        return new DatasetHealthResponse(
                new DatasetHealthResponse.PlayerSummary(
                        playerRepository.countByActiveTrueAndRetiredFalse(),
                        playerRepository.countByActiveFalseAndRetiredFalse(),
                        playerRepository.countByRetiredTrue()),
                new DatasetHealthResponse.TeamSummary(
                        teamRepository.countByActiveTrue(),
                        teamRepository.countByActiveFalse()),
                new DatasetHealthResponse.ValidationSummary(
                        duplicatePlayers, invalidRatings, invalidSquads),
                LocalDateTime.now());
    }

    private long count(ValidationResponse response, String code) {
        return response.messages().stream()
                .map(ValidationMessage::code)
                .filter(code::equals)
                .count();
    }
}
