import { Link } from 'react-router-dom'
import Card from '../common/Card'
import EmptyState from '../common/EmptyState'

const date = (value) => value ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(value)) : '—'

export default function CareerHistory({ entries }) {
  if (!entries?.length) return <EmptyState title="No career history yet." description="Completed tournament campaigns will appear here." />
  return <Card><h2 className="text-xl font-bold text-white">Tournament history</h2><p className="mt-1 text-sm text-slate-400">Completed tournament campaigns supplied by the backend.</p><div className="mt-5 overflow-x-auto"><table className="min-w-[46rem] w-full text-sm"><thead className="border-b border-slate-800 text-left text-xs uppercase tracking-wide text-slate-500"><tr><th className="p-3">Tournament</th><th className="p-3">Team</th><th className="p-3">Finished</th><th className="p-3">Position</th><th className="p-3">Wins</th><th className="p-3">Goals</th><th className="p-3">Conceded</th><th className="p-3">Trophies</th></tr></thead><tbody>{entries.map((entry) => <tr key={entry.id} className="border-b border-slate-800/80"><td className="p-3 font-semibold text-slate-100">{entry.tournamentId != null ? <Link to={`/tournaments/${entry.tournamentId}`} className="hover:text-emerald-300">{entry.tournamentName}</Link> : entry.tournamentName}</td><td className="p-3">{entry.teamId != null ? <Link to={`/teams/${entry.teamId}/squad`} className="text-emerald-300 hover:text-emerald-200">{entry.teamName}</Link> : entry.teamName}</td><td className="p-3 text-slate-400">{date(entry.dateCompleted)}</td><td className="p-3">{entry.finishingPosition ?? '—'}</td><td className="p-3">{entry.wins ?? '—'}</td><td className="p-3">{entry.goalsScored ?? '—'}</td><td className="p-3">{entry.goalsConceded ?? '—'}</td><td className="p-3 font-semibold text-emerald-300">{entry.trophies ?? '—'}</td></tr>)}</tbody></table></div></Card>
}
