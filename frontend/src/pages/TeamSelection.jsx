import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Card from '../components/common/Card'
import EmptyState from '../components/common/EmptyState'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import Button from '../components/common/Button'
import { getTeams } from '../api/teamApi'

export default function TeamSelection() {
  const navigate = useNavigate()
  const [teams, setTeams] = useState([])
  const [query, setQuery] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getTeams().then(({ data }) => setTeams(data)).catch((requestError) => setError(requestError.message || 'Unable to load national teams.')).finally(() => setIsLoading(false))
  }, [])

  const filteredTeams = useMemo(() => teams.filter((team) => team.name.toLowerCase().includes(query.toLowerCase())), [teams, query])
  if (isLoading) return <Loading label="Loading national teams..." />

  return <div className="space-y-6"><div><p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-400">National Team Hub</p><h1 className="mt-2 text-3xl font-bold text-white">Choose a national team</h1><p className="mt-2 text-slate-400">Explore the available national squads and prepare your selection.</p></div><ErrorMessage message={error} /><label className="block max-w-md text-sm font-medium text-slate-300">Search teams<input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search by team name" className="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-white outline-none focus:border-emerald-400" /></label>{filteredTeams.length ? <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{filteredTeams.map((team) => <Card key={team.id} className="flex flex-col justify-between gap-5"><div><div className="flex items-start justify-between gap-4"><h2 className="text-lg font-bold text-slate-100">{team.name}</h2><span className="rounded-lg bg-emerald-400/10 px-2 py-1 text-sm font-bold text-emerald-300">{team.overallRating} OVR</span></div><p className="mt-3 text-sm text-slate-400">National team squad</p></div><Button onClick={() => navigate(`/teams/${team.id}/squad`)}>Manage squad</Button></Card>)}</div> : <EmptyState title="No teams found" description={query ? 'Try a different team name.' : 'The backend returned no national teams.'} />}</div>
}
