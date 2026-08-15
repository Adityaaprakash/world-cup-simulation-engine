import { Link } from 'react-router-dom'
import Card from '../common/Card'

const label = (value) => value ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (character) => character.toUpperCase()) : '—'

export default function ProfileHeader({ manager, email }) {
  const name = manager?.displayName || manager?.username || email
  const initials = name ? name.split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase() : '?'

  if (!manager) return null
  return <Card className="overflow-hidden"><div className="flex flex-col gap-5 sm:flex-row sm:items-center"><div aria-hidden="true" className="flex h-20 w-20 shrink-0 items-center justify-center rounded-full bg-emerald-400/15 text-2xl font-bold text-emerald-300 ring-1 ring-emerald-400/30">{initials}</div><div className="min-w-0 flex-1"><p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-400">Manager profile</p><h1 className="mt-2 truncate text-3xl font-bold text-white">{name}</h1>{email && <p className="mt-1 text-sm text-slate-400">{email}</p>}<div className="mt-4 flex flex-wrap gap-2">{[['Reputation', label(manager.reputation)], ['Coaching style', label(manager.coachingStyle)], ['Nationality', manager.nationality], ['Preferred formation', manager.favoriteFormation]].filter(([, value]) => value && value !== '—').map(([title, value]) => <span key={title} className="rounded-full border border-slate-700 bg-slate-950/50 px-3 py-1 text-sm text-slate-300"><span className="text-slate-500">{title}: </span>{value}</span>)}</div><Link to="/career" className="mt-5 inline-flex rounded-lg bg-emerald-500 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-emerald-400">View full career</Link></div><div className="grid grid-cols-2 gap-3 sm:min-w-52"><div className="rounded-lg border border-slate-800 bg-slate-950/50 p-3"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Level</p><p className="mt-1 text-2xl font-bold text-emerald-300">{manager.level ?? '—'}</p></div><div className="rounded-lg border border-slate-800 bg-slate-950/50 p-3"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Experience</p><p className="mt-1 text-2xl font-bold text-slate-100">{manager.experiencePoints ?? '—'} XP</p></div></div></div></Card>
}
