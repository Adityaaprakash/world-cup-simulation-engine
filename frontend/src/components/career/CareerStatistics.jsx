import Card from '../common/Card'
import EmptyState from '../common/EmptyState'

function Metric({ label, value, accent }) { return <div className="rounded-lg border border-slate-800 bg-slate-950/50 p-3"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p><p className={`mt-1 text-xl font-bold ${accent ? 'text-emerald-300' : 'text-slate-100'}`}>{value ?? '—'}</p></div> }

export default function CareerStatistics({ statistics }) {
  if (!statistics) return <EmptyState title="Career statistics unavailable" description="The backend has not returned career totals yet." />
  const metrics = [['Tournaments managed', statistics.tournamentsManaged], ['Matches managed', statistics.matchesManaged], ['Wins', statistics.wins, true], ['Draws', statistics.draws], ['Losses', statistics.losses], ['Goals scored', statistics.goalsScored, true], ['Goals conceded', statistics.goalsConceded], ['Clean sheets', statistics.cleanSheets], ['Trophies won', statistics.trophiesWon, true], ['Finals reached', statistics.finalsReached], ['Semi-finals reached', statistics.semiFinalsReached]]
  return <Card><h2 className="text-xl font-bold text-white">Career statistics</h2><p className="mt-1 text-sm text-slate-400">All persisted career totals for this manager.</p><div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-4">{metrics.map(([label, value, accent]) => <Metric key={label} label={label} value={value} accent={accent} />)}</div></Card>
}
