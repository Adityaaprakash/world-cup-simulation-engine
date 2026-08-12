import Card from '../common/Card'
import EmptyState from '../common/EmptyState'

const statistics = [
  ['Possession', 'possession', '%'],
  ['Shots', 'shots', ''],
  ['Shots on target', 'shotsOnTarget', ''],
  ['Passes', 'passes', ''],
  ['Pass accuracy', 'passAccuracy', '%'],
  ['Corners', 'corners', ''],
  ['Fouls', 'fouls', ''],
  ['Offsides', 'offsides', ''],
  ['Saves', 'saves', ''],
  ['Expected goals', 'expectedGoals', ''],
]

const formatValue = (value, suffix) => value == null ? '—' : `${value}${suffix}`
const width = (value, opposingValue) => {
  if (value == null || opposingValue == null || Number(value) + Number(opposingValue) === 0) return 50
  return (Number(value) / (Number(value) + Number(opposingValue))) * 100
}

export default function MatchStatistics({ statistics: matchStatistics, homeTeam, awayTeam }) {
  if (!matchStatistics?.homeTeam || !matchStatistics?.awayTeam) return <EmptyState title="Match statistics unavailable" description="The backend has not persisted statistics for this match." />
  const availableStatistics = statistics.filter(([, key]) => matchStatistics.homeTeam[key] != null || matchStatistics.awayTeam[key] != null)
  if (!availableStatistics.length) return <EmptyState title="Match statistics unavailable" description="The returned statistics contain no displayable values." />
  return <Card><h2 className="text-xl font-bold text-white">Match statistics</h2><div className="mt-1 flex justify-between gap-4 text-sm text-slate-400"><span className="truncate">{homeTeam}</span><span className="truncate text-right">{awayTeam}</span></div><div className="mt-5 space-y-5">{availableStatistics.map(([label, key, suffix]) => { const homeValue = matchStatistics.homeTeam[key]; const awayValue = matchStatistics.awayTeam[key]; const homeWidth = width(homeValue, awayValue); return <div key={key}><div className="grid grid-cols-[3.5rem_1fr_3.5rem] items-center gap-3 text-sm"><span className="font-bold text-slate-100">{formatValue(homeValue, suffix)}</span><span className="text-center text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</span><span className="text-right font-bold text-slate-100">{formatValue(awayValue, suffix)}</span></div><div className="mt-2 flex h-2 overflow-hidden rounded-full bg-slate-800"><span className="bg-emerald-400" style={{ width: `${homeWidth}%` }} /><span className="bg-sky-400" style={{ width: `${100 - homeWidth}%` }} /></div></div> })}</div><p className="mt-5 text-xs text-slate-500"><span className="text-emerald-400">■</span> {homeTeam} <span className="ml-3 text-sky-400">■</span> {awayTeam}</p></Card>
}
