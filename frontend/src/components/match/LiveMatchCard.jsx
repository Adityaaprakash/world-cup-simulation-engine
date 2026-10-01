import { Link } from 'react-router-dom'
import Card from '../common/Card'
import StatusBadge from '../tournament/StatusBadge'

/**
 * Displays a single match fixture enriched with real-time live state.
 *
 * Props:
 *   match      — the persisted match record from the REST API
 *   liveEntry  — optional MatchLiveEntry from useTournamentLive hook
 *   tournamentId — for "Open Match Centre" link
 */
export default function LiveMatchCard({ match, liveEntry, tournamentId }) {
  const isLive = liveEntry &&
    liveEntry.phase &&
    liveEntry.phase !== 'PRE_MATCH' &&
    liveEntry.phase !== 'FULL_TIME';

  const isCompleted = match.status === 'FINISHED' ||
    (liveEntry && liveEntry.phase === 'FULL_TIME');

  // Live score overrides DB score when a live session is active
  const homeScore = liveEntry ? liveEntry.homeScore : match.homeScore;
  const awayScore = liveEntry ? liveEntry.awayScore : match.awayScore;

  const scoreLabel = (isLive || isCompleted)
    ? `${homeScore ?? 0} – ${awayScore ?? 0}`
    : 'vs';

  const minuteLabel = buildMinuteLabel(liveEntry);

  return (
    <Card className="p-0 overflow-hidden">
      <div className="p-4">
        {/* Header row */}
        <div className="flex items-center justify-between gap-3 mb-4">
          <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">
            {match.group || formatRound(match.round)}
          </span>
          <div className="flex items-center gap-2">
            {isLive && (
              <span className="flex items-center gap-1.5 rounded-full border border-rose-400/40 bg-rose-400/10 px-2.5 py-1 text-xs font-bold uppercase tracking-wide text-rose-300">
                <span className="relative flex h-1.5 w-1.5">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-rose-400 opacity-75" />
                  <span className="relative inline-flex rounded-full h-1.5 w-1.5 bg-rose-500" />
                </span>
                LIVE
              </span>
            )}
            {!isLive && <StatusBadge status={match.status} />}
          </div>
        </div>

        {/* Score row */}
        <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-3 text-center">
          <p className="text-right font-semibold text-slate-100">
            {liveEntry?.homeTeam ?? match.homeTeam}
          </p>
          <span className={`rounded-lg px-3 py-2 font-black text-lg transition-colors duration-300
            ${isLive ? 'bg-rose-950/60 border border-rose-700/40 text-white' :
              isCompleted ? 'bg-slate-950 border border-slate-700 text-white' :
              'bg-slate-950 border border-slate-800 text-slate-400'}`}
          >
            {scoreLabel}
          </span>
          <p className="text-left font-semibold text-slate-100">
            {liveEntry?.awayTeam ?? match.awayTeam}
          </p>
        </div>

        {/* Live status row */}
        {isLive && (
          <div className="mt-3 flex flex-col items-center gap-1">
            {minuteLabel && (
              <span className="text-sm font-bold text-rose-300">{minuteLabel}</span>
            )}
            {liveEntry.latestCommentary && (
              <p className="text-xs text-slate-400 text-center line-clamp-1">
                {liveEntry.latestCommentary}
              </p>
            )}
          </div>
        )}

        {/* FULL_TIME status row */}
        {isCompleted && !isLive && (
          <p className="mt-3 text-center text-xs font-semibold uppercase tracking-wide text-slate-500">
            Full time
          </p>
        )}
      </div>

      {/* Match Centre link */}
      {(isCompleted || isLive) && (
        <div className="border-t border-slate-800/60 px-4 py-2.5">
          <Link
            to={`/matches/${match.id}`}
            state={{ tournamentId }}
            className="block text-center text-xs font-semibold text-emerald-400 hover:text-emerald-300 transition-colors"
          >
            {isLive ? 'Watch Live →' : 'Match Centre →'}
          </Link>
        </div>
      )}
    </Card>
  );
}

function buildMinuteLabel(entry) {
  if (!entry) return null;
  if (entry.minute == null) return null;
  if (entry.phase === 'HALF_TIME') return 'HT';
  return `${entry.minute}'`;
}

function formatRound(round) {
  if (!round) return 'Group Stage';
  return round.replace(/_/g, ' ');
}
