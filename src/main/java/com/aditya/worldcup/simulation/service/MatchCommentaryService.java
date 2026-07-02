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

    private String createCommentary(MatchEventResponse event) {

        MatchEventType eventType =
                MatchEventType.valueOf(event.eventType());

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
}
