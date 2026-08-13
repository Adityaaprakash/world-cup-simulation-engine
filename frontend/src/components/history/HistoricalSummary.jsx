import Card from '../common/Card'

function Metric({ label, value }) { return <div className="rounded-lg border border-slate-800 bg-slate-950/50 p-3"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p><p className="mt-1 text-2xl font-bold text-slate-100">{value ?? '—'}</p></div> }

export default function HistoricalSummary({ summary }) {
  if (!summary) return null
  return <Card><h2 className="text-xl font-bold text-white">Historical overview</h2><div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-5"><Metric label="Players profiled" value={summary.playersProfiled} /><Metric label="Teams profiled" value={summary.teamsProfiled} /><Metric label="Managers profiled" value={summary.managersProfiled} /><Metric label="Completed tournaments" value={summary.completedTournaments} /><Metric label="Recorded matches" value={summary.recordedMatches} /></div><div className="mt-5 grid gap-3 md:grid-cols-3">{[['Greatest player', summary.greatestPlayer], ['Greatest team', summary.greatestTeam], ['Greatest manager', summary.greatestManager]].map(([label, value]) => <div key={label} className="rounded-lg border border-amber-400/20 bg-amber-400/5 p-3"><p className="text-xs font-bold uppercase tracking-wide text-amber-300">{label}</p><p className="mt-1 font-semibold text-slate-100">{value || '—'}</p></div>)}</div></Card>
}
