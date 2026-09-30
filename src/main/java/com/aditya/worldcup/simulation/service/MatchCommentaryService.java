package com.aditya.worldcup.simulation.service;

import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.simulation.dto.CommentaryResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class MatchCommentaryService {

    public List<CommentaryResponse> generate(
            List<MatchEventResponse> events
    ) {

        return events.stream()
                .sorted(Comparator.comparing(
                        MatchEventResponse::minute
                ))
                .map(event -> new CommentaryResponse(
                        event.minute(),
                        createCommentary(event)
                ))
                .toList();
    }

    public String createCommentary(MatchEventResponse event) {

        MatchEventType eventType;
        try {
            eventType = MatchEventType.valueOf(event.eventType());
        } catch (Exception ex) {
            return null;
        }

        return switch (eventType) {
            case GOAL -> selectTemplate(
                    event,
                    "%s scores from close range.",
                    "%s finishes clinically.",
                    "A brilliant finish by %s.",
                    "Goal! %s finds the back of the net."
            );
            case ASSIST -> selectTemplate(
                    event,
                    "%s provides the assist.",
                    "Excellent build-up play from %s."
            );
            case YELLOW_CARD -> selectTemplate(
                    event,
                    "%s goes into the referee's notebook.",
                    "Yellow card shown to %s."
            );
            case RED_CARD -> selectTemplate(
                    event,
                    "Straight red card for %s.",
                    "%s is sent off."
            );
            case PENALTY -> penaltyCommentary(event);
            case OWN_GOAL -> selectTemplate(
                    event,
                    "An unfortunate own goal by %s.",
                    "The ball ends up in %s's own net."
            );
            case INJURY -> injuryCommentary(event);
            case SUBSTITUTION -> substitutionCommentary(event);
        };
    }

    private String penaltyCommentary(MatchEventResponse event) {

        String description =
                event.description() == null
                        ? ""
                        : event.description().toLowerCase();

        if (description.contains("converts")) {
            return "Penalty converted by " + event.player() + ".";
        }

        if (description.contains("misses")) {
            return "Penalty missed by " + event.player() + ".";
        }

        return selectTemplate(
                event,
                "Penalty awarded.",
                "The referee points to the spot."
        );
    }

    private String substitutionCommentary(MatchEventResponse event) {

        if (event.description() != null
                && event.description().contains(" replaces ")) {
            return event.description();
        }

        return selectTemplate(
                event,
                "Fresh legs introduced.",
                "A tactical substitution is made."
        );
    }

    private String injuryCommentary(MatchEventResponse event) {

        if (event.description() != null
                && !event.description().isEmpty()) {
            return event.description();
        }

        return selectTemplate(
                event,
                "%s sustains an injury.",
                "%s goes down injured."
        );
    }

    private String selectTemplate(
            MatchEventResponse event,
            String... templates
    ) {

        int index =
                Math.floorMod(
                        eventKey(event),
                        templates.length
                );

        return String.format(
                templates[index],
                event.player()
        );
    }

    private int eventKey(MatchEventResponse event) {

        int result = event.minute() == null ? 0 : event.minute();
        result = 31 * result
                + (event.player() == null ? 0 : event.player().hashCode());
        result = 31 * result
                + (event.eventType() == null
                ? 0
                : event.eventType().hashCode());

        return result;
    }

    public String generateLifecycleCommentary(com.aditya.worldcup.live.dto.LiveMatchEventType type, String homeTeam, String awayTeam, Integer homeScore, Integer awayScore) {
        if (type == null) return null;
        return switch (type) {
            case MATCH_STARTED -> "The teams are on the pitch. We are about to begin.";
            case KICK_OFF -> "KICK-OFF! We are underway in the first half.";
            case HALF_TIME -> "HALF-TIME: " + homeTeam + " " + homeScore + " - " + awayScore + " " + awayTeam + ".";
            case SECOND_HALF_STARTED -> "SECOND HALF: We are underway again.";
            case EXTRA_TIME_STARTED -> "EXTRA TIME: The match remains tied. We head into extra time.";
            case PENALTY_SHOOTOUT_STARTED -> "PENALTY SHOOTOUT: The outcome will be decided from the spot.";
            case FULL_TIME -> "FULL-TIME: The referee blows the final whistle! " + homeTeam + " " + homeScore + " - " + awayScore + " " + awayTeam + ".";
            default -> null;
        };
    }
}
