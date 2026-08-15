import { Link } from 'react-router-dom'
import Card from '../common/Card'
import EmptyState from '../common/EmptyState'

const date = (value) => value ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '—'
const label = (value) => value ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (character) => character.toUpperCase()) : 'Career event'

export default function CareerTimeline({ events }) {
  if (!events?.length) return <EmptyState title="No career timeline events yet." description="Progression, tournament, and milestone events will appear as they are recorded." />
  return <Card><h2 className="text-xl font-bold text-white">Career timeline</h2><p className="mt-1 text-sm text-slate-400">Chronological events persisted for this manager career.</p><ol className="mt-5 space-y-5 border-l border-slate-700 pl-5">{events.map((event) => <li key={event.id} className="relative"><span className="absolute -left-[1.8rem] top-1.5 h-2.5 w-2.5 rounded-full bg-emerald-400 ring-4 ring-slate-900" /><p className="text-xs font-semibold uppercase tracking-wide text-emerald-400">{label(event.eventType)} · {date(event.occurredAt)}</p><h3 className="mt-1 font-semibold text-white">{event.title}</h3>{event.description && <p className="mt-1 text-sm text-slate-400">{event.description}</p>}<div className="mt-2 flex flex-wrap gap-3 text-sm">{event.tournamentId != null && <Link to={`/tournaments/${event.tournamentId}`} className="font-semibold text-emerald-300 hover:text-emerald-200">Tournament</Link>}{event.teamId != null && <Link to={`/teams/${event.teamId}/squad`} className="font-semibold text-emerald-300 hover:text-emerald-200">Team</Link>}</div></li>)}</ol></Card>
}
