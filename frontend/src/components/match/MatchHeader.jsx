import Card from '../common/Card'
import StatusBadge from '../tournament/StatusBadge'
import { formatLabel } from '../../utils/football'

const score = (value) => value ?? '-'

export default function MatchHeader({ match }) {
  const completed = match.status === 'FINISHED'
  return <Card className="overflow-hidden border-emerald-500/25 bg-gradient-to-br from-slate-900 via-slate-900 to-emerald-950/30"><div className="flex flex-wrap items-center justify-center gap-2 text-xs font-bold uppercase tracking-[0.14em] text-emerald-300">{match.round && <span>{formatLabel(match.round)}</span>}{match.group && <><span className="text-slate-600">•</span><span>{match.group}</span></>}</div><div className="mt-6 grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-3 text-center sm:gap-7"><div><h1 className="break-words text-xl font-bold text-white sm:text-3xl">{match.homeTeam}</h1></div><div><div className="rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 sm:px-7"><p className="text-3xl font-black tracking-tight text-white sm:text-5xl">{score(match.homeScore)} <span className="text-emerald-400">–</span> {score(match.awayScore)}</p></div><div className="mt-3"><StatusBadge status={match.status} /></div></div><div><h2 className="break-words text-xl font-bold text-white sm:text-3xl">{match.awayTeam}</h2></div></div>{completed && match.winner && <p className="mt-5 text-center text-sm font-semibold text-amber-300">Winner: {match.winner}</p>}</Card>
}
