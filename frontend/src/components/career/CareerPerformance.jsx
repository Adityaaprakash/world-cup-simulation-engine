import Card from '../common/Card'
import EmptyState from '../common/EmptyState'

const label = (value) => value ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (character) => character.toUpperCase()) : null
const decimal = (value) => value == null ? null : Number(value).toFixed(2)

export default function CareerPerformance({ analytics }) {
  if (!analytics) return <EmptyState title="No career analytics yet" description="Performance analysis will appear when the backend has recorded career data." />
  const metrics = [['Win percentage', analytics.winPercentage == null ? null : `${Number(analytics.winPercentage).toFixed(1)}%`], ['Average goals scored', decimal(analytics.averageGoalsScored)], ['Average goals conceded', decimal(analytics.averageGoalsConceded)], ['Average possession', analytics.averagePossession == null ? null : `${Number(analytics.averagePossession).toFixed(1)}%`], ['Longest unbeaten streak', analytics.longestUnbeatenStreak], ['Favourite formation', analytics.favoriteFormation], ['Favourite tactics', analytics.favoriteTactics], ['Tactical profile', label(analytics.tacticalProfile)], ['Most used lineup', analytics.mostUsedLineup], ['Most selected captain', analytics.mostSelectedCaptain], ['Most trusted players', analytics.mostTrustedPlayers]].filter(([, value]) => value != null && value !== '')
  return <Card><h2 className="text-xl font-bold text-white">Career performance</h2><p className="mt-1 text-sm text-slate-400">Analytics recalculated by the backend from persisted career data.</p>{metrics.length ? <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{metrics.map(([title, value]) => <div key={title} className="rounded-lg border border-slate-800 bg-slate-950/50 p-3"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{title}</p><p className="mt-1 font-semibold text-slate-100">{value}</p></div>)}</div> : <div className="mt-5"><EmptyState title="No displayable analytics" description="The backend returned no completed analytics values yet." /></div>}</Card>
}
