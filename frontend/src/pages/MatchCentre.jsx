import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { getMatchDetail } from '../api/matchApi'
import Card from '../components/common/Card'
import EmptyState from '../components/common/EmptyState'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import MatchCommentary from '../components/match/MatchCommentary'
import MatchHeader from '../components/match/MatchHeader'
import MatchTimeline from '../components/match/MatchTimeline'

const statisticRows = [['Possession', 'possession', '%'], ['Shots', 'shots', ''], ['Shots on target', 'shotsOnTarget', ''], ['Corners', 'corners', ''], ['Fouls', 'fouls', ''], ['Expected goals', 'expectedGoals', '']]

function MatchStatistics({ statistics, homeTeam, awayTeam }) {
  if (!statistics?.homeTeam || !statistics?.awayTeam) return null
  return <Card><h2 className="text-xl font-bold text-white">Match statistics</h2><div className="mt-4 overflow-x-auto"><table className="min-w-[32rem] w-full text-sm"><thead className="border-b border-slate-800 text-slate-400"><tr><th className="pb-3 text-left">{homeTeam}</th><th className="pb-3 text-center">Stat</th><th className="pb-3 text-right">{awayTeam}</th></tr></thead><tbody>{statisticRows.map(([label, key, suffix]) => <tr key={key} className="border-b border-slate-800/80 last:border-0"><td className="py-3 font-semibold text-slate-100">{statistics.homeTeam[key] == null ? '-' : `${statistics.homeTeam[key]}${suffix}`}</td><td className="py-3 text-center text-slate-500">{label}</td><td className="py-3 text-right font-semibold text-slate-100">{statistics.awayTeam[key] == null ? '-' : `${statistics.awayTeam[key]}${suffix}`}</td></tr>)}</tbody></table></div></Card>
}

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

  useEffect(() => { load() }, [load])
  if (isLoading) return <Loading label="Loading Match Centre..." />
  if (!match) return <div className="space-y-4"><ErrorMessage message={error || 'Match details are unavailable.'} /><Link to="/tournaments" className="text-sm font-semibold text-emerald-400">Back to tournaments</Link></div>

  return <div className="space-y-7"><nav className="flex flex-wrap gap-3 text-sm font-semibold text-emerald-400">{tournamentId ? <><Link to={`/tournaments/${tournamentId}`}>Tournament overview</Link><Link to={`/tournaments/${tournamentId}/groups`}>Groups</Link><Link to={`/tournaments/${tournamentId}/knockout`}>Knockout</Link></> : <Link to="/tournaments">Tournaments</Link>}</nav><MatchHeader match={match} /><div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(20rem,0.7fr)]"><Card><h2 className="text-xl font-bold text-white">Match timeline</h2><p className="mt-1 text-sm text-slate-400">Persisted match events in chronological order.</p><div className="mt-5"><MatchTimeline events={match.events} /></div></Card><Card><h2 className="text-xl font-bold text-white">Match commentary</h2><p className="mt-1 text-sm text-slate-400">Server-generated commentary from persisted events.</p><div className="mt-5"><MatchCommentary commentary={match.commentary} /></div></Card></div><MatchStatistics statistics={match.statistics} homeTeam={match.homeTeam} awayTeam={match.awayTeam} />{match.manOfTheMatch ? <Card><p className="text-xs font-bold uppercase tracking-[0.14em] text-amber-300">Player of the Match</p><h2 className="mt-1 text-xl font-bold text-white">{match.manOfTheMatch.playerName}</h2><p className="mt-1 text-sm text-slate-400">{match.manOfTheMatch.team} · {match.manOfTheMatch.position} · {match.manOfTheMatch.rating?.toFixed(2)} rating</p></Card> : null}{!match.events?.length && !match.commentary?.length && !match.statistics && <EmptyState title="Match detail is incomplete" description="The result is available, but the backend has not persisted supporting match detail yet." />}</div>
}
