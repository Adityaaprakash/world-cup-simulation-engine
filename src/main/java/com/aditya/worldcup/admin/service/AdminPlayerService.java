package com.aditya.worldcup.admin.service;

import com.aditya.worldcup.admin.dto.BulkPlayerUpdateItem;
import com.aditya.worldcup.admin.dto.BulkPlayerUpdateRequest;
import com.aditya.worldcup.admin.dto.BulkPlayerUpdateResponse;
import com.aditya.worldcup.admin.dto.PlayerUpdateRequest;
import com.aditya.worldcup.admin.dto.ValidationMessage;
import com.aditya.worldcup.players.dto.PlayerResponse;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminPlayerService {

    private static final String ENTITY_TYPE_PLAYER = "PLAYER";
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 99;

    private final PlayerRepository playerRepository;
    private final AdminAuditService adminAuditService;

    @Transactional
    public PlayerResponse updatePlayer(
            Long playerId,
            PlayerUpdateRequest request,
            Authentication authentication) {

        Player player = findPlayer(playerId);
        List<ValidationMessage> messages = validateUpdate(playerId, request);
        if (!messages.isEmpty()) {
            throw new IllegalArgumentException(messages.get(0).message());
        }
        applyUpdate(player, request);
        Player saved = playerRepository.save(player);
        audit(authentication, "PLAYER_UPDATED", saved.getId());
        return map(saved);
    }

    @Transactional
    public PlayerResponse activatePlayer(Long playerId, Authentication authentication) {
        Player player = findPlayer(playerId);
        if (Boolean.TRUE.equals(player.getRetired())) {
            throw new IllegalStateException("Retired players must be restored before activation");
        }
        player.setActive(true);
        Player saved = playerRepository.save(player);
        audit(authentication, "PLAYER_ACTIVATED", saved.getId());
        return map(saved);
    }

    @Transactional
    public PlayerResponse deactivatePlayer(Long playerId, Authentication authentication) {
        Player player = findPlayer(playerId);
        player.setActive(false);
        Player saved = playerRepository.save(player);
        audit(authentication, "PLAYER_DEACTIVATED", saved.getId());
        return map(saved);
    }

    @Transactional
    public PlayerResponse retirePlayer(Long playerId, Authentication authentication) {
        Player player = findPlayer(playerId);
        player.setActive(false);
        player.setRetired(true);
        Player saved = playerRepository.save(player);
        audit(authentication, "PLAYER_RETIRED", saved.getId());
        return map(saved);
    }

    @Transactional
    public PlayerResponse restorePlayer(Long playerId, Authentication authentication) {
        Player player = findPlayer(playerId);
        player.setRetired(false);
        player.setActive(true);
        Player saved = playerRepository.save(player);
        audit(authentication, "PLAYER_RESTORED", saved.getId());
        return map(saved);
    }

    @Transactional
    public BulkPlayerUpdateResponse bulkUpdate(
            BulkPlayerUpdateRequest request,
            Authentication authentication) {

        List<BulkPlayerUpdateItem> updates = request == null || request.updates() == null
                ? List.of()
                : request.updates();
        List<ValidationMessage> messages = new ArrayList<>();
        if (updates.isEmpty()) {
            messages.add(message(null, "Bulk update must contain at least one player update"));
            return new BulkPlayerUpdateResponse(0, 1, messages);
        }

        Map<Long, Integer> occurrences = new HashMap<>();
        for (BulkPlayerUpdateItem item : updates) {
            Long playerId = item == null ? null : item.playerId();
            occurrences.merge(playerId, 1, Integer::sum);
        }

        int updated = 0;
        int failed = 0;
        for (BulkPlayerUpdateItem item : updates) {
            if (item == null || item.playerId() == null) {
                failed++;
                messages.add(message(null, "Player id is required"));
                continue;
            }
            if (occurrences.get(item.playerId()) > 1) {
                failed++;
                messages.add(message(item.playerId(), "Duplicate player id in bulk update"));
                continue;
            }

            Player player = playerRepository.findById(item.playerId()).orElse(null);
            if (player == null) {
                failed++;
                messages.add(message(item.playerId(), "Player was not found"));
                continue;
            }

            List<ValidationMessage> itemMessages = validateUpdate(item.playerId(), item.update());
            if (!itemMessages.isEmpty()) {
                failed++;
                messages.addAll(itemMessages);
                continue;
            }

            applyUpdate(player, item.update());
            playerRepository.save(player);
            audit(authentication, "PLAYER_UPDATED", player.getId());
            updated++;
        }

        adminAuditService.log(username(authentication), "BULK_PLAYER_UPDATE", "PLAYER_BULK", 0L);
        return new BulkPlayerUpdateResponse(updated, failed, List.copyOf(messages));
    }

    private Player findPlayer(Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
    }

    private List<ValidationMessage> validateUpdate(Long playerId, PlayerUpdateRequest request) {
        List<ValidationMessage> messages = new ArrayList<>();
        if (request == null) {
            messages.add(message(playerId, "Player update is required"));
            return messages;
        }
        if (request.overallRating() == null && request.pace() == null
                && request.shooting() == null && request.passing() == null
                && request.dribbling() == null && request.defending() == null
                && request.physical() == null && request.potential() == null) {
            messages.add(message(playerId, "At least one rating must be supplied"));
            return messages;
        }
        validateRating(playerId, "overall rating", request.overallRating(), messages);
        validateRating(playerId, "pace", request.pace(), messages);
        validateRating(playerId, "shooting", request.shooting(), messages);
        validateRating(playerId, "passing", request.passing(), messages);
        validateRating(playerId, "dribbling", request.dribbling(), messages);
        validateRating(playerId, "defending", request.defending(), messages);
        validateRating(playerId, "physical", request.physical(), messages);
        validateRating(playerId, "potential", request.potential(), messages);
        return messages;
    }

    private void validateRating(
            Long playerId,
            String ratingName,
            Integer value,
            List<ValidationMessage> messages) {

        if (value != null && (value < MIN_RATING || value > MAX_RATING)) {
            messages.add(message(playerId, ratingName + " must be between "
                    + MIN_RATING + " and " + MAX_RATING));
        }
    }

    private void applyUpdate(Player player, PlayerUpdateRequest request) {
        if (request.overallRating() != null) player.setOverallRating(request.overallRating());
        if (request.pace() != null) player.setPace(request.pace());
        if (request.shooting() != null) player.setShooting(request.shooting());
        if (request.passing() != null) player.setPassing(request.passing());
        if (request.dribbling() != null) player.setDribbling(request.dribbling());
        if (request.defending() != null) player.setDefending(request.defending());
        if (request.physical() != null) player.setPhysical(request.physical());
        if (request.potential() != null) player.setPotential(request.potential());
    }

    private PlayerResponse map(Player player) {
        return new PlayerResponse(player.getId(), player.getName(),
                player.getPosition() == null ? null : player.getPosition().name(),
                player.getOverallRating());
    }

    private ValidationMessage message(Long playerId, String text) {
        return new ValidationMessage("BULK_PLAYER_UPDATE", "PLAYER", playerId, text);
    }

    private void audit(Authentication authentication, String action, Long playerId) {
        adminAuditService.log(username(authentication), action, ENTITY_TYPE_PLAYER, playerId);
    }

    private String username(Authentication authentication) {
        return authentication == null ? "unknown" : authentication.getName();
    }
}
