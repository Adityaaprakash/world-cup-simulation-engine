import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getTournament, getTournamentGroups, getTournamentMatches, getTournamentTeams } from '../api/tournamentApi'
import Card from '../components/common/Card'
import EmptyState from '../components/common/EmptyState'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import StatusBadge from '../components/tournament/StatusBadge'
import { formatLabel } from '../utils/football'

const STAGE_ORDER = ['GROUP_STAGE', 'ROUND_OF_16', 'QUARTER_FINALS', 'SEMI_FINALS', 'FINAL']

function Metric({ label, value, accent = false }) {
  return <div className="rounded-lg border border-slate-800 bg-slate-950/50 p-3"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p><p className={`mt-1 text-2xl font-bold ${accent ? 'text-emerald-400' : 'text-slate-100'}`}>{value}</p></div>
}

export default function TournamentDashboard() {
  const { tournamentId } = useParams()
  const [data, setData] = useState({ tournament: null, teams: [], groups: [], matches: [] })
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setIsLoading(true); setError('')
    const results = await Promise.allSettled([getTournament(tournamentId), getTournamentTeams(tournamentId), getTournamentGroups(tournamentId), getTournamentMatches(tournamentId)])
    const [tournament, teams, groups, matches] = results
    setData({ tournament: tournament.status === 'fulfilled' ? tournament.value.data : null, teams: teams.status === 'fulfilled' ? teams.value.data : [], groups: groups.status === 'fulfilled' ? groups.value.data : [], matches: matches.status === 'fulfilled' ? matches.value.data : [] })
    const failure = results.find((result) => result.status === 'rejected')
    if (failure) setError(failure.reason?.message || 'Some tournament information could not be loaded.')
    setIsLoading(false)
  }, [tournamentId])

  useEffect(() => { load() }, [load])
  const { tournament, teams, groups, matches } = data
  const completedMatches = matches.filter((match) => match.status === 'FINISHED').length
  const stages = useMemo(() => STAGE_ORDER.filter((stage) => matches.some((match) => match.round === stage)), [matches])
  const currentFixtureStage = stages.find((stage) => matches.some((match) => match.round === stage && match.status !== 'FINISHED')) || stages.at(-1)
  if (isLoading) return <Loading label="Loading tournament dashboard..." />
  if (!tournament) return <ErrorMessage message={error || 'Tournament not found.'} />

  const navLinks = [
    ['Overview', `/tournaments/${tournamentId}`],
    ['Groups & standings', `/tournaments/${tournamentId}/groups`],
    ['Group fixtures', `/tournaments/${tournamentId}/groups#fixtures`],
    ['Knockout bracket', `/tournaments/${tournamentId}/knockout`],
    ['Summary', `/tournaments/${tournamentId}/summary`],
    ['Awards', `/tournaments/${tournamentId}/summary#awards`],
    ['Statistics', `/tournaments/${tournamentId}/summary#statistics`],
  ]

  return <div className="space-y-7"><Link to="/tournaments" className="text-sm font-semibold text-emerald-400 hover:text-emerald-300">Back to all tournaments</Link><Card><div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-end"><div><p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-400">Tournament Dashboard</p><h1 className="mt-2 text-3xl font-bold text-white sm:text-4xl">{tournament.name}</h1><p className="mt-2 text-slate-400">{tournament.year} · Host: {tournament.hostCountry}</p>{currentFixtureStage && <p className="mt-2 text-sm font-medium text-emerald-300">Current fixture stage: {formatLabel(currentFixtureStage)}</p>}</div><StatusBadge status={tournament.status} /></div></Card><ErrorMessage message={error} /><section><h2 className="mb-3 text-lg font-bold text-white">Competition overview</h2><div className="grid grid-cols-2 gap-3 sm:grid-cols-4"><Metric label="Teams" value={teams.length} /><Metric label="Matches complete" value={completedMatches} accent /><Metric label="Matches remaining" value={Math.max(matches.length - completedMatches, 0)} /><Metric label="Groups" value={groups.length} /></div></section><Card><h2 className="text-lg font-bold text-white">Tournament progress</h2><p className="mt-1 text-sm text-slate-400">Stages appear only when they are present in persisted tournament fixtures.</p>{stages.length ? <ol className="mt-5 flex flex-wrap gap-2">{stages.map((stage, index) => <li key={stage} className="flex items-center gap-2"><span className={`rounded-full border px-3 py-2 text-sm font-semibold ${matches.some((match) => match.round === stage && match.status === 'FINISHED') ? 'border-emerald-400/40 bg-emerald-400/10 text-emerald-200' : 'border-slate-700 bg-slate-950 text-slate-300'}`}>{formatLabel(stage)}</span>{index < stages.length - 1 && <span className="text-slate-600">→</span>}</li>)}</ol> : <EmptyState title="No fixture stages available" description="Tournament progress will appear once fixtures have been generated." />}</Card><div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(18rem,0.75fr)]"><Card><div className="mb-4 flex items-center justify-between gap-4"><div><h2 className="text-lg font-bold text-white">Participating teams</h2><p className="mt-1 text-sm text-slate-400">Registered teams and their persisted overall ratings.</p></div><span className="text-sm font-semibold text-slate-400">{teams.length} teams</span></div>{teams.length ? <div className="grid gap-3 sm:grid-cols-2">{teams.map((team) => <div key={team.tournamentTeamId} className="flex items-center justify-between rounded-lg border border-slate-800 bg-slate-950/50 p-3"><div><p className="font-semibold text-slate-100">{team.teamName}</p>{team.groupId && <p className="mt-1 text-xs text-slate-500">Assigned to a group</p>}</div><span className="rounded-lg bg-slate-800 px-2 py-1 text-sm font-bold text-slate-100">{team.overallRating} OVR</span></div>)}</div> : <EmptyState title="No teams registered" description="Registered national teams will appear here." />}</Card><Card><h2 className="text-lg font-bold text-white">Tournament navigation</h2><p className="mt-1 text-sm text-slate-400">Overview, groups, knockout reports, awards, and statistics.</p><div className="mt-4 grid gap-2">{navLinks.map(([label, target]) => <Link key={label} to={target} className="rounded-lg border border-slate-700 px-4 py-3 font-semibold text-slate-100 transition hover:border-emerald-400 hover:bg-emerald-400/10">{label} →</Link>)}</div></Card></div></div>
}
