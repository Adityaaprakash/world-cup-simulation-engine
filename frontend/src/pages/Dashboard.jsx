import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Card from '../components/common/Card'
import EmptyState from '../components/common/EmptyState'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import axiosClient from '../api/axiosClient'
import {
  getCurrentManager,
  getCareerStatistics,
  getCareerHistory,
  getPendingEvents,
  getObjectives,
  getMyJobs
} from '../api/managerApi'
import { getMySquads, getSquadPlayers } from '../api/squadApi'
import { getTournaments } from '../api/tournamentApi'

function SectionHeading({ title, description, linkTo, linkText }) {
  return (
    <div className="mb-4 flex flex-wrap items-baseline justify-between gap-2">
      <div>
        <h2 className="text-lg font-bold text-slate-100">{title}</h2>
        {description && <p className="mt-1 text-sm text-slate-400">{description}</p>}
      </div>
      {linkTo && linkText && (
        <Link to={linkTo} className="text-sm font-semibold text-emerald-400 hover:text-emerald-300 transition">
          {linkText} &rarr;
        </Link>
      )}
    </div>
  )
}

function Metric({ label, value, accent = false }) {
  return (
    <div className="rounded-lg border border-slate-800 bg-slate-950/50 p-3">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
      <p className={`mt-1 text-2xl font-bold ${accent ? 'text-emerald-400' : 'text-slate-100'}`}>{value}</p>
    </div>
  )
}

// -------------------------------------------------------------
// SECTIONS
// -------------------------------------------------------------

function HubIdentity() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getCurrentManager().then(res => setData(res.data)).catch(() => {}).finally(() => setLoading(false))
  }, [])

  if (loading) return <Loading label="Loading identity..." />
  if (!data) return <ErrorMessage message="Failed to load manager identity." />

  return (
    <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end mb-8">
      <div>
        <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-400">National Team Manager</p>
        <h1 className="mt-2 text-3xl font-bold tracking-tight text-white sm:text-4xl">{data.displayName || data.username || 'Your Career'}</h1>
        <p className="mt-2 text-slate-400">{data.nationality ? `from ${data.nationality}` : ''} • Reputation: {data.reputation?.replace('_', ' ') || 'Unknown'}</p>
      </div>
      <div className="rounded-lg border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-100">
        <span className="text-emerald-300">Level {data.level ?? 0}</span>
        <span className="mx-2 text-emerald-400/60">•</span>
        {data.experiencePoints ?? 0} XP
      </div>
    </div>
  )
}

function HubAttentionAndEvents() {
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getPendingEvents().then(res => setEvents(res.data)).catch(() => {}).finally(() => setLoading(false))
  }, [])

  if (loading) return <Card><Loading label="Checking for pending events..." /></Card>

  if (events.length === 0) {
    return (
      <Card>
        <SectionHeading title="Pending Decisions" />
        <EmptyState title="No decisions require your attention" description="You have no pending managerial events." />
      </Card>
    )
  }

  const critical = events.length

  return (
    <Card className="border-l-4 border-l-amber-500 bg-amber-500/5">
      <SectionHeading 
        title="Needs Your Attention" 
        description={`You have ${critical} pending decision${critical !== 1 ? 's' : ''}.`}
        linkTo="/career"
        linkText="View Events"
      />
      <div className="space-y-3">
        {events.slice(0, 3).map(ev => (
          <div key={ev.id} className="rounded border border-amber-500/20 bg-slate-900/50 p-3 flex justify-between items-center">
            <div>
              <p className="font-bold text-amber-400">{ev.title}</p>
              <p className="text-sm text-slate-300 line-clamp-1">{ev.description}</p>
            </div>
            <Link to="/career" className="text-xs bg-amber-500/20 text-amber-300 px-3 py-1 rounded-full whitespace-nowrap ml-4 hover:bg-amber-500/40 transition">
              Resolve
            </Link>
          </div>
        ))}
      </div>
    </Card>
  )
}

function HubSquadHealth() {
  const [squadData, setSquadData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function fetchSquad() {
      try {
        const { data: jobs } = await getMyJobs()
        const activeJob = jobs.find(j => j.status === 'ACTIVE')
        if (!activeJob) throw new Error('No active squad assigned.')
        
        const { data: squads } = await getMySquads()
        const activeSquad = squads.find(s => s.teamName === activeJob.teamName)
        if (!activeSquad) throw new Error('No active squad configuration.')

        const { data: players } = await getSquadPlayers(activeSquad.id)
        
        const injured = players.filter(p => p.injuryStatus !== 'HEALTHY').length
        const fatigued = players.filter(p => p.fatigue > 75).length
        const highWorkload = players.filter(p => p.workload > 75).length
        const avgFitness = players.length ? Math.round(players.reduce((acc, p) => acc + p.fitness, 0) / players.length) : 0

        setSquadData({
          teamId: activeJob.teamId,
          activeSquad,
          playersCount: players.length,
          injured,
          fatigued,
          highWorkload,
          avgFitness
        })
      } catch (err) {
        setError(err.message || 'Squad not found.')
      } finally {
        setLoading(false)
      }
    }
    fetchSquad()
  }, [])

  if (loading) return <Card><Loading label="Loading squad health..." /></Card>
  if (error) return <Card><SectionHeading title="Squad Health" /><EmptyState title="Unavailable" description={error} /></Card>

  return (
    <Card>
      <SectionHeading title="Squad Health" linkTo={`/teams/${squadData.teamId}/squad`} linkText="Manage Squad" />
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <Metric label="Avg Fitness" value={`${squadData.avgFitness}%`} accent={squadData.avgFitness > 85} />
        <Metric label="Fatigue Risk" value={squadData.fatigued} accent={false} />
        <Metric label="High Workload" value={squadData.highWorkload} accent={false} />
        <Metric label="Injuries" value={squadData.injured} accent={false} />
      </div>
      {(squadData.fatigued > 0 || squadData.highWorkload > 0 || squadData.injured > 0) && (
        <p className="mt-3 text-sm text-red-400 font-semibold uppercase tracking-wide">
          ⚠️ Players require medical attention or rest.
        </p>
      )}
    </Card>
  )
}

function HubEconomy() {
  const [economy, setEconomy] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    axiosClient.get('/manager/economy')
      .then(res => setEconomy(res.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Card><Loading label="Loading resources..." /></Card>
  if (!economy) return <Card><SectionHeading title="Federation Resources" /><ErrorMessage message="Economy data unavailable." /></Card>

  const isLow = economy.balance < 500
  return (
    <Card>
      <SectionHeading title="Federation Resources" linkTo="/career/economy" linkText="Manage Resources" />
      <div className="flex gap-4 items-center mb-4">
        <div className={`p-4 rounded-lg border ${isLow ? 'bg-red-500/10 border-red-500/30' : 'bg-slate-900 border-slate-700'}`}>
          <p className="text-xs font-semibold uppercase text-slate-400">Current Balance</p>
          <p className={`text-3xl font-bold ${isLow ? 'text-red-400' : 'text-emerald-400'}`}>${economy.balance?.toLocaleString()}</p>
        </div>
        <div className="space-y-1 text-sm text-slate-300">
          <p>Training: ${economy.trainingAllocation?.toLocaleString()}</p>
          <p>Medical: ${economy.medicalAllocation?.toLocaleString()}</p>
          <p>Scouting: ${economy.scoutingAllocation?.toLocaleString()}</p>
        </div>
      </div>
      {isLow && <p className="text-sm font-semibold text-red-400">⚠️ Federation resources are critically low.</p>}
    </Card>
  )
}

function HubObjectives() {
  const [objectives, setObjectives] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getObjectives()
      .then(res => setObjectives(res.data.filter(o => o.status === 'ACTIVE' || o.status === 'IN_PROGRESS')))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Card><Loading label="Loading objectives..." /></Card>

  return (
    <Card>
      <SectionHeading title="Current Objectives" linkTo="/career" linkText="View All Objectives" />
      {objectives.length === 0 ? (
        <EmptyState title="No active objectives" description="You do not have any pressing board objectives." />
      ) : (
        <div className="space-y-3">
          {objectives.map(obj => {
            const progress = (obj.currentValue / obj.targetValue) * 100
            return (
              <div key={obj.id} className="p-3 bg-slate-900/50 rounded border border-slate-800">
                <div className="flex justify-between items-end mb-2">
                  <span className="font-semibold text-slate-200">{obj.description.replace(/_/g, ' ')}</span>
                  <span className="text-xs font-bold text-emerald-400">{obj.status}</span>
                </div>
                <div className="h-2 w-full bg-slate-800 rounded-full overflow-hidden">
                  <div className="h-full bg-emerald-500 rounded-full" style={{ width: `${Math.min(100, Math.max(0, progress))}%` }}></div>
                </div>
                <div className="flex justify-between mt-1 text-xs text-slate-400">
                  <span>{obj.currentValue} / {obj.targetValue}</span>
                  <span className="text-amber-400">Reward: ${obj.rewardAmount}</span>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </Card>
  )
}

function HubFixturesAndResults() {
  const [data, setData] = useState({ history: null, activeJob: null })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.allSettled([getCareerHistory(), getMyJobs()]).then(([histRes, jobsRes]) => {
      setData({
        history: histRes.status === 'fulfilled' ? histRes.value.data : [],
        activeJob: jobsRes.status === 'fulfilled' ? jobsRes.value.data.find(j => j.status === 'ACTIVE') : null
      })
      setLoading(false)
    })
  }, [])

  if (loading) return <Card><Loading label="Loading history..." /></Card>

  return (
    <Card>
      <SectionHeading title="Recent Tournaments" />
      {data.history && data.history.length > 0 ? (
        <div className="space-y-3">
          {data.history.slice(0, 4).map(entry => (
            <div key={entry.id} className="p-3 rounded border border-slate-800 bg-slate-950 flex justify-between items-center text-sm">
              <div className="font-semibold text-slate-200">{entry.tournamentName}</div>
              <div className="text-slate-400">Rank {entry.finishingPosition}</div>
            </div>
          ))}
        </div>
      ) : (
        <EmptyState title="No recently completed tournaments" description="You have not completed any tournaments recently." />
      )}
      
      {data.activeJob && (
         <div className="mt-4 pt-4 border-t border-slate-800">
            <h3 className="font-semibold text-slate-300 mb-2">Active Appointment</h3>
            <p className="text-sm text-slate-400">Currently managing <strong className="text-white">{data.activeJob.teamName}</strong>.</p>
            <p className="text-sm text-slate-400 mt-1">Board Confidence: <span className="text-emerald-400">{data.activeJob.boardConfidence?.toFixed(1)}%</span></p>
         </div>
      )}
    </Card>
  )
}

// -------------------------------------------------------------
// MAIN DASHBOARD
// -------------------------------------------------------------

export default function Dashboard() {
  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <HubIdentity />
      
      {/* Top Banner - High Priority */}
      <HubAttentionAndEvents />
      
      {/* Middle Grid - Operational */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <HubSquadHealth />
        <HubEconomy />
      </div>
      
      {/* Lower Grid - General & Career */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <HubObjectives />
        <HubFixturesAndResults />
      </div>

      {/* Quick Actions Footer */}
      <div className="pt-6 border-t border-slate-800 flex flex-wrap gap-3">
        <Link to="/career" className="rounded-lg bg-slate-800 px-4 py-2 text-sm font-semibold text-slate-100 hover:bg-slate-700 transition">View Full Career</Link>
        <Link to="/teams" className="rounded-lg bg-slate-800 px-4 py-2 text-sm font-semibold text-slate-100 hover:bg-slate-700 transition">National Teams</Link>
        <Link to="/tournaments" className="rounded-lg bg-slate-800 px-4 py-2 text-sm font-semibold text-slate-100 hover:bg-slate-700 transition">Tournaments</Link>
      </div>
    </div>
  )
}
