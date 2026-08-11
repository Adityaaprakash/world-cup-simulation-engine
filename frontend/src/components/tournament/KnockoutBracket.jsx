import { Link } from 'react-router-dom'
import StatusBadge from './StatusBadge'
import { formatLabel } from '../../utils/football'

function MatchCard({ match, tournamentId }) {
  const finished = match.status === 'FINISHED'
  return <Link to={`/matches/${match.id}`} state={{ tournamentId }} className="block rounded-lg border border-slate-700 bg-slate-950/70 p-3 transition hover:border-emerald-400 hover:bg-emerald-400/5"><div className="flex items-center justify-between gap-2"><span className="text-xs font-semibold uppercase tracking-wide text-slate-500">Match {match.id}</span><StatusBadge status={match.status} /></div><div className="mt-3 grid grid-cols-[1fr_auto] gap-x-3 gap-y-2 text-sm"><span className="truncate font-semibold text-slate-100">{match.homeTeam}</span><span className="font-bold text-white">{finished ? match.homeScore : '-'}</span><span className="truncate font-semibold text-slate-100">{match.awayTeam}</span><span className="font-bold text-white">{finished ? match.awayScore : '-'}</span></div></Link>
}

export default function KnockoutBracket({ matches, tournamentId }) {
  const stages = [...new Set(matches.map((match) => match.round))]
  if (!stages.length) return null
  return <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">{stages.map((stage) => <section key={stage} className="min-w-0"><h2 className="mb-3 text-sm font-bold uppercase tracking-[0.14em] text-emerald-400">{formatLabel(stage)}</h2><div className="space-y-3">{matches.filter((match) => match.round === stage).map((match) => <MatchCard key={match.id} match={match} tournamentId={tournamentId} />)}</div></section>)}</div>
}
