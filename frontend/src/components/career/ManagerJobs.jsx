import React from 'react'

export default function ManagerJobs({ jobs }) {
  if (!jobs || jobs.length === 0) {
    return (
      <div className="rounded-xl border border-slate-700/50 bg-slate-800/20 p-8 text-center text-sm text-slate-400">
        You have no recorded manager jobs. Accept a job in the Team Selection or Profile screen.
      </div>
    )
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'ACTIVE': return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
      case 'SACKED': return 'bg-red-500/10 text-red-400 border-red-500/20'
      case 'RESIGNED': return 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20'
      default: return 'bg-slate-500/10 text-slate-400 border-slate-500/20'
    }
  }

  const getObjectiveLabel = (obj) => {
    switch (obj) {
      case 'WIN_TOURNAMENT': return 'Win Tournament'
      case 'REACH_FINAL': return 'Reach Final'
      case 'REACH_SEMI_FINAL': return 'Semi-Finals'
      case 'REACH_QUARTER_FINAL': return 'Quarter-Finals'
      case 'KNOCKOUT_STAGE': return 'Reach Knockouts'
      case 'AVOID_BOTTOM': return 'Avoid Bottom'
      case 'WIN_MATCH': return 'Win Match'
      default: return obj
    }
  }

  return (
    <div className="space-y-4">
      {jobs.map((job) => (
        <div key={job.id} className="rounded-xl border border-slate-700/50 bg-slate-800/20 p-6 flex flex-col md:flex-row gap-6 items-start md:items-center justify-between">
          <div>
            <div className="flex items-center gap-3">
              <h3 className="text-xl font-bold text-slate-100">{job.teamName}</h3>
              <span className={`px-2 py-0.5 rounded-full text-xs font-semibold border ${getStatusColor(job.status)}`}>
                {job.status}
              </span>
            </div>
            
            <div className="mt-2 text-sm text-slate-400 space-y-1">
              <p>Objective: <span className="text-slate-200">{getObjectiveLabel(job.boardObjective)}</span></p>
              <p>Hired: <span className="text-slate-200">{new Date(job.hiredAt).toLocaleDateString()}</span></p>
              {job.endedAt && <p>Ended: <span className="text-slate-200">{new Date(job.endedAt).toLocaleDateString()}</span></p>}
            </div>
          </div>
          
          <div className="bg-slate-900/50 rounded-xl p-4 min-w-[200px] border border-slate-700/50">
            <div className="text-sm font-semibold uppercase tracking-wider text-slate-400 mb-2 text-center">Board Confidence</div>
            <div className="relative h-4 bg-slate-800 rounded-full overflow-hidden">
              <div 
                className={`absolute top-0 left-0 h-full rounded-full transition-all duration-1000 ${
                  job.boardConfidence >= 60 ? 'bg-emerald-500' :
                  job.boardConfidence >= 30 ? 'bg-yellow-500' : 'bg-red-500'
                }`}
                style={{ width: `${Math.max(0, Math.min(100, job.boardConfidence))}%` }}
              />
            </div>
            <div className="mt-2 text-center text-xl font-bold text-slate-200">
              {job.boardConfidence.toFixed(1)}%
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}
