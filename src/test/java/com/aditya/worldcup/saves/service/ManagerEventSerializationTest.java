package com.aditya.worldcup.saves.service;

import com.aditya.worldcup.managers.entity.ManagerEventStatus;
import com.aditya.worldcup.managers.entity.ManagerEventType;
import com.aditya.worldcup.saves.dto.SaveExportResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ManagerEventSerializationTest {

    @Test
    void testManagerEventSerialization() {
        LocalDateTime now = LocalDateTime.now();
        
        SaveExportResponse.ManagerEventSnapshot snap = new SaveExportResponse.ManagerEventSnapshot(
                1L, // eventId
                2L, // managerId
                ManagerEventType.FEDERATION_RESOURCE_GRANT, // type
                "Federation Grant", // title
                "Allocate to training.", // description
                "GRANT_100", // contextId
                ManagerEventStatus.RESOLVED, // status
                "ALLOCATE_TRAINING", // selectedDecision
                "Received 500 resources for Training.", // resolutionText
                3L, // relatedPlayerId
                4L, // relatedMatchId
                5L, // relatedTournamentId
                now.minusDays(1), // createdAt
                now.plusDays(1), // expiresAt
                now // resolvedAt
        );
        
        assertThat(snap.eventId()).isEqualTo(1L);
        assertThat(snap.managerId()).isEqualTo(2L);
        assertThat(snap.type()).isEqualTo(ManagerEventType.FEDERATION_RESOURCE_GRANT);
        assertThat(snap.title()).isEqualTo("Federation Grant");
        assertThat(snap.description()).isEqualTo("Allocate to training.");
        assertThat(snap.contextId()).isEqualTo("GRANT_100");
        assertThat(snap.status()).isEqualTo(ManagerEventStatus.RESOLVED);
        assertThat(snap.selectedDecision()).isEqualTo("ALLOCATE_TRAINING");
        assertThat(snap.resolutionText()).isEqualTo("Received 500 resources for Training.");
        assertThat(snap.relatedPlayerId()).isEqualTo(3L);
        assertThat(snap.relatedMatchId()).isEqualTo(4L);
        assertThat(snap.relatedTournamentId()).isEqualTo(5L);
        assertThat(snap.createdAt()).isEqualTo(now.minusDays(1));
        assertThat(snap.expiresAt()).isEqualTo(now.plusDays(1));
        assertThat(snap.resolvedAt()).isEqualTo(now);
    }
}
