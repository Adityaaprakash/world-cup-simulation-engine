import { formatLabel } from '../../utils/football'

const statusStyles = {
  UPCOMING: 'border-sky-400/40 bg-sky-400/10 text-sky-200',
  IN_PROGRESS: 'border-emerald-400/40 bg-emerald-400/10 text-emerald-200',
  COMPLETED: 'border-violet-400/40 bg-violet-400/10 text-violet-200',
  ARCHIVED: 'border-slate-600 bg-slate-800 text-slate-300',
  SCHEDULED: 'border-sky-400/40 bg-sky-400/10 text-sky-200',
  LIVE: 'border-rose-400/40 bg-rose-400/10 text-rose-200',
  FINISHED: 'border-slate-600 bg-slate-800 text-slate-200',
}

export default function StatusBadge({ status }) {
  return <span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-bold uppercase tracking-wide ${statusStyles[status] || statusStyles.ARCHIVED}`}>{formatLabel(status)}</span>
}
