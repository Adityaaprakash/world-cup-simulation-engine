import EmptyState from '../common/EmptyState'
import { formatLabel } from '../../utils/football'

const typeStyle = { GOAL: 'bg-emerald-400/15 text-emerald-200 border-emerald-400/30', OWN_GOAL: 'bg-amber-400/15 text-amber-100 border-amber-400/30', PENALTY: 'bg-violet-400/15 text-violet-100 border-violet-400/30', YELLOW_CARD: 'bg-yellow-300/15 text-yellow-100 border-yellow-300/30', RED_CARD: 'bg-rose-400/15 text-rose-100 border-rose-400/30', SUBSTITUTION: 'bg-sky-400/15 text-sky-100 border-sky-400/30', ASSIST: 'bg-slate-700 text-slate-200 border-slate-600' }
const labels = { GOAL: 'GOAL', OWN_GOAL: 'OWN GOAL', PENALTY: 'PENALTY', YELLOW_CARD: 'YELLOW', RED_CARD: 'RED', SUBSTITUTION: 'SUB', ASSIST: 'ASSIST' }

export default function MatchTimeline({ events }) {
  const orderedEvents = [...(events || [])].sort((first, second) => (first.minute ?? 0) - (second.minute ?? 0))
  if (!orderedEvents.length) return <EmptyState title="No match events recorded" description="The backend has not persisted any timeline events for this match." />
  return <ol className="space-y-3 border-l border-slate-700 pl-5">{orderedEvents.map((event, index) => <li key={`${event.minute}-${event.eventType}-${event.player}-${index}`} className="relative"><span className="absolute -left-[1.85rem] top-3 h-3 w-3 rounded-full bg-emerald-400 ring-4 ring-slate-950" /><article className={`rounded-lg border p-4 ${typeStyle[event.eventType] || 'border-slate-700 bg-slate-900 text-slate-100'}`}><div className="flex flex-wrap items-center justify-between gap-2"><div className="flex items-center gap-2"><span className="text-xs font-bold uppercase tracking-wide">{labels[event.eventType] || formatLabel(event.eventType)}</span>{event.minute != null && <span className="rounded bg-slate-950/30 px-2 py-0.5 text-xs font-bold">{event.minute}'</span>}</div>{event.player && <span className="font-semibold">{event.player}</span>}</div>{event.description && <p className="mt-2 text-sm leading-6 text-slate-200/90">{event.description}</p>}</article></li>)}</ol>
}
