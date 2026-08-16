import Card from '../common/Card'

const badgeClass = { BRONZE: 'border-amber-700/50 bg-amber-700/15 text-amber-200', SILVER: 'border-slate-400/40 bg-slate-300/10 text-slate-200', GOLD: 'border-yellow-400/40 bg-yellow-400/10 text-yellow-200', PLATINUM: 'border-sky-300/40 bg-sky-300/10 text-sky-100', DIAMOND: 'border-violet-300/40 bg-violet-300/10 text-violet-100' }
const date = (value) => value ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : null
const label = (value) => value ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (character) => character.toUpperCase()) : null

export default function AchievementCard({ achievement }) {
  return <Card className="flex flex-col justify-between gap-5 border-emerald-400/20"><div><div className="flex flex-wrap items-start justify-between gap-3"><div><p className="text-xs font-bold uppercase tracking-[0.14em] text-emerald-300">Unlocked achievement</p><h2 className="mt-2 text-xl font-bold text-white">{achievement.title}</h2></div>{achievement.badge && <span className={`rounded-full border px-3 py-1 text-xs font-bold ${badgeClass[achievement.badge] || 'border-slate-700 bg-slate-800 text-slate-200'}`}>{label(achievement.badge)}</span>}</div>{achievement.description && <p className="mt-4 text-sm leading-6 text-slate-400">{achievement.description}</p>}</div><div className="border-t border-slate-800 pt-3 text-sm text-slate-500"><span className="font-semibold text-slate-300">{label(achievement.code)}</span>{date(achievement.unlockedAt) && <span className="mt-1 block">Unlocked {date(achievement.unlockedAt)}</span>}</div></Card>
}
