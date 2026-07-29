package com.aditya.worldcup.saves.service;

import com.aditya.worldcup.managers.entity.CareerStatistics;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.repository.CareerStatisticsRepository;
import com.aditya.worldcup.managers.service.ManagerService;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.saves.dto.CreateSaveSlotRequest;
import com.aditya.worldcup.saves.dto.SaveSlotResponse;
import com.aditya.worldcup.saves.dto.UpdateSaveSlotRequest;
import com.aditya.worldcup.saves.entity.SaveSlot;
import com.aditya.worldcup.saves.entity.SaveType;
import com.aditya.worldcup.saves.repository.SaveSlotRepository;
import com.aditya.worldcup.shared.exception.SaveSlotNotFoundException;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import com.aditya.worldcup.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SaveGameService {

    private static final int AUTOSAVE_SLOT_NUMBER = 0;
    private static final String AUTOSAVE_SLOT_NAME = "Autosave";
    private static final String DEFAULT_STAGE = "CAREER";
    private static final String UNKNOWN = "Unknown";

    private final SaveSlotRepository saveSlotRepository;
    private final ManagerService managerService;
    private final CareerStatisticsRepository careerStatisticsRepository;
    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final SquadRepository squadRepository;

    @Transactional
    public List<SaveSlotResponse> listSaves(Authentication authentication) {
        Manager manager = managerService.getOrCreateManager(authentication);
        return saveSlotRepository.findByManagerIdOrderBySlotNumber(
                        manager.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public SaveSlotResponse getSave(
            Long saveId,
            Authentication authentication) {

        Manager manager = managerService.getOrCreateManager(authentication);
        SaveSlot saveSlot = getOwnedSave(saveId, manager);
        return mapToResponse(saveSlot);
    }

    @Transactional
    public SaveSlotResponse createSave(
            CreateSaveSlotRequest request,
            Authentication authentication) {

        Manager manager = managerService.getOrCreateManager(authentication);

        if (saveSlotRepository.existsByManagerIdAndSlotNumber(
                manager.getId(),
                request.slotNumber())) {
            throw new IllegalStateException(
                    "Save slot number already exists");
        }

        validateTournamentReference(request.currentTournamentId());

        LocalDateTime now = LocalDateTime.now();
        SaveSlot saveSlot = SaveSlot.builder()
                .manager(manager)
                .slotName(request.slotName())
                .slotNumber(request.slotNumber())
                .description(request.description())
                .saveType(SaveType.MANUAL)
                .currentTournamentId(request.currentTournamentId())
                .currentSeason(defaultSeason(request.currentSeason(), now))
                .currentStage(defaultStage(request.currentStage()))
                .totalPlayTime(defaultPlayTime(request.totalPlayTime()))
                .createdAt(now)
                .autosave(false)
                .active(false)
                .build();

        refreshMetadata(saveSlot, now);
        return mapToResponse(saveSlotRepository.save(saveSlot));
    }

    @Transactional
    public SaveSlotResponse overwriteSave(
            Long saveId,
            UpdateSaveSlotRequest request,
            Authentication authentication) {

        Manager manager = managerService.getOrCreateManager(authentication);
        SaveSlot saveSlot = getOwnedSave(saveId, manager);

        if (Boolean.TRUE.equals(saveSlot.getAutosave())) {
            throw new IllegalStateException(
                    "Autosave can only be overwritten through the autosave endpoint");
        }

        if (request.currentTournamentId() != null) {
            validateTournamentReference(request.currentTournamentId());
            saveSlot.setCurrentTournamentId(request.currentTournamentId());
        }

        if (request.slotName() != null && !request.slotName().isBlank()) {
            saveSlot.setSlotName(request.slotName());
        }

        if (request.description() != null) {
            saveSlot.setDescription(request.description());
        }

        if (request.currentSeason() != null) {
            saveSlot.setCurrentSeason(request.currentSeason());
        }

        if (request.currentStage() != null && !request.currentStage().isBlank()) {
            saveSlot.setCurrentStage(request.currentStage());
        }

        if (request.totalPlayTime() != null) {
            saveSlot.setTotalPlayTime(request.totalPlayTime());
        }

        refreshMetadata(saveSlot, LocalDateTime.now());
        return mapToResponse(saveSlotRepository.save(saveSlot));
    }

    @Transactional
    public void deleteSave(
            Long saveId,
            Authentication authentication) {

        Manager manager = managerService.getOrCreateManager(authentication);
        SaveSlot saveSlot = getOwnedSave(saveId, manager);

        if (Boolean.TRUE.equals(saveSlot.getAutosave())
                && Boolean.TRUE.equals(saveSlot.getActive())) {
            throw new IllegalStateException(
                    "Active autosave cannot be deleted");
        }

        saveSlotRepository.delete(saveSlot);
    }

    @Transactional
    public SaveSlotResponse autosave(Authentication authentication) {
        Manager manager = managerService.getOrCreateManager(authentication);
        return mapToResponse(autosave(
                manager,
                null,
                "Manual autosave requested"
        ));
    }

    @Transactional
    public SaveSlot autosave(
            Manager manager,
            Long currentTournamentId,
            String reason) {

        if (currentTournamentId != null) {
            validateTournamentReference(currentTournamentId);
        }

        LocalDateTime now = LocalDateTime.now();
        SaveSlot saveSlot = saveSlotRepository
                .findByManagerIdAndSlotNumber(
                        manager.getId(),
                        AUTOSAVE_SLOT_NUMBER
                )
                .orElseGet(() -> SaveSlot.builder()
                        .manager(manager)
                        .slotName(AUTOSAVE_SLOT_NAME)
                        .slotNumber(AUTOSAVE_SLOT_NUMBER)
                        .saveType(SaveType.AUTOSAVE)
                        .createdAt(now)
                        .autosave(true)
                        .active(false)
                        .build());

        saveSlot.setSlotName(AUTOSAVE_SLOT_NAME);
        saveSlot.setSlotNumber(AUTOSAVE_SLOT_NUMBER);
        saveSlot.setSaveType(SaveType.AUTOSAVE);
        saveSlot.setAutosave(true);
        saveSlot.setDescription(reason);
        saveSlot.setCurrentTournamentId(currentTournamentId);
        saveSlot.setCurrentSeason(defaultSeason(
                saveSlot.getCurrentSeason(),
                now
        ));
        saveSlot.setCurrentStage(defaultStage(saveSlot.getCurrentStage()));
        saveSlot.setTotalPlayTime(defaultPlayTime(saveSlot.getTotalPlayTime()));
        refreshMetadata(saveSlot, now);

        return saveSlotRepository.save(saveSlot);
    }

    @Transactional
    public SaveSlotResponse activateSave(
            Long saveId,
            Authentication authentication) {

        Manager manager = managerService.getOrCreateManager(authentication);
        SaveSlot saveSlot = getOwnedSave(saveId, manager);

        saveSlotRepository.findByManagerIdAndActiveTrue(manager.getId())
                .forEach(activeSave -> {
                    activeSave.setActive(false);
                    activeSave.setUpdatedAt(LocalDateTime.now());
                    saveSlotRepository.save(activeSave);
                });

        saveSlot.setActive(true);
        saveSlot.setLastPlayedAt(LocalDateTime.now());
        refreshMetadata(saveSlot, LocalDateTime.now());
        return mapToResponse(saveSlotRepository.save(saveSlot));
    }

    private SaveSlot getOwnedSave(Long saveId, Manager manager) {
        return saveSlotRepository.findByIdAndManagerId(
                        saveId,
                        manager.getId())
                .orElseThrow(() -> new SaveSlotNotFoundException(saveId));
    }

    private void refreshMetadata(
            SaveSlot saveSlot,
            LocalDateTime timestamp) {

        Manager manager = saveSlot.getManager();
        CareerStatistics statistics = careerStatisticsRepository
                .findByManagerId(manager.getId())
                .orElse(null);
        Optional<Tournament> tournament = currentTournament(saveSlot);

        saveSlot.setManagerLevel(manager.getLevel());
        saveSlot.setReputation(manager.getReputation());
        saveSlot.setTournamentsPlayed(statistics == null
                ? 0
                : statistics.getTournamentsManaged());
        saveSlot.setTrophies(statistics == null
                ? 0
                : statistics.getTrophiesWon());
        saveSlot.setCurrentTeam(resolveCurrentTeam(manager));
        saveSlot.setCurrentTournament(tournament
                .map(Tournament::getName)
                .orElse(UNKNOWN));
        saveSlot.setProgressPercentage(tournament
                .map(this::progressPercentage)
                .orElse(0.0));
        saveSlot.setLatestSaveTimestamp(timestamp);
        saveSlot.setUpdatedAt(timestamp);
        saveSlot.setLastPlayedAt(timestamp);
    }

    private Optional<Tournament> currentTournament(SaveSlot saveSlot) {
        if (saveSlot.getCurrentTournamentId() == null) {
            return Optional.empty();
        }
        return tournamentRepository.findById(saveSlot.getCurrentTournamentId());
    }

    private double progressPercentage(Tournament tournament) {
        var matches = matchRepository.findByTournamentIdOrderById(
                tournament.getId());
        if (matches.isEmpty()) {
            return 0.0;
        }

        long completed = matches.stream()
                .filter(match -> match.getStatus() == MatchStatus.FINISHED)
                .count();

        return BigDecimal.valueOf((completed * 100.0) / matches.size())
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String resolveCurrentTeam(Manager manager) {
        return userRepository.findByEmail(manager.getUsername())
                .flatMap(user -> squadRepository.findByUserId(user.getId())
                        .stream()
                        .findFirst())
                .map(Squad::getTeam)
                .map(team -> team.getName())
                .orElse(UNKNOWN);
    }

    private void validateTournamentReference(Long tournamentId) {
        if (tournamentId != null
                && !tournamentRepository.existsById(tournamentId)) {
            throw new IllegalArgumentException(
                    "Tournament not found: " + tournamentId);
        }
    }

    private int defaultSeason(Integer season, LocalDateTime now) {
        return season == null ? now.getYear() : season;
    }

    private String defaultStage(String stage) {
        if (stage == null || stage.isBlank()) {
            return DEFAULT_STAGE;
        }
        return stage;
    }

    private long defaultPlayTime(Long playTime) {
        return playTime == null ? 0L : playTime;
    }

    private SaveSlotResponse mapToResponse(SaveSlot saveSlot) {
        return new SaveSlotResponse(
                saveSlot.getId(),
                saveSlot.getSlotName(),
                saveSlot.getSlotNumber(),
                saveSlot.getDescription(),
                saveSlot.getSaveType(),
                saveSlot.getCurrentTournamentId(),
                saveSlot.getCurrentSeason(),
                saveSlot.getCurrentStage(),
                saveSlot.getTotalPlayTime(),
                saveSlot.getManagerLevel(),
                saveSlot.getReputation(),
                saveSlot.getTournamentsPlayed(),
                saveSlot.getTrophies(),
                saveSlot.getCurrentTeam(),
                saveSlot.getCurrentTournament(),
                saveSlot.getProgressPercentage(),
                saveSlot.getLatestSaveTimestamp(),
                saveSlot.getCreatedAt(),
                saveSlot.getUpdatedAt(),
                saveSlot.getLastPlayedAt(),
                saveSlot.getAutosave(),
                saveSlot.getActive()
        );
    }
}
