package com.aditya.worldcup.matchevents.repository;

import com.aditya.worldcup.matchevents.entity.MatchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {
    List<MatchEvent> findByMatchId(Long matchId);
}
