import { useEffect, useState } from 'react'
import { getCurrentManager, getManagerLeaderboards } from '../api/managerApi'
import Card from '../components/common/Card'
import EmptyState from '../components/common/EmptyState'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'

const categories = [
  ['highestWinRate', 'Highest win rate', 'Win rate'],
  ['mostTrophies', 'Most trophies', 'Trophies'],
  ['mostMatches', 'Most matches managed', 'Matches'],
  ['longestUnbeatenStreak', 'Longest unbeaten streak', 'Unbeaten run'],
  ['highestReputation', 'Highest reputation', 'Reputation'],
]
const label = (value) => value ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (character) => character.toUpperCase()) : '—'
const categoryValue = (entry, key) => key === 'highestWinRate' ? `${Number(entry.winPercentage ?? 0).toFixed(2)}%` : key === 'mostTrophies' ? entry.trophiesWon : key === 'mostMatches' ? entry.matchesManaged : key === 'longestUnbeatenStreak' ? entry.longestUnbeatenStreak : label(entry.reputation)

export default function ManagerLeaderboard() {
  const [boards, setBoards] = useState(null); const [manager, setManager] = useState(null); const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [category, setCategory] = useState('highestWinRate')
  useEffect(() => { let active = true; Promise.allSettled([getManagerLeaderboards(), getCurrentManager()]).then(([leaderboardResult, managerResult]) => { if (!active) return; if (leaderboardResult.status === 'fulfilled') setBoards(leaderboardResult.value.data); else setError(leaderboardResult.reason?.message || 'Unable to load manager leaderboards.'); if (managerResult.status === 'fulfilled') setManager(managerResult.value.data); }).finally(() => { if (active) setLoading(false) }); return () => { active = false } }, [])
  if (loading) return <Loading label="Loading manager leaderboards..." />
  const [key, title, valueLabel] = categories.find(([item]) => item === category)
  const entries = boards?.[key] || []
  return <div className="space-y-7"><div><p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-400">Manager rankings</p><h1 className="mt-2 text-3xl font-bold text-white">Manager leaderboards</h1><p className="mt-2 text-slate-400">Server-ordered top manager lists from persisted career statistics and analytics.</p></div><ErrorMessage message={error} /><Card className="p-3"><nav className="flex max-w-full gap-2 overflow-x-auto" aria-label="Manager leaderboard categories">{categories.map(([item, name]) => <button type="button" key={item} onClick={() => setCategory(item)} className={`whitespace-nowrap rounded-lg px-4 py-2 text-sm font-semibold ${category === item ? 'bg-emerald-500 text-slate-950' : 'bg-slate-800 text-slate-300 hover:bg-slate-700'}`}>{name}</button>)}</nav></Card>{entries.length ? <Card><h2 className="text-xl font-bold text-white">{title}</h2><p className="mt-1 text-sm text-slate-400">The backend returns this ordered top-10 list. Positions indicate order within this list.</p><div className="mt-5 overflow-x-auto"><table className="min-w-[40rem] w-full text-sm"><thead className="border-b border-slate-800 text-left text-xs uppercase tracking-wide text-slate-500"><tr><th className="p-3">Position</th><th className="p-3">Manager</th><th className="p-3">Reputation</th><th className="p-3">Level</th><th className="p-3 text-right">{valueLabel}</th></tr></thead><tbody>{entries.map((entry, index) => <tr key={entry.managerId} className={`border-b border-slate-800/80 ${entry.managerId === manager?.id ? 'bg-emerald-400/10' : ''}`}><td className="p-3 font-bold text-emerald-300">{index + 1}</td><td className="p-3 font-semibold text-slate-100">{entry.displayName}{entry.managerId === manager?.id && <span className="ml-2 text-xs font-bold text-emerald-300">YOU</span>}</td><td className="p-3 text-slate-300">{label(entry.reputation)}</td><td className="p-3">{entry.level ?? '—'}</td><td className="p-3 text-right font-bold text-slate-100">{categoryValue(entry, key) ?? '—'}</td></tr>)}</tbody></table></div></Card> : <EmptyState title="No manager leaderboard entries." description="The backend returned no managers for this category." />}</div>
}
