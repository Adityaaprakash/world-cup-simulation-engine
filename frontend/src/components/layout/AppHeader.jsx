import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import Button from '../common/Button'

export default function AppHeader() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="border-b border-slate-800 bg-slate-950/90 px-4 py-4 backdrop-blur sm:px-6">
      <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-6">
          <NavLink to="/dashboard" className="text-lg font-bold tracking-tight text-white">World Cup <span className="text-emerald-400">Manager</span></NavLink>
          <nav aria-label="Primary navigation" className="flex items-center gap-1 text-sm font-medium">
            <NavLink to="/dashboard" className={({ isActive }) => `rounded-md px-3 py-2 transition ${isActive ? 'bg-emerald-500/10 text-emerald-300' : 'text-slate-400 hover:text-slate-100'}`}>Dashboard</NavLink>
            <NavLink to="/teams" className={({ isActive }) => `rounded-md px-3 py-2 transition ${isActive ? 'bg-emerald-500/10 text-emerald-300' : 'text-slate-400 hover:text-slate-100'}`}>Teams</NavLink>
            <NavLink to="/tournaments" className={({ isActive }) => `rounded-md px-3 py-2 transition ${isActive ? 'bg-emerald-500/10 text-emerald-300' : 'text-slate-400 hover:text-slate-100'}`}>Tournaments</NavLink>
            <NavLink to="/statistics" className={({ isActive }) => `rounded-md px-3 py-2 transition ${isActive ? 'bg-emerald-500/10 text-emerald-300' : 'text-slate-400 hover:text-slate-100'}`}>Statistics</NavLink>
            <NavLink to="/history" className={({ isActive }) => `rounded-md px-3 py-2 transition ${isActive ? 'bg-emerald-500/10 text-emerald-300' : 'text-slate-400 hover:text-slate-100'}`}>History</NavLink>
          </nav>
        </div>
        <div className="flex items-center gap-3"><span className="max-w-40 truncate text-sm text-slate-400 sm:max-w-none">{user?.email}</span><Button variant="secondary" onClick={handleLogout}>Log out</Button></div>
      </div>
    </header>
  )
}
