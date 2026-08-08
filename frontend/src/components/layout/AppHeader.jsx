import { NavLink } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import Button from '../common/Button'

export default function AppHeader() {
  const { user, logout } = useAuth()

  return (
    <header className="border-b border-slate-800 bg-slate-950/90 px-4 py-4 backdrop-blur sm:px-6">
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-4">
        <NavLink to="/dashboard" className="text-lg font-bold tracking-tight text-white">World Cup <span className="text-emerald-400">Manager</span></NavLink>
        <div className="flex items-center gap-3"><span className="hidden text-sm text-slate-400 sm:inline">{user?.email}</span><Button variant="secondary" onClick={logout}>Log out</Button></div>
      </div>
    </header>
  )
}
