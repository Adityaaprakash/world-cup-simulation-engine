import React, { useEffect, useState } from 'react'
import { getObjectives } from '../../api/managerApi'
import Card from '../common/Card'
import Loading from '../common/Loading'
import ErrorMessage from '../common/ErrorMessage'

export default function ManagerObjectives() {
  const [objectives, setObjectives] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true
    getObjectives()
      .then(({ data }) => {
        if (active) setObjectives(data)
      })
      .catch((err) => {
        if (active) setError(err.message || 'Failed to load objectives.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  if (loading) return <Loading label="Loading objectives..." />
  if (error) return <ErrorMessage message={error} />

  if (!objectives || objectives.length === 0) {
    return (
      <Card className="p-6 text-center">
        <p className="text-slate-400">You currently have no active objectives.</p>
      </Card>
    )
  }

  return (
    <Card className="p-6">
      <h3 className="mb-4 text-lg font-bold text-emerald-400 uppercase tracking-widest">Career Objectives</h3>
      <div className="space-y-4">
        {objectives.map((obj) => {
          const progressPercentage = Math.max(0, Math.min(100, Math.round((obj.currentValue / obj.targetValue) * 100)))
          let statusBadge = null
          switch (obj.status) {
            case 'COMPLETED':
              statusBadge = <span className="ml-2 inline-flex items-center rounded-full bg-emerald-400/10 px-2 py-1 text-xs font-medium text-emerald-400">Completed</span>
              break
            case 'FAILED':
              statusBadge = <span className="ml-2 inline-flex items-center rounded-full bg-red-400/10 px-2 py-1 text-xs font-medium text-red-400">Failed</span>
              break
            case 'CANCELLED':
              statusBadge = <span className="ml-2 inline-flex items-center rounded-full bg-slate-400/10 px-2 py-1 text-xs font-medium text-slate-400">Cancelled</span>
              break
            case 'EXPIRED':
              statusBadge = <span className="ml-2 inline-flex items-center rounded-full bg-orange-400/10 px-2 py-1 text-xs font-medium text-orange-400">Expired</span>
              break
            default:
              statusBadge = <span className="ml-2 inline-flex items-center rounded-full bg-blue-400/10 px-2 py-1 text-xs font-medium text-blue-400">Active</span>
              break
          }

          return (
            <div key={obj.id} className="rounded-lg border border-slate-700 bg-slate-800/50 p-4">
              <div className="flex items-start justify-between">
                <div>
                  <h4 className="font-semibold text-slate-200">
                    {obj.description}
                    {statusBadge}
                  </h4>
                  <p className="mt-1 text-sm text-slate-400">
                    Reward: <span className="text-yellow-400 font-medium">+{obj.rewardAmount} Resources</span>
                  </p>
                  {obj.tournamentName && (
                    <p className="mt-1 text-xs text-slate-500">Tournament: {obj.tournamentName}</p>
                  )}
                </div>
                <div className="text-right">
                  <p className="text-sm font-medium text-slate-300">
                    {obj.currentValue} / {obj.targetValue}
                  </p>
                  <p className="text-xs text-slate-500 uppercase tracking-wider">{obj.type.replace(/_/g, ' ')}</p>
                </div>
              </div>
              <div className="mt-4 h-2 w-full overflow-hidden rounded-full bg-slate-700">
                <div
                  className={`h-full ${obj.status === 'COMPLETED' ? 'bg-emerald-500' : obj.status === 'FAILED' ? 'bg-red-500' : 'bg-blue-500'}`}
                  style={{ width: `${progressPercentage}%` }}
                />
              </div>
            </div>
          )
        })}
      </div>
    </Card>
  )
}
