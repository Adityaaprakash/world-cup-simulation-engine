import Card from '../common/Card'
import EmptyState from '../common/EmptyState'

const value = (item) => item ?? '—'

export default function MatchDiscipline({ statistics, homeTeam, awayTeam }) {
  if (!statistics?.homeTeam || !statistics?.awayTeam) return <EmptyState title="Discipline summary unavailable" description="The backend has not persisted card totals for this match." />
  const hasCards = ['yellowCards', 'redCards'].some((key) => statistics.homeTeam[key] != null || statistics.awayTeam[key] != null)
  if (!hasCards) return <EmptyState title="Discipline summary unavailable" description="No persisted card totals are available for this match." />
  return <Card><h2 className="text-xl font-bold text-white">Discipline</h2><div className="mt-4 grid grid-cols-2 gap-3 text-center"><div className="rounded-lg border border-slate-800 bg-slate-950/50 p-4"><p className="truncate font-semibold text-slate-100">{homeTeam}</p><p className="mt-3 text-sm text-yellow-200">Yellow cards <strong className="ml-1 text-lg">{value(statistics.homeTeam.yellowCards)}</strong></p><p className="mt-2 text-sm text-rose-200">Red cards <strong className="ml-1 text-lg">{value(statistics.homeTeam.redCards)}</strong></p></div><div className="rounded-lg border border-slate-800 bg-slate-950/50 p-4"><p className="truncate font-semibold text-slate-100">{awayTeam}</p><p className="mt-3 text-sm text-yellow-200">Yellow cards <strong className="ml-1 text-lg">{value(statistics.awayTeam.yellowCards)}</strong></p><p className="mt-2 text-sm text-rose-200">Red cards <strong className="ml-1 text-lg">{value(statistics.awayTeam.redCards)}</strong></p></div></div></Card>
}
