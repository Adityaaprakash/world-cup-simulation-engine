import Card from '../common/Card'
import EmptyState from '../common/EmptyState'

export default function RankingTable({ title, entries }) {
  return <Card><h2 className="text-lg font-bold text-white">{title}</h2>{entries?.length ? <div className="mt-4 overflow-x-auto"><table className="min-w-[28rem] w-full text-sm"><thead className="border-b border-slate-800 text-left text-xs uppercase tracking-wide text-slate-500"><tr><th className="p-2">Rank</th><th className="p-2">Name</th><th className="p-2">Detail</th><th className="p-2 text-right">Score</th></tr></thead><tbody>{entries.map((entry) => <tr key={entry.id} className="border-b border-slate-800/80"><td className="p-2 font-bold text-emerald-400">{entry.rank}</td><td className="p-2 font-semibold text-slate-100">{entry.name}</td><td className="p-2 text-slate-400">{entry.detail}</td><td className="p-2 text-right font-bold text-slate-100">{entry.score}</td></tr>)}</tbody></table></div> : <EmptyState title="No rankings available" description="The backend returned no historical ranking entries." />}</Card>
}
