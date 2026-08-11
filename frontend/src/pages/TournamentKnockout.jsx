import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getTournament, getTournamentMatches } from '../api/tournamentApi'
import { getTournamentStatistics } from '../api/statisticsApi'
import Card from '../components/common/Card'
import EmptyState from '../components/common/EmptyState'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import KnockoutBracket from '../components/tournament/KnockoutBracket'

export default function TournamentKnockout() {
  const { tournamentId } = useParams()
  const [data, setData] = useState({ tournament: null, matches: [], statistics: null })
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setIsLoading(true); setError('')
    try {
      const { data: tournament } = await getTournament(tournamentId)
      const [matchesResult, statisticsResult] = await Promise.allSettled([getTournamentMatches(tournamentId), getTournamentStatistics(tournament.name, tournament.year)])
      const statistics = statisticsResult.status === 'fulfilled' ? statisticsResult.value.data.content?.find((item) => item.tournamentId === tournament.id) || null : null
      setData({ tournament, matches: matchesResult.status === 'fulfilled' ? matchesResult.value.data : [], statistics })
      const failure = [matchesResult, statisticsResult].find((result) => result.status === 'rejected')
      if (failure) setError(failure.reason?.message || 'Some knockout information could not be loaded.')
    } catch (requestError) { setError(requestError.message || 'Unable to load knockout fixtures.') } finally { setIsLoading(false) }
  }, [tournamentId])

  useEffect(() => { load() }, [load])
  if (isLoading) return <Loading label="Loading knockout bracket..." />
  if (!data.tournament) return <ErrorMessage message={error || 'Tournament not found.'} />
  const knockoutMatches = data.matches.filter((match) => match.round && match.round !== 'GROUP_STAGE')
  const remaining = knockoutMatches.filter((match) => match.status !== 'FINISHED').length
  const champion = data.tournament.status === 'COMPLETED' ? data.statistics?.champion : null

  return <div className="space-y-7"><Link to={`/tournaments/${tournamentId}`} className="text-sm font-semibold text-emerald-400 hover:text-emerald-300">Back to tournament overview</Link><div><p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-400">Knockout Centre</p><h1 className="mt-2 text-3xl font-bold text-white">{data.tournament.name} bracket</h1><p className="mt-2 text-slate-400">Persisted knockout fixtures and match results.</p></div><ErrorMessage message={error} />{champion ? <Card className="border-amber-400/40 bg-gradient-to-r from-amber-400/15 to-slate-900"><p className="text-sm font-bold uppercase tracking-[0.18em] text-amber-300">Tournament Champion</p><h2 className="mt-2 text-3xl font-bold text-white">{champion}</h2><p className="mt-2 text-slate-300">Champion confirmed by tournament statistics.</p></Card> : <Card><p className="font-semibold text-slate-100">{remaining ? `${remaining} knockout matches remaining` : 'Knockout progression is awaiting persisted results.'}</p><p className="mt-1 text-sm text-slate-400">A champion is shown only when the completed tournament statistics provide one.</p></Card>}{knockoutMatches.length ? <KnockoutBracket matches={knockoutMatches} tournamentId={tournamentId} /> : <EmptyState title="No knockout fixtures available" description="The backend has not persisted knockout matches for this tournament." />}</div>
}
