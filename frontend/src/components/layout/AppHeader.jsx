import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import Button from '../common/Button'

const primaryLink = ({ isActive }) => `rounded-md px-3 py-2 transition ${isActive ? 'bg-emerald-500/10 text-emerald-300' : 'text-slate-400 hover:text-slate-100'}`
const accountLink = ({ isActive }) => `text-sm font-semibold ${isActive ? 'text-emerald-300' : 'text-slate-300 hover:text-white'}`

export default function AppHeader() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const handleLogout = () => { logout(); navigate('/login', { replace: true }) }
  return <header className="border-b border-slate-800 bg-slate-950/90 px-4 py-4 backdrop-blur sm:px-6"><div className="mx-auto flex max-w-7xl flex-col gap-4 xl:flex-row xl:items-center xl:justify-between"><div className="min-w-0 xl:flex xl:flex-1 xl:items-center xl:gap-6"><NavLink to="/dashboard" className="text-lg font-bold tracking-tight text-white">World Cup <span className="text-emerald-400">Manager</span></NavLink><nav aria-label="Primary navigation" className="mt-3 flex w-full max-w-full gap-1 overflow-x-auto pb-1 text-sm font-medium xl:mt-0 xl:w-auto xl:overflow-visible xl:pb-0"><NavLink to="/dashboard" className={primaryLink}>Dashboard</NavLink><NavLink to="/teams" className={primaryLink}>Teams</NavLink><NavLink to="/tournaments" className={primaryLink}>Tournaments</NavLink><NavLink to="/statistics" className={primaryLink}>Statistics</NavLink><NavLink to="/history" className={primaryLink}>History</NavLink><NavLink to="/saves" className={primaryLink}>Saves</NavLink></nav></div><div className="flex flex-wrap items-center gap-3"><span className="max-w-40 truncate text-sm text-slate-400 sm:max-w-none">{user?.email}</span><NavLink to="/profile" className={accountLink}>Profile</NavLink><NavLink to="/career" className={accountLink}>Career</NavLink><NavLink to="/achievements" className={accountLink}>Achievements</NavLink><NavLink to="/leaderboards/managers" className={accountLink}>Rankings</NavLink><NavLink to="/settings" className={accountLink}>Settings</NavLink><Button variant="secondary" onClick={handleLogout}>Log out</Button></div></div></header>
}
