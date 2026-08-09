import Button from '../common/Button'

export default function PlayerCard({ player, selected = false, inSquad = false, actionLabel, onAction, disabled = false }) {
  return (
    <article className={`rounded-xl border p-4 transition ${selected ? 'border-emerald-400 bg-emerald-400/10 shadow-md shadow-emerald-950/20' : 'border-slate-800 bg-slate-900/80'}`}>
      <div className="flex items-start justify-between gap-3"><div><p className="font-semibold text-slate-100">{player.name}</p><p className="mt-1 text-xs font-bold uppercase tracking-wider text-emerald-400">{player.position}</p></div><div className="rounded-lg bg-slate-950 px-2 py-1 text-center"><p className="text-xs text-slate-500">OVR</p><p className="font-bold text-white">{player.overallRating}</p></div></div>
      <div className="mt-4 flex items-center justify-between gap-3"><span className={`text-xs font-medium ${inSquad ? 'text-emerald-300' : 'text-slate-500'}`}>{inSquad ? 'In your squad' : 'Roster player'}</span>{actionLabel && <Button variant={selected ? 'secondary' : 'primary'} className="px-3 py-1.5 text-sm" onClick={onAction} disabled={disabled}>{actionLabel}</Button>}</div>
    </article>
  )
}
