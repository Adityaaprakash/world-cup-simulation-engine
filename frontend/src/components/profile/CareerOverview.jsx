import Card from '../common/Card'
import EmptyState from '../common/EmptyState'

const display = (value) => value == null ? '—' : value
function Metric({ label, value, accent }) { return <div className="rounded-lg border border-slate-800 bg-slate-950/50 p-3"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p><p className={`mt-1 text-xl font-bold ${accent ? 'text-emerald-300' : 'text-slate-100'}`}>{display(value)}</p></div> }

export default function CareerOverview({ statistics, analytics }) {
  if (!statistics) return <EmptyState title="Career statistics unavailable" description="The backend did not return your manager career totals." />
  return <Card><h2 className="text-xl font-bold text-white">Career overview</h2><p className="mt-1 text-sm text-slate-400">Aggregate manager career information supplied by the backend.</p><div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5"><Metric label="Matches" value={statistics.matchesManaged} /><Metric label="Wins" value={statistics.wins} accent /><Metric label="Draws" value={statistics.draws} /><Metric label="Losses" value={statistics.losses} /><Metric label="Win rate" value={analytics?.winPercentage == null ? null : `${Number(analytics.winPercentage).toFixed(1)}%`} accent /><Metric label="Trophies" value={statistics.trophiesWon} accent /><Metric label="Clean sheets" value={statistics.cleanSheets} /><Metric label="Finals" value={statistics.finalsReached} /><Metric label="Semi-finals" value={statistics.semiFinalsReached} /><Metric label="Tournaments" value={statistics.tournamentsManaged} /></div></Card>
}
