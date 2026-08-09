import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import Button from '../components/common/Button'
import Card from '../components/common/Card'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import { useAuth } from '../context/AuthContext'

export default function Login() {
  const { isAuthenticated, isLoading, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (isLoading) return <AuthPage><Loading label="Restoring your session..." /></AuthPage>
  if (isAuthenticated) return <Navigate to="/dashboard" replace />

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      await login(form)
      navigate(location.state?.from?.pathname || '/dashboard', { replace: true })
    } catch (requestError) {
      setError(requestError.message || 'Unable to sign in.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return <AuthPage><Card><h1 className="text-2xl font-bold">Welcome back</h1><p className="mt-2 text-sm text-slate-400">Sign in to continue your management career.</p><form className="mt-6 space-y-4" onSubmit={submit}><Field label="Email" type="email" value={form.email} onChange={(email) => setForm({ ...form, email })} /><Field label="Password" type="password" value={form.password} onChange={(password) => setForm({ ...form, password })} /><ErrorMessage message={error} /><Button type="submit" className="w-full" disabled={isSubmitting}>{isSubmitting ? 'Signing in...' : 'Sign in'}</Button></form><p className="mt-5 text-center text-sm text-slate-400">New here? <Link to="/register" className="font-semibold text-emerald-400 hover:text-emerald-300">Create an account</Link></p></Card></AuthPage>
}

export function AuthPage({ children }) { return <main className="flex min-h-screen items-center justify-center bg-[radial-gradient(ellipse_at_top,_var(--color-emerald-950),_var(--color-slate-950)_55%)] px-4 text-slate-100"><div className="w-full max-w-md">{children}</div></main> }
export function Field({ label, type, value, onChange, ...props }) { return <label className="block text-sm font-medium text-slate-200">{label}<input required type={type} value={value} onChange={(event) => onChange(event.target.value)} className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-white outline-none transition placeholder:text-slate-600 focus:border-emerald-400 focus:ring-2 focus:ring-emerald-400/20" {...props} /></label> }
