import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import Button from '../components/common/Button'
import Card from '../components/common/Card'
import ErrorMessage from '../components/common/ErrorMessage'
import { useAuth } from '../context/AuthContext'
import { AuthPage, Field } from './Login'

export default function Register() {
  const { isAuthenticated, register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  if (isAuthenticated) return <Navigate to="/dashboard" replace />
  const submit = async (event) => { event.preventDefault(); setError(''); setIsSubmitting(true); try { await register(form); navigate('/dashboard', { replace: true }) } catch (requestError) { setError(requestError.message || 'Unable to create the account.') } finally { setIsSubmitting(false) } }
  return <AuthPage><Card><h1 className="text-2xl font-bold">Create your manager profile</h1><p className="mt-2 text-sm text-slate-400">Your account is the starting point for future career phases.</p><form className="mt-6 space-y-4" onSubmit={submit}><Field label="Username" type="text" value={form.username} onChange={(username) => setForm({ ...form, username })} /><Field label="Email" type="email" value={form.email} onChange={(email) => setForm({ ...form, email })} /><Field label="Password" type="password" value={form.password} onChange={(password) => setForm({ ...form, password })} /><ErrorMessage message={error} /><Button type="submit" className="w-full" disabled={isSubmitting}>{isSubmitting ? 'Creating account...' : 'Create account'}</Button></form><p className="mt-5 text-center text-sm text-slate-400">Already registered? <Link to="/login" className="font-semibold text-emerald-400 hover:text-emerald-300">Sign in</Link></p></Card></AuthPage>
}
