package com.aditya.worldcup.live.controller;

import com.aditya.worldcup.live.dto.LiveMatchPhase;
import com.aditya.worldcup.live.dto.LiveMatchSnapshot;
import com.aditya.worldcup.live.service.LiveMatchStateService;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Validated
@Tag(name = "Live Match", description = "Live match state snapshot endpoint for late-join / reconnect")
public class LiveMatchController {

    private final LiveMatchStateService stateService;
    private final MatchRepository matchRepository;

    /**
     * Returns the current authoritative live snapshot for a match.
     *
     * <ul>
     *   <li>200 — match is currently live; response contains real-time snapshot.</li>
     *   <li>200 — match has finished; response contains the final snapshot (phase=FULL_TIME).</li>
     *   <li>200 — match exists but has not started yet; snapshot reflects PRE_MATCH state.</li>
     *   <li>404 — match ID does not exist in the database.</li>
     * </ul>
     *
     * <p>Clients should use {@code latestSequence} to identify where they are in the
     * STOMP event stream and discard any events with a lower sequence number.
     */
    @GetMapping("/{id}/live")
    @Operation(
            summary = "Get live match snapshot",
            description = "Returns the current authoritative state of a live match. "
                    + "Suitable for late-join and reconnect scenarios. "
                    + "Subscribe to /topic/matches/{id} on STOMP after consuming this snapshot."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Snapshot returned"),
            @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public ResponseEntity<LiveMatchSnapshot> getLiveMatchSnapshot(
            @Parameter(description = "Match id")
            @PathVariable @Positive Long id
    ) {
        // Verify the match exists in the DB — ensure canonical 404 if not
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Match not found with id: " + id));

        // Try state store first (covers LIVE and recently-finished matches)
        return stateService.getSnapshot(id)
                .map(snapshot -> ResponseEntity.ok(snapshot))
                .orElseGet(() -> {
                    // Match exists in DB but no live state — build a static snapshot
                    LiveMatchPhase phase = switch (match.getStatus()) {
                        case FINISHED -> LiveMatchPhase.FULL_TIME;
                        case LIVE -> LiveMatchPhase.FIRST_HALF; // should rarely reach here
                        default -> LiveMatchPhase.PRE_MATCH;
                    };

                    LiveMatchSnapshot staticSnapshot = new LiveMatchSnapshot(
                            match.getId(),
                            match.getTournament() != null ? match.getTournament().getId() : null,
                            match.getHomeTeam() != null ? match.getHomeTeam().getName() : null,
                            match.getAwayTeam() != null ? match.getAwayTeam().getName() : null,
                            match.getHomeScore() != null ? match.getHomeScore() : 0,
                            match.getAwayScore() != null ? match.getAwayScore() : 0,
                            match.getStatus() == MatchStatus.FINISHED ? 90 : 0,
                            phase,
                            0,           // latestSequence — no events yet
                            null,        // no latestEventType known
                            Instant.now()
                    );

                    return ResponseEntity.ok(staticSnapshot);
                });
    }
}
