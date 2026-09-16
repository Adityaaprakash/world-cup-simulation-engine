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
    error: liveError
  } = useLiveMatch(matchId)

  useEffect(() => { load() }, [load])
  if (isLoading) return <Loading label="Loading Match Centre..." />
  if (!match) return <div className="space-y-4"><ErrorMessage message={error || 'Match details are unavailable.'} /><Link to="/tournaments" className="text-sm font-semibold text-emerald-400">Back to tournaments</Link></div>

  const isLiveActive = connectionStatus === 'connected' || connectionStatus === 'connecting' || connectionStatus === 'reconnecting';

  const displayMatch = finalResult || match;

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

  return <div className="space-y-7"><nav className="flex flex-wrap gap-3 text-sm font-semibold text-emerald-400">{tournamentId ? <><Link to={`/tournaments/${tournamentId}`}>Tournament overview</Link><Link to={`/tournaments/${tournamentId}/groups`}>Groups</Link><Link to={`/tournaments/${tournamentId}/knockout`}>Knockout</Link></> : <Link to="/tournaments">Tournaments</Link>}</nav>
    {isLiveActive && displayMatch.status !== 'FINISHED' && (
      <div className="flex items-center gap-2 rounded-lg bg-emerald-950/40 px-4 py-2 text-emerald-400 text-sm font-bold border border-emerald-900/50">
        <span className="relative flex h-2 w-2">
          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
          <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
        </span>
        LIVE updates connected
      </div>
    )}
    {hasConnectionError && (
      <div className="rounded-lg bg-slate-900 px-4 py-2 text-slate-400 text-sm border border-slate-800">
        Live connection unavailable. Displaying static data.
        {liveError && <p className="text-xs text-rose-400 mt-1">{liveError}</p>}
      </div>
    )}
    <MatchHeader match={displayMatch} /><div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(20rem,0.7fr)]"><Card><h2 className="text-xl font-bold text-white">Match timeline</h2><p className="mt-1 text-sm text-slate-400">Persisted match events in chronological order.</p><div className="mt-5"><MatchTimeline events={displayEvents} /></div></Card><Card><h2 className="text-xl font-bold text-white">Match commentary</h2><p className="mt-1 text-sm text-slate-400">Server-generated commentary from persisted events.</p><div className="mt-5"><MatchCommentary commentary={displayCommentary} /></div></Card></div><div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(18rem,0.6fr)]"><MatchStatistics statistics={displayMatch.statistics} homeTeam={displayMatch.homeTeam} awayTeam={displayMatch.awayTeam} /><MatchDiscipline statistics={displayMatch.statistics} homeTeam={displayMatch.homeTeam} awayTeam={displayMatch.awayTeam} /></div><PlayerRatings ratings={displayMatch.playerRatings} homeTeam={displayMatch.homeTeam} awayTeam={displayMatch.awayTeam} manOfTheMatch={displayMatch.manOfTheMatch} />{displayMatch.manOfTheMatch ? <Card><p className="text-xs font-bold uppercase tracking-[0.14em] text-amber-300">Player of the Match</p><h2 className="mt-1 text-xl font-bold text-white">{displayMatch.manOfTheMatch.playerName}</h2><p className="mt-1 text-sm text-slate-400">{displayMatch.manOfTheMatch.team} · {displayMatch.manOfTheMatch.position} · {displayMatch.manOfTheMatch.rating?.toFixed(2)} rating</p></Card> : null}{!displayEvents.length && !displayCommentary.length && !displayMatch.statistics && !displayMatch.playerRatings?.length && <EmptyState title="Match detail is incomplete" description="The result is available, but the backend has not persisted supporting match detail yet." />}</div>
}
