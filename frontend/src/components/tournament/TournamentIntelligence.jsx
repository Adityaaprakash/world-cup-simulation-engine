import EmptyState from '../common/EmptyState'

const insights = [
  ['Biggest Upset', 'biggestUpset'],
  ['Longest Streak', 'longestStreak'],
  ['Champion Path', 'championPath'],
  ['Most Entertaining Match', 'mostEntertainingMatch'],
  ['Highest-Scoring Match', 'highestScoringMatch'],
]

export default function TournamentIntelligence({ summary }) {
  const availableInsights = summary ? insights.filter(([, key]) => summary[key]) : []
  if (!availableInsights.length) return <EmptyState title="No tournament intelligence yet" description="Notable matches, paths, and streaks will appear as persisted tournament data becomes available." />
  return <div className="grid gap-3 md:grid-cols-2">{availableInsights.map(([label, key]) => <article key={key} className="rounded-lg border border-slate-800 bg-slate-950/50 p-4"><p className="text-xs font-bold uppercase tracking-[0.14em] text-emerald-400">{label}</p><p className="mt-2 text-sm leading-6 text-slate-200">{summary[key]}</p></article>)}</div>
}
