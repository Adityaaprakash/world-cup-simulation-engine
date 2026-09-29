package com.aditya.worldcup.live.publisher;

import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.simulation.dto.CommentaryResponse;
import com.aditya.worldcup.simulation.dto.MatchSimulationResponse;

import java.util.List;

public interface LiveEventPublisher {

    void publishMatchStarted(Long tournamentId, Long matchId);

    void publishKickOff(Long tournamentId, Long matchId);

    void publishMinuteTick(Long tournamentId, Long matchId, int minute,
                           List<MatchEventResponse> events,
                           List<CommentaryResponse> commentary,
                           String homeTeam, String awayTeam);

    void publishMatchEnded(Long tournamentId, Long matchId, int finalMinute, MatchSimulationResponse finalResult);

    void publishError(Long tournamentId, Long matchId, String message);
}
