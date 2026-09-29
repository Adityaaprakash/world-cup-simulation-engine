import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { getMatchDetail } from '../api/matchApi'
import Card from '../components/common/Card'
import EmptyState from '../components/common/EmptyState'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import MatchCommentary from '../components/match/MatchCommentary'
import MatchDiscipline from '../components/match/MatchDiscipline'
import MatchHeader from '../components/match/MatchHeader'
import MatchStatistics from '../components/match/MatchStatistics'
import MatchTimeline from '../components/match/MatchTimeline'
import PlayerRatings from '../components/match/PlayerRatings'
import useLiveMatch from '../hooks/useLiveMatch'

export default function MatchCentre() {
  const { matchId } = useParams()
  const location = useLocation()
  const tournamentId = location.state?.tournamentId
  const [match, setMatch] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setIsLoading(true); setError('')
    try { const { data } = await getMatchDetail(matchId); setMatch(data) } catch (requestError) { setError(requestError.status === 404 ? 'Match not found.' : requestError.message || 'Unable to load match details.') } finally { setIsLoading(false) }
  }, [matchId])

  const {
    connectionStatus,
    events: liveEvents,
    commentary: liveCommentary,
    finalResult,
    hasConnectionError,
    error: liveError,
    liveScore,
    liveMinute,
    livePhase,
  } = useLiveMatch(matchId)

  useEffect(() => { load() }, [load])
  if (isLoading) return <Loading label="Loading Match Centre..." />
  if (!match) return <div className="space-y-4"><ErrorMessage message={error || 'Match details are unavailable.'} /><Link to="/tournaments" className="text-sm font-semibold text-emerald-400">Back to tournaments</Link></div>

  // Create authoritative display match representation
  const baseMatch = finalResult || match;
  const displayMatch = { ...baseMatch };
  
  if (liveScore) {
    displayMatch.homeScore = liveScore.home;
    displayMatch.awayScore = liveScore.away;
  }
  if (livePhase) {
    displayMatch.status = livePhase === 'FULL_TIME' ? 'FINISHED' : (livePhase === 'PRE_MATCH' ? 'SCHEDULED' : 'IN_PROGRESS');
    displayMatch.phase = livePhase;
  }
  if (liveMinute != null) {
    displayMatch.minute = liveMinute;
  }

  // Merge live events/commentary, deduplicating based on minute+description etc.
  const displayEvents = match.events ? [...match.events] : [];
  liveEvents.forEach(le => {
    if (!displayEvents.some(e => e.minute === le.minute && e.player === le.player && e.eventType === le.eventType && e.description === le.description)) {
      displayEvents.push(le);
    }
  });
  displayEvents.sort((a,b) => (a.minute ?? 0) - (b.minute ?? 0));

  const displayCommentary = match.commentary ? [...match.commentary] : [];
  liveCommentary.forEach(lc => {
    if (!displayCommentary.some(c => c.minute === lc.minute && c.commentary === lc.commentary)) {
      displayCommentary.push(lc);
    }
  });
  displayCommentary.sort((a,b) => (a.minute ?? 0) - (b.minute ?? 0));

  const renderConnectionStatus = () => {
    if (displayMatch.status === 'FINISHED') return null; // No status needed for finished matches
    
    if (connectionStatus === 'connected') {
      return (
        <div className="flex items-center gap-2 rounded-lg bg-emerald-950/40 px-4 py-2 text-emerald-400 text-sm font-bold border border-emerald-900/50">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
          </span>
          LIVE
        </div>
      );
    }
    
    if (connectionStatus === 'connecting' || connectionStatus === 'reconnecting') {
      return (
        <div className="flex items-center gap-2 rounded-lg bg-amber-950/40 px-4 py-2 text-amber-400 text-sm font-bold border border-amber-900/50">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-amber-500"></span>
          </span>
          CONNECTING...
        </div>
      );
    }

    if (connectionStatus === 'disconnected' || connectionStatus === 'error') {
      return (
        <div className="flex items-center gap-2 rounded-lg bg-rose-950/40 px-4 py-2 text-rose-400 text-sm font-bold border border-rose-900/50">
          <span className="h-2 w-2 rounded-full bg-rose-500"></span>
          DISCONNECTED
        </div>
      );
    }
    
    return null;
  };

  return <div className="space-y-7"><nav className="flex items-center justify-between flex-wrap gap-3 text-sm font-semibold text-emerald-400">
      <div className="flex gap-3">
        {tournamentId ? <><Link to={`/tournaments/${tournamentId}`}>Tournament overview</Link><Link to={`/tournaments/${tournamentId}/groups`}>Groups</Link><Link to={`/tournaments/${tournamentId}/knockout`}>Knockout</Link></> : <Link to="/tournaments">Tournaments</Link>}
      </div>
      <div>
        {renderConnectionStatus()}
      </div>
    </nav>
    
    {hasConnectionError && displayMatch.status !== 'FINISHED' && (
      <div className="rounded-lg bg-slate-900 px-4 py-2 text-slate-400 text-sm border border-slate-800">
        Live connection unavailable. Displaying last known static snapshot state.
        {liveError && <p className="text-xs text-rose-400 mt-1">{liveError}</p>}
      </div>
    )}
    <MatchHeader match={displayMatch} />
    
    <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(20rem,0.7fr)]"><Card><h2 className="text-xl font-bold text-white">Match timeline</h2><p className="mt-1 text-sm text-slate-400">Chronological match events.</p><div className="mt-5"><MatchTimeline events={displayEvents} /></div></Card><Card><h2 className="text-xl font-bold text-white">Match commentary</h2><p className="mt-1 text-sm text-slate-400">Real-time generated commentary.</p><div className="mt-5"><MatchCommentary commentary={displayCommentary} /></div></Card></div><div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(18rem,0.6fr)]"><MatchStatistics statistics={displayMatch.statistics} homeTeam={displayMatch.homeTeam} awayTeam={displayMatch.awayTeam} /><MatchDiscipline statistics={displayMatch.statistics} homeTeam={displayMatch.homeTeam} awayTeam={displayMatch.awayTeam} /></div><PlayerRatings ratings={displayMatch.playerRatings} homeTeam={displayMatch.homeTeam} awayTeam={displayMatch.awayTeam} manOfTheMatch={displayMatch.manOfTheMatch} />{displayMatch.manOfTheMatch ? <Card><p className="text-xs font-bold uppercase tracking-[0.14em] text-amber-300">Player of the Match</p><h2 className="mt-1 text-xl font-bold text-white">{displayMatch.manOfTheMatch.playerName}</h2><p className="mt-1 text-sm text-slate-400">{displayMatch.manOfTheMatch.team} · {displayMatch.manOfTheMatch.position} · {displayMatch.manOfTheMatch.rating?.toFixed(2)} rating</p></Card> : null}{!displayEvents.length && !displayCommentary.length && !displayMatch.statistics && !displayMatch.playerRatings?.length && <EmptyState title="Match detail is incomplete" description="The result is available, but the backend has not persisted supporting match detail yet." />}</div>
}
