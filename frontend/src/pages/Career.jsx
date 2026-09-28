import { useEffect, useState } from 'react'
import { getCareerAnalytics, getCareerHistory, getCareerStatistics, getCareerTimeline, getCurrentManager, getMyJobs, getManagerEvents } from '../api/managerApi'
import Card from '../components/common/Card'
import EmptyState from '../components/common/EmptyState'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import CareerHeader from '../components/career/CareerHeader'
import CareerHistory from '../components/career/CareerHistory'
import CareerPerformance from '../components/career/CareerPerformance'
import CareerStatistics from '../components/career/CareerStatistics'
import CareerSummary from '../components/career/CareerSummary'
import CareerTimeline from '../components/career/CareerTimeline'
import ManagerJobs from '../components/career/ManagerJobs'
import ManagerObjectives from '../components/career/ManagerObjectives'
import ManagerEvents from '../components/career/ManagerEvents'

const tabs = ['overview', 'objectives', 'statistics', 'performance', 'history', 'timeline', 'jobs', 'events']
const labels = { overview: 'Overview', objectives: 'Objectives', statistics: 'Statistics', performance: 'Performance', history: 'History', timeline: 'Timeline', jobs: 'Jobs', events: 'Events' }
const emptyData = { manager: null, statistics: null, analytics: null, history: [], timeline: [], jobs: [], events: [] }
const initialLoading = { manager: true, statistics: true, analytics: true, history: true, timeline: true, jobs: true, events: true }

export default function Career() {
  const [tab, setTab] = useState('overview')
  const [data, setData] = useState(emptyData)
  const [loading, setLoading] = useState(initialLoading)
  const [errors, setErrors] = useState({})

  useEffect(() => {
    let active = true
    const requests = { manager: getCurrentManager, statistics: getCareerStatistics, analytics: getCareerAnalytics, history: getCareerHistory, timeline: getCareerTimeline, jobs: getMyJobs, events: getManagerEvents }
    Object.entries(requests).forEach(([key, request]) => {
      request()
        .then(({ data: value }) => { if (active) setData((current) => ({ ...current, [key]: value })) })
        .catch((requestError) => { if (active) setErrors((current) => ({ ...current, [key]: requestError.message || `Unable to load ${key}.` })) })
        .finally(() => { if (active) setLoading((current) => ({ ...current, [key]: false })) })
    })
    return () => { active = false }
  }, [])

  const overview = <div className="space-y-6">{loading.statistics ? <Loading label="Loading career summary..." /> : errors.statistics ? <ErrorMessage message={errors.statistics} /> : <><CareerSummary manager={data.manager} statistics={data.statistics} /><CareerStatistics statistics={data.statistics} /></>}{loading.analytics ? <Loading label="Loading career performance..." /> : errors.analytics ? <ErrorMessage message={errors.analytics} /> : <CareerPerformance analytics={data.analytics} />}<ManagerObjectives />{loading.events ? null : errors.events ? null : (<ManagerEvents initialEvents={data.events} />)}</div>
  const content = tab === 'overview' ? overview : tab === 'objectives' ? <ManagerObjectives /> : tab === 'statistics' ? (loading.statistics ? <Loading label="Loading career statistics..." /> : errors.statistics ? <ErrorMessage message={errors.statistics} /> : <CareerStatistics statistics={data.statistics} />) : tab === 'performance' ? (loading.analytics ? <Loading label="Loading career performance..." /> : errors.analytics ? <ErrorMessage message={errors.analytics} /> : <CareerPerformance analytics={data.analytics} />) : tab === 'history' ? (loading.history ? <Loading label="Loading career history..." /> : errors.history ? <ErrorMessage message={errors.history} /> : <CareerHistory entries={data.history} />) : tab === 'timeline' ? (loading.timeline ? <Loading label="Loading career timeline..." /> : errors.timeline ? <ErrorMessage message={errors.timeline} /> : <CareerTimeline events={data.timeline} />) : tab === 'events' ? (loading.events ? <Loading label="Loading manager events..." /> : errors.events ? <ErrorMessage message={errors.events} /> : <ManagerEvents initialEvents={data.events} />) : (loading.jobs ? <Loading label="Loading manager jobs..." /> : errors.jobs ? <ErrorMessage message={errors.jobs} /> : <ManagerJobs jobs={data.jobs} />)

  const pendingEventsCount = data.events ? data.events.filter(e => e.status === 'PENDING').length : 0

  return <div className="space-y-7"><div><p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-400">Manager progression</p><p className="mt-2 text-slate-400">Detailed backend-recorded career statistics, performance, tournament history, and milestones.</p></div>{loading.manager ? <Loading label="Loading career identity..." /> : errors.manager ? <ErrorMessage message={errors.manager} /> : data.manager ? <CareerHeader manager={data.manager} /> : <EmptyState title="Career profile unavailable" description="The backend did not return your manager profile." />}<Card className="p-3"><nav className="flex max-w-full gap-2 overflow-x-auto" aria-label="Career sections">{tabs.map((item) => <button key={item} type="button" onClick={() => setTab(item)} className={`whitespace-nowrap rounded-lg px-4 py-2 text-sm font-semibold relative ${tab === item ? 'bg-emerald-500 text-slate-950' : 'bg-slate-800 text-slate-300 hover:bg-slate-700'}`}>{labels[item]} {item === 'events' && pendingEventsCount > 0 && <span className="absolute -top-1 -right-1 inline-flex items-center justify-center w-4 h-4 text-[10px] font-bold text-white bg-red-500 rounded-full">{pendingEventsCount}</span>}</button>)}</nav></Card>{content}</div>
}
