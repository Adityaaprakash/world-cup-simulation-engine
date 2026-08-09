const POSITION_ROWS = [
  { name: 'Attack', slots: ['LW', 'RW', 'ST', 'CF'] },
  { name: 'Midfield', slots: ['CDM', 'CM', 'CAM', 'LM', 'RM'] },
  { name: 'Defence', slots: ['LB', 'CB', 'RB', 'LWB', 'RWB'] },
  { name: 'Goalkeeper', slots: ['GK'] },
]

export default function FormationPitch({ players, activePlayerId, onSelectPlayer }) {
  return (
    <div className="relative overflow-hidden rounded-2xl border border-emerald-300/30 bg-emerald-950 p-4 shadow-inner shadow-emerald-950/60">
      <div className="pointer-events-none absolute inset-4 rounded-lg border border-emerald-200/30" /><div className="pointer-events-none absolute left-4 right-4 top-1/2 border-t border-emerald-200/30" /><div className="pointer-events-none absolute left-1/2 top-1/2 h-20 w-20 -translate-x-1/2 -translate-y-1/2 rounded-full border border-emerald-200/30" />
      <div className="relative z-10 flex min-h-[32rem] flex-col justify-between gap-5 py-2">
        {POSITION_ROWS.map((row) => {
          const rowPlayers = players.filter((player) => row.slots.includes(player.positionSlot || player.position))
          return <div key={row.name} className="flex min-h-13 items-center justify-center gap-2 sm:gap-4">{rowPlayers.length ? rowPlayers.map((player) => <button key={player.id} type="button" onClick={() => onSelectPlayer?.(player.id)} className={`w-20 rounded-lg border px-2 py-2 text-center text-xs shadow-lg transition sm:w-28 ${activePlayerId === player.id ? 'border-amber-300 bg-amber-300 text-slate-950' : 'border-emerald-200/50 bg-emerald-800 text-white hover:bg-emerald-700'}`}><span className="block font-bold">{player.positionSlot || player.position}</span><span className="mt-0.5 block truncate">{player.name}</span></button>) : <span className="rounded-full bg-slate-950/30 px-3 py-1 text-xs text-emerald-100/60">{row.name}</span>}</div>
        })}
      </div>
    </div>
  )
}
