import Card from '../components/common/Card'
import EmptyState from '../components/common/EmptyState'
import { useAuth } from '../context/AuthContext'

const placeholders = [
  ['Manager Career', 'Career progress and manager reputation will appear here.'],
  ['Current Tournament', 'Your active tournament overview will appear here.'],
  ['Current Team', 'Your selected national team will appear here.'],
  ['Recent Matches', 'Recent results and match summaries will appear here.'],
]

export default function Dashboard() {
  const { user } = useAuth()
  return <div><div className="mb-8"><p className="text-sm font-semibold uppercase tracking-widest text-emerald-400">Command Centre</p><h1 className="mt-2 text-3xl font-bold">Welcome, {user?.email}</h1><p className="mt-2 text-slate-400">Your World Cup management workspace is ready for the next phase.</p></div><div className="grid gap-5 md:grid-cols-2">{placeholders.map(([title, description]) => <Card key={title}><EmptyState title={title} description={description} /></Card>)}</div></div>
}
