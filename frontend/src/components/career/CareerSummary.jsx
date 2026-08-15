import Card from '../common/Card'

function Metric({ label, value, accent }) { return <div className="rounded-lg border border-slate-800 bg-slate-950/50 p-3"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p><p className={`mt-1 text-xl font-bold ${accent ? 'text-emerald-300' : 'text-slate-100'}`}>{value ?? '—'}</p></div> }

export default function CareerSummary({ manager, statistics }) {
  return <Card><h2 className="text-xl font-bold text-white">Career summary</h2><p className="mt-1 text-sm text-slate-400">A concise view of backend-recorded manager progression.</p><div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6"><Metric label="Level" value={manager?.level} accent /><Metric label="Reputation" value={manager?.reputation?.replaceAll('_', ' ')} /><Metric label="Trophies" value={statistics?.trophiesWon} accent /><Metric label="Tournaments" value={statistics?.tournamentsManaged} /><Metric label="Matches" value={statistics?.matchesManaged} /><Metric label="Wins" value={statistics?.wins} accent /></div></Card>
}
