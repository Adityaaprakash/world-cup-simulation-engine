import EmptyState from '../common/EmptyState'

export default function MatchCommentary({ commentary }) {
  const orderedCommentary = [...(commentary || [])].sort((first, second) => (first.minute ?? 0) - (second.minute ?? 0))
  if (!orderedCommentary.length) return <EmptyState title="No commentary available" description="The backend has not returned generated commentary for this match." />
  return <div className="space-y-3">{orderedCommentary.map((entry, index) => <article key={`${entry.minute}-${index}`} className="rounded-lg border border-slate-800 bg-slate-950/50 p-4"><div className="flex items-start gap-3"><span className="rounded bg-emerald-400/10 px-2 py-1 text-xs font-bold text-emerald-300">{entry.minute != null ? `${entry.minute}'` : 'Event'}</span><p className="text-sm leading-6 text-slate-300">{entry.commentary}</p></div></article>)}</div>
}
