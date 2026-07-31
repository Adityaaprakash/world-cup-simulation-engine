package com.aditya.worldcup.matchevents.repository;

import com.aditya.worldcup.matchevents.entity.MatchEvent;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.players.entity.Player;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {
    List<MatchEvent> findByMatchId(Long matchId);
    List<MatchEvent> findByMatchIdIn(List<Long> matchIds);
    void deleteByMatchIdIn(List<Long> matchIds);

    @Query("SELECT e.player, COUNT(e) FROM MatchEvent e WHERE e.eventType = :eventType AND e.player IS NOT NULL GROUP BY e.player ORDER BY COUNT(e) DESC")
    List<Object[]> findTopPlayersByEventType(@Param("eventType") MatchEventType eventType, Pageable pageable);

    @Query("SELECT e.player, COUNT(e) FROM MatchEvent e WHERE (e.eventType = com.aditya.worldcup.matchevents.entity.MatchEventType.GOAL OR e.eventType = com.aditya.worldcup.matchevents.entity.MatchEventType.PENALTY) AND e.player IS NOT NULL GROUP BY e.player ORDER BY COUNT(e) DESC")
    List<Object[]> findTopScoringPlayers(Pageable pageable);
}
