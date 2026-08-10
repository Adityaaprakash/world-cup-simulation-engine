import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getTournaments } from '../api/tournamentApi'
import Card from '../components/common/Card'
import EmptyState from '../components/common/EmptyState'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import Button from '../components/common/Button'
import StatusBadge from '../components/tournament/StatusBadge'

export default function TournamentSelection() {
  const navigate = useNavigate()
  const [tournaments, setTournaments] = useState([])
  const [query, setQuery] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => { getTournaments().then(({ data }) => setTournaments(data)).catch((requestError) => setError(requestError.message || 'Unable to load tournaments.')).finally(() => setIsLoading(false)) }, [])
  const visibleTournaments = useMemo(() => tournaments.filter((tournament) => `${tournament.name} ${tournament.year} ${tournament.hostCountry}`.toLowerCase().includes(query.toLowerCase())), [query, tournaments])
  if (isLoading) return <Loading label="Loading tournaments..." />

  return <div className="space-y-6"><div><p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-400">Tournament Centre</p><h1 className="mt-2 text-3xl font-bold text-white">Browse tournaments</h1><p className="mt-2 text-slate-400">Open a competition to inspect its teams, groups, standings, and fixtures.</p></div><ErrorMessage message={error} /><label className="block max-w-md text-sm font-medium text-slate-300">Search tournaments<input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Name, year, or host country" className="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-white outline-none focus:border-emerald-400" /></label>{visibleTournaments.length ? <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{visibleTournaments.map((tournament) => <Card key={tournament.id} className="flex flex-col justify-between gap-5"><div><div className="flex items-start justify-between gap-3"><div><h2 className="text-lg font-bold text-white">{tournament.name}</h2><p className="mt-1 text-sm text-slate-400">{tournament.year} · Host: {tournament.hostCountry}</p></div><StatusBadge status={tournament.status} /></div></div><Button onClick={() => navigate(`/tournaments/${tournament.id}`)}>Open tournament</Button></Card>)}</div> : <EmptyState title="No tournaments found" description={query ? 'Try a different search term.' : 'The backend returned no tournaments.'} />}</div>
}
