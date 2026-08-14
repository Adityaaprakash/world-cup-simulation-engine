import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getCurrentManager } from '../api/managerApi'
import Button from '../components/common/Button'
import Card from '../components/common/Card'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import { useAuth } from '../context/AuthContext'

const REDUCE_MOTION_KEY = 'world-cup-reduce-motion'

export default function Settings() {
  const { user, logout } = useAuth(); const navigate = useNavigate(); const [manager, setManager] = useState(null); const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [reduceMotion, setReduceMotion] = useState(() => localStorage.getItem(REDUCE_MOTION_KEY) === 'true')
  useEffect(() => { getCurrentManager().then(({ data }) => setManager(data)).catch((requestError) => setError(requestError.message || 'Unable to load account information.')).finally(() => setLoading(false)) }, [])
  useEffect(() => { localStorage.setItem(REDUCE_MOTION_KEY, String(reduceMotion)); document.documentElement.dataset.reduceMotion = String(reduceMotion) }, [reduceMotion])
  const handleLogout = () => { logout(); navigate('/login', { replace: true }) }
  return <div className="space-y-7"><div><p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-400">Account</p><h1 className="mt-2 text-3xl font-bold text-white">Settings</h1><p className="mt-2 text-slate-400">Account information and local application preferences.</p></div><ErrorMessage message={error} /><div className="grid gap-6 xl:grid-cols-2"><Card><h2 className="text-xl font-bold text-white">Account</h2>{loading ? <Loading label="Loading account information..." /> : <dl className="mt-4 space-y-3 text-sm"><div><dt className="text-slate-500">Email</dt><dd className="mt-1 font-semibold text-slate-100">{user?.email || '—'}</dd></div><div><dt className="text-slate-500">Manager username</dt><dd className="mt-1 font-semibold text-slate-100">{manager?.username || '—'}</dd></div></dl>}<p className="mt-5 text-sm text-slate-400">Profile, username, and email updates are not currently exposed by the backend.</p></Card><Card><h2 className="text-xl font-bold text-white">Security</h2><p className="mt-3 text-sm text-slate-400">Password changes and account deletion are not currently exposed by the backend.</p><div className="mt-6"><Button variant="danger" onClick={handleLogout}>Log out</Button></div></Card><Card><h2 className="text-xl font-bold text-white">Application preferences</h2><p className="mt-1 text-sm text-slate-400">These preferences stay in this browser only and are not saved to your account.</p><label className="mt-5 flex cursor-pointer items-center justify-between gap-4 rounded-lg border border-slate-800 bg-slate-950/50 p-4"><span><span className="block font-semibold text-slate-100">Reduce animations</span><span className="mt-1 block text-sm text-slate-400">Minimise interface animation and transitions.</span></span><input type="checkbox" checked={reduceMotion} onChange={(event) => setReduceMotion(event.target.checked)} className="h-5 w-5 accent-emerald-400" /></label></Card></div></div>
}
