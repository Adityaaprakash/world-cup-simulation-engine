import { useCallback, useEffect, useRef, useState } from 'react';
import { getTournamentLiveState } from '../api/tournamentApi';

const POLL_INTERVAL_MS = 5000; // Poll every 5 seconds — suitable for a tournament scoreboard

/**
 * Polls GET /api/tournaments/{tournamentId}/live at a fixed interval.
 *
 * Returns a map of matchId → MatchLiveEntry so consumers can look up live state
 * for any specific match by ID.
 *
 * Design principles:
 *   - Polling only — no separate WebSocket for tournament level.
 *   - Stops polling when the component unmounts.
 *   - One failed poll does not crash the hook or subsequent polls.
 *   - Sequences remain match-scoped; no global tournament sequence is created here.
 *   - Returns matchById Map for O(1) per-match lookups in rendering.
 */
export default function useTournamentLive(tournamentId) {
  const [liveState, setLiveState] = useState(null);
  const [isLiveAvailable, setIsLiveAvailable] = useState(false);
  const [liveError, setLiveError] = useState(null);
  const intervalRef = useRef(null);

  const fetchLive = useCallback(async () => {
    if (!tournamentId) return;
    try {
      const { data } = await getTournamentLiveState(tournamentId);
      setLiveState(data);
      setIsLiveAvailable(true);
      setLiveError(null);
    } catch (err) {
      // On failure keep existing state; just expose the error
      setLiveError(err?.message || 'Failed to fetch tournament live state');
    }
  }, [tournamentId]);

  useEffect(() => {
    if (!tournamentId) return;

    // Initial fetch immediately
    fetchLive();

    // Periodic polling
    intervalRef.current = setInterval(fetchLive, POLL_INTERVAL_MS);

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [tournamentId, fetchLive]);

  /**
   * Map from matchId (number) → MatchLiveEntry.
   * Built on each render from liveState for O(1) per-match lookups.
   */
  const matchById = buildMatchById(liveState);

  const activeMatches = liveState?.activeMatches ?? [];
  const recentlyCompletedMatches = liveState?.recentlyCompletedMatches ?? [];
  const upcomingMatches = liveState?.upcomingMatches ?? [];
  const hasActiveMatches = activeMatches.length > 0;

  return {
    liveState,
    matchById,
    activeMatches,
    recentlyCompletedMatches,
    upcomingMatches,
    hasActiveMatches,
    isLiveAvailable,
    liveError,
    refresh: fetchLive,
  };
}

function buildMatchById(liveState) {
  const map = new Map();
  if (!liveState) return map;
  for (const entry of liveState.activeMatches ?? []) map.set(entry.matchId, entry);
  for (const entry of liveState.recentlyCompletedMatches ?? []) map.set(entry.matchId, entry);
  for (const entry of liveState.upcomingMatches ?? []) map.set(entry.matchId, entry);
  return map;
}
