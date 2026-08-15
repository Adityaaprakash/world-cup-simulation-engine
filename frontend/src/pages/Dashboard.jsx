import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Card from '../components/common/Card'
import EmptyState from '../components/common/EmptyState'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import {
  getAchievements,
  getCareerAnalytics,
  getCareerHistory,
  getCareerStatistics,
  getCareerTimeline,
  getCurrentManager,
} from '../api/managerApi'

const emptyDashboard = {
  manager: null,
  statistics: null,
  history: [],
  achievements: [],
  analytics: null,
  timeline: [],
}

const formatLabel = (value) => value ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (character) => character.toUpperCase()) : '—'
const number = (value) => value ?? 0
const percent = (value) => value == null ? '—' : `${Number(value).toFixed(1)}%`
const dateTime = (value) => value ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(value)) : '—'

function SectionHeading({ title, description }) {
  return <div className="mb-4"><h2 className="text-lg font-bold text-slate-100">{title}</h2>{description && <p className="mt-1 text-sm text-slate-400">{description}</p>}</div>
}

function Metric({ label, value, accent = false }) {
  return <div className="rounded-lg border border-slate-800 bg-slate-950/50 p-3"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p><p className={`mt-1 text-2xl font-bold ${accent ? 'text-emerald-400' : 'text-slate-100'}`}>{value}</p></div>
}

export default function Dashboard() {
  const [dashboard, setDashboard] = useState(emptyDashboard)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  const loadDashboard = useCallback(async () => {
    setIsLoading(true)
    setError('')
    const results = await Promise.allSettled([
      getCurrentManager(), getCareerStatistics(), getCareerHistory(), getAchievements(), getCareerAnalytics(), getCareerTimeline(),
    ])
    const [manager, statistics, history, achievements, analytics, timeline] = results
    const failedRequest = results.find((result) => result.status === 'rejected')

    setDashboard({
      manager: manager.status === 'fulfilled' ? manager.value.data : null,
      statistics: statistics.status === 'fulfilled' ? statistics.value.data : null,
      history: history.status === 'fulfilled' ? history.value.data : [],
      achievements: achievements.status === 'fulfilled' ? achievements.value.data : [],
      analytics: analytics.status === 'fulfilled' ? analytics.value.data : null,
      timeline: timeline.status === 'fulfilled' ? timeline.value.data : [],
    })
    if (failedRequest) setError(failedRequest.reason?.message || 'Some career information could not be loaded.')
    setIsLoading(false)
  }, [])

  useEffect(() => { loadDashboard() }, [loadDashboard])

  if (isLoading) return <Loading label="Loading your manager career..." />

  const { manager, statistics, history, achievements, analytics, timeline } = dashboard
  return (
    <div className="space-y-8">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div><p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-400">Manager Command Centre</p><h1 className="mt-2 text-3xl font-bold tracking-tight text-white sm:text-4xl">{manager?.displayName || manager?.username || 'Your Career'}</h1><p className="mt-2 max-w-2xl text-slate-400">Track your progress, achievements, and the decisions shaping your World Cup legacy.</p></div>
        {manager && <div className="rounded-lg border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-100"><span className="text-emerald-300">Level {number(manager.level)}</span><span className="mx-2 text-emerald-400/60">•</span>{number(manager.experiencePoints)} XP</div>}
      </div>

      <ErrorMessage message={error} />
      <Link to="/career" className="inline-flex rounded-lg border border-emerald-400/30 bg-emerald-400/10 px-4 py-2 text-sm font-semibold text-emerald-200 transition hover:bg-emerald-400/20">View full career</Link>

      {manager ? <Card><div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between"><div><SectionHeading title="Manager Overview" description={manager.nationality ? `National team manager from ${manager.nationality}.` : null} /><div className="flex flex-wrap gap-2">{[['Reputation', formatLabel(manager.reputation)], ['Coaching style', formatLabel(manager.coachingStyle)], ['Preferred formation', manager.favoriteFormation], ['Preferred tactics', manager.favoriteTacticalProfile]].map(([label, value]) => <span key={label} className="rounded-full border border-slate-700 bg-slate-950 px-3 py-1 text-sm text-slate-300"><span className="text-slate-500">{label}: </span>{value || '—'}</span>)}</div></div><div className="grid grid-cols-2 gap-3 sm:min-w-64"><Metric label="Level" value={number(manager.level)} accent /><Metric label="Experience" value={`${number(manager.experiencePoints)} XP`} /></div></div></Card> : <EmptyState title="Manager profile unavailable" description="Your manager profile could not be loaded. Try refreshing the page." />}

      {statistics ? <section><SectionHeading title="Career Statistics" description="Aggregate results from your completed management career." /><div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6"><Metric label="Matches" value={number(statistics.matchesManaged)} /><Metric label="Wins" value={number(statistics.wins)} accent /><Metric label="Draws" value={number(statistics.draws)} /><Metric label="Losses" value={number(statistics.losses)} /><Metric label="Trophies" value={number(statistics.trophiesWon)} accent /><Metric label="Clean sheets" value={number(statistics.cleanSheets)} /></div></section> : <EmptyState title="Career statistics unavailable" description="Career totals will appear once the backend returns your manager statistics." />}

      <div className="grid gap-6 xl:grid-cols-2">
        <Card><SectionHeading title="Career Form & Analytics" description="Patterns calculated from your completed career." />{analytics ? <div className="grid grid-cols-2 gap-3 sm:grid-cols-3"><Metric label="Win rate" value={percent(analytics.winPercentage)} accent /><Metric label="Goals scored" value={analytics.averageGoalsScored?.toFixed(2) ?? '—'} /><Metric label="Goals conceded" value={analytics.averageGoalsConceded?.toFixed(2) ?? '—'} /><Metric label="Possession" value={percent(analytics.averagePossession)} /><Metric label="Unbeaten run" value={number(analytics.longestUnbeatenStreak)} /><Metric label="Favourite formation" value={analytics.favoriteFormation || '—'} /></div> : <EmptyState title="No analytics yet" description="Career analytics will appear as tournament data is recorded." />}</Card>
        <Card><SectionHeading title="Achievements" description="Badges unlocked throughout your management career." />{achievements.length ? <ul className="space-y-3">{achievements.map((achievement) => <li key={`${achievement.code}-${achievement.unlockedAt}`} className="rounded-lg border border-slate-800 bg-slate-950/50 p-3"><div className="flex items-start justify-between gap-4"><div><p className="font-semibold text-slate-100">{achievement.title}</p><p className="mt-1 text-sm text-slate-400">{achievement.description}</p></div><span className="rounded-full bg-amber-400/10 px-2 py-1 text-xs font-bold text-amber-300">{formatLabel(achievement.badge)}</span></div><p className="mt-2 text-xs text-slate-500">Unlocked {dateTime(achievement.unlockedAt)}</p></li>)}</ul> : <EmptyState title="No achievements unlocked" description="Complete career milestones to build your trophy cabinet." />}</Card>
      </div>

      <div className="grid gap-6 xl:grid-cols-2">
        <Card><SectionHeading title="Career Timeline" description="Milestones and defining moments from your career." />{timeline.length ? <ol className="space-y-4 border-l border-slate-700 pl-5">{timeline.map((event) => <li key={event.id} className="relative"><span className="absolute -left-[1.8rem] top-1.5 h-2.5 w-2.5 rounded-full bg-emerald-400 ring-4 ring-slate-900" /><p className="text-xs font-semibold uppercase tracking-wide text-emerald-400">{formatLabel(event.eventType)} · {dateTime(event.occurredAt)}</p><h3 className="mt-1 font-semibold text-slate-100">{event.title}</h3><p className="mt-1 text-sm text-slate-400">{event.description}</p></li>)}</ol> : <EmptyState title="Career timeline is clear" description="Major management events will be recorded here as your career develops." />}</Card>
        <Card><SectionHeading title="Recent Career History" description="Your most recently completed tournament campaigns." />{history.length ? <div className="space-y-3">{history.map((entry) => <article key={entry.id} className="rounded-lg border border-slate-800 bg-slate-950/50 p-4"><div className="flex flex-wrap items-start justify-between gap-2"><div><h3 className="font-semibold text-slate-100">{entry.tournamentName}</h3><p className="mt-1 text-sm text-slate-400">{entry.teamName} · completed {dateTime(entry.dateCompleted)}</p></div><span className="rounded-full bg-slate-800 px-2 py-1 text-xs font-semibold text-slate-300">Position {number(entry.finishingPosition)}</span></div><div className="mt-3 grid grid-cols-3 gap-2 text-sm"><span className="text-slate-400">Wins <strong className="text-slate-100">{number(entry.wins)}</strong></span><span className="text-slate-400">Goals <strong className="text-slate-100">{number(entry.goalsScored)}</strong></span><span className="text-slate-400">Trophies <strong className="text-emerald-400">{number(entry.trophies)}</strong></span></div></article>)}</div> : <EmptyState title="No completed campaigns" description="Completed tournaments will form your career history." />}</Card>
      </div>
    </div>
  )
}
