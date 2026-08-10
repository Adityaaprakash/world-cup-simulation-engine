import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getTournament, getTournamentAwards, getTournamentSummary, getTournamentTeamAwards } from '../api/tournamentApi'
import { getTournamentStatistics } from '../api/statisticsApi'
import Card from '../components/common/Card'
import EmptyState from '../components/common/EmptyState'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import TournamentAwards from '../components/tournament/TournamentAwards'
import TournamentIntelligence from '../components/tournament/TournamentIntelligence'

function Metric({ label, value, accent = false }) { return <div className="rounded-lg border border-slate-800 bg-slate-950/50 p-3"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p><p className={`mt-1 text-2xl font-bold ${accent ? 'text-emerald-400' : 'text-slate-100'}`}>{value ?? '—'}</p></div> }

export default function TournamentSummary() {
  const { tournamentId } = useParams()
  const [data, setData] = useState({ tournament: null, summary: null, awards: null, teamAwards: null, statistics: null })
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setIsLoading(true); setError('')
    try {
      const { data: tournament } = await getTournament(tournamentId)
      const results = await Promise.allSettled([getTournamentSummary(tournamentId), getTournamentAwards(tournamentId), getTournamentTeamAwards(tournamentId), getTournamentStatistics(tournament.name, tournament.year)])
      const [summary, awards, teamAwards, statisticsPage] = results
      setData({
        tournament,
        summary: summary.status === 'fulfilled' ? summary.value.data : null,
        awards: awards.status === 'fulfilled' ? awards.value.data : null,
        teamAwards: teamAwards.status === 'fulfilled' ? teamAwards.value.data : null,
        statistics: statisticsPage.status === 'fulfilled' ? statisticsPage.value.data.content?.find((item) => item.tournamentId === tournament.id) || null : null,
      })
      const failure = results.find((result) => result.status === 'rejected')
      if (failure) setError(failure.reason?.message || 'Some tournament reports are not available yet.')
    } catch (requestError) { setError(requestError.message || 'Unable to load tournament reports.') } finally { setIsLoading(false) }
  }, [tournamentId])

  useEffect(() => { load() }, [load])
  if (isLoading) return <Loading label="Loading tournament reports..." />
  if (!data.tournament) return <ErrorMessage message={error || 'Tournament not found.'} />
  const { summary, awards, teamAwards, statistics } = data

  return <div className="space-y-7"><Link to={`/tournaments/${tournamentId}`} className="text-sm font-semibold text-emerald-400 hover:text-emerald-300">Back to tournament overview</Link><div><p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-400">Tournament Reports</p><h1 className="mt-2 text-3xl font-bold text-white">{data.tournament.name} summary</h1><p className="mt-2 text-slate-400">Backend-generated tournament intelligence, awards, and statistics.</p></div><ErrorMessage message={error} />{statistics?.champion ? <Card className="border-amber-400/40 bg-gradient-to-r from-amber-400/15 to-slate-900"><p className="text-sm font-bold uppercase tracking-[0.18em] text-amber-300">Champion</p><h2 className="mt-2 text-3xl font-bold text-white">{statistics.champion}</h2></Card> : null}<section id="statistics"><h2 className="mb-3 text-xl font-bold text-white">Tournament statistics</h2>{statistics ? <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-6"><Metric label="Goals" value={statistics.totalGoals} accent /><Metric label="Average goals" value={statistics.averageGoals?.toFixed(2)} /><Metric label="Matches" value={statistics.completedMatches} /><Metric label="Clean sheets" value={statistics.cleanSheets} /><Metric label="Yellow cards" value={statistics.yellowCards} /><Metric label="Red cards" value={statistics.redCards} /></div> : <EmptyState title="Tournament statistics unavailable" description="The statistics service has not returned a report for this tournament." />}</section><section><h2 className="mb-3 text-xl font-bold text-white">Tournament summary</h2>{summary ? <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4"><Metric label="Total goals" value={summary.totalGoals} accent /><Metric label="Completed matches" value={summary.completedMatches} /><Metric label="Remaining fixtures" value={summary.remainingFixtures} /><Metric label="Current stage" value={summary.currentStage?.replaceAll('_', ' ')} /></div> : <EmptyState title="Summary unavailable" description="The backend summary will appear when tournament information is available." />}</section><section><h2 className="mb-3 text-xl font-bold text-white">Tournament intelligence</h2><TournamentIntelligence summary={summary} /></section><section id="awards"><h2 className="mb-3 text-xl font-bold text-white">Tournament awards</h2><TournamentAwards awards={awards} teamAwards={teamAwards || summary?.teamAwards} /></section>{summary?.narratives?.length ? <section><h2 className="mb-3 text-xl font-bold text-white">Match narratives</h2><div className="grid gap-3 md:grid-cols-2">{summary.narratives.map((narrative) => <article key={narrative.matchId} className="rounded-lg border border-slate-800 bg-slate-900/80 p-4"><p className="font-semibold text-slate-100">{narrative.headline}</p><p className="mt-2 text-sm leading-6 text-slate-400">{narrative.narrative}</p></article>)}</div></section> : null}</div>
}
