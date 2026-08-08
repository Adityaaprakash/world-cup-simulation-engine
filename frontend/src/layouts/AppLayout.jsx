import { Outlet } from 'react-router-dom'
import AppHeader from '../components/layout/AppHeader'

export default function AppLayout() {
  return <div className="min-h-screen bg-slate-950 text-slate-100"><AppHeader /><main className="mx-auto max-w-7xl px-4 py-8 sm:px-6"><Outlet /></main></div>
}
