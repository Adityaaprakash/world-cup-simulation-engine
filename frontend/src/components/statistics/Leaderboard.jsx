import Card from '../common/Card'
import EmptyState from '../common/EmptyState'

export default function Leaderboard({ title, entries, valueLabel, rating = false }) {
  return <Card><h2 className="text-lg font-bold text-white">{title}</h2>{entries?.length ? <ol className="mt-4 space-y-2">{entries.map((entry, index) => <li key={entry.playerId} className="grid grid-cols-[2rem_1fr_auto] items-center gap-3 rounded-lg border border-slate-800 bg-slate-950/50 p-3"><span className="font-bold text-emerald-400">{index + 1}</span><div className="min-w-0"><p className="truncate font-semibold text-slate-100">{entry.playerName}</p><p className="truncate text-xs text-slate-500">{entry.teamName}</p></div><span className="text-right text-sm font-bold text-white">{rating ? entry.averageRating?.toFixed(2) : entry.count} <span className="text-xs font-normal text-slate-500">{valueLabel}</span></span></li>)}</ol> : <EmptyState title="No leaderboard data" description="The backend returned no entries." />}</Card>
}
