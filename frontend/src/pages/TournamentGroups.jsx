import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getTournament, getTournamentGroups, getTournamentMatches, getTournamentStandings } from '../api/tournamentApi'
import Card from '../components/common/Card'
import EmptyState from '../components/common/EmptyState'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import StatusBadge from '../components/tournament/StatusBadge'

const value = (item) => item ?? '—'

function StandingsTable({ standings }) {
  if (!standings?.length) return <EmptyState title="No standings available" description="Persisted standings will appear after groups have been generated." />
  return <div className="overflow-x-auto"><table className="min-w-[42rem] w-full text-left text-sm"><thead className="border-b border-slate-800 text-xs uppercase tracking-wide text-slate-500"><tr><th className="px-2 py-3">#</th><th className="px-2 py-3">Team</th><th className="px-2 py-3 text-center">W</th><th className="px-2 py-3 text-center">D</th><th className="px-2 py-3 text-center">L</th><th className="px-2 py-3 text-center">GF</th><th className="px-2 py-3 text-center">GA</th><th className="px-2 py-3 text-center">GD</th><th className="px-2 py-3 text-center">Pts</th></tr></thead><tbody>{standings.map((standing, index) => <tr key={standing.team} className="border-b border-slate-800/80 last:border-0"><td className="px-2 py-3 font-bold text-slate-400">{index + 1}</td><td className="px-2 py-3 font-semibold text-slate-100">{standing.team}</td><td className="px-2 py-3 text-center text-slate-300">{value(standing.wins)}</td><td className="px-2 py-3 text-center text-slate-300">{value(standing.draws)}</td><td className="px-2 py-3 text-center text-slate-300">{value(standing.losses)}</td><td className="px-2 py-3 text-center text-slate-300">{value(standing.goalsFor)}</td><td className="px-2 py-3 text-center text-slate-300">{value(standing.goalsAgainst)}</td><td className="px-2 py-3 text-center text-slate-300">{value(standing.goalDifference)}</td><td className="px-2 py-3 text-center font-bold text-emerald-300">{value(standing.points)}</td></tr>)}</tbody></table></div>
}

function Fixtures({ matches }) {
  return <section id="fixtures"><div className="mb-4"><h2 className="text-xl font-bold text-white">Group fixtures</h2><p className="mt-1 text-sm text-slate-400">Scores and match status from persisted group-stage fixtures.</p></div>{matches.length ? <div className="grid gap-3 lg:grid-cols-2">{matches.map((match) => <Card key={match.id} className="p-4"><div className="flex items-center justify-between gap-3"><span className="text-xs font-semibold uppercase tracking-wide text-slate-500">{match.group || 'Group stage'}</span><StatusBadge status={match.status} /></div><div className="mt-4 grid grid-cols-[1fr_auto_1fr] items-center gap-3 text-center"><p className="text-right font-semibold text-slate-100">{match.homeTeam}</p><span className="rounded-lg bg-slate-950 px-3 py-2 font-bold text-white">{match.status === 'FINISHED' ? `${value(match.homeScore)} – ${value(match.awayScore)}` : 'vs'}</span><p className="text-left font-semibold text-slate-100">{match.awayTeam}</p></div>{match.status === 'FINISHED' && <Link to={`/matches/${match.id}`} className="mt-4 block text-center text-sm font-semibold text-emerald-400 hover:text-emerald-300">Open Match Centre →</Link>}</Card>)}</div> : <EmptyState title="No group fixtures" description="Group-stage fixtures will appear once they have been generated." />}</section>
}

export default function TournamentGroups() {
  const { tournamentId } = useParams()
  const [data, setData] = useState({ tournament: null, groups: [], standings: [], matches: [] })
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setIsLoading(true); setError('')
    const results = await Promise.allSettled([getTournament(tournamentId), getTournamentGroups(tournamentId), getTournamentStandings(tournamentId), getTournamentMatches(tournamentId)])
    const [tournament, groups, standings, matches] = results
    setData({ tournament: tournament.status === 'fulfilled' ? tournament.value.data : null, groups: groups.status === 'fulfilled' ? groups.value.data : [], standings: standings.status === 'fulfilled' ? standings.value.data : [], matches: matches.status === 'fulfilled' ? matches.value.data : [] })
    const failure = results.find((result) => result.status === 'rejected')
    if (failure) setError(failure.reason?.message || 'Some group-stage information could not be loaded.')
    setIsLoading(false)
  }, [tournamentId])

  useEffect(() => { load() }, [load])
  const groupMatches = useMemo(() => data.matches.filter((match) => match.round === 'GROUP_STAGE'), [data.matches])
  const standingsByGroup = useMemo(() => new Map(data.standings.map((group) => [group.group, group.standings])), [data.standings])
  if (isLoading) return <Loading label="Loading groups and standings..." />
  if (!data.tournament) return <ErrorMessage message={error || 'Tournament not found.'} />

  return <div className="space-y-7"><Link to={`/tournaments/${tournamentId}`} className="text-sm font-semibold text-emerald-400 hover:text-emerald-300">← {data.tournament.name}</Link><div><p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-400">Group Stage</p><h1 className="mt-2 text-3xl font-bold text-white">Groups & standings</h1><p className="mt-2 text-slate-400">Persisted group tables and fixtures for {data.tournament.name}.</p></div><ErrorMessage message={error} />{data.groups.length ? <div className="grid gap-6 xl:grid-cols-2">{data.groups.map((group) => <Card key={group.id} className="overflow-hidden"><h2 className="mb-4 text-xl font-bold text-white">{group.name}</h2><StandingsTable standings={standingsByGroup.get(group.name)} /></Card>)}</div> : <EmptyState title="Groups have not been generated" description="This tournament currently has no persisted group-stage draw to display." />}<Fixtures matches={groupMatches} /></div>
}
