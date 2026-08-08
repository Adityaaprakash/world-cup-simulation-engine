import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { loginRequest, registerRequest } from '../api/authApi'
import { TOKEN_STORAGE_KEY } from '../api/axiosClient'

const AuthContext = createContext(null)

function readTokenUser(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    if (!payload.sub || (payload.exp && payload.exp * 1000 <= Date.now())) return null
    return { email: payload.sub }
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [isLoading, setIsLoading] = useState(true)

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    setUser(null)
  }, [])

  const restoreSession = useCallback(() => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY)
    const restoredUser = token && readTokenUser(token)
    if (!restoredUser) localStorage.removeItem(TOKEN_STORAGE_KEY)
    setUser(restoredUser)
    setIsLoading(false)
    return restoredUser
  }, [])

  const completeAuthentication = useCallback((token) => {
    const authenticatedUser = readTokenUser(token)
    if (!authenticatedUser) throw new Error('The server returned an invalid authentication token.')
    localStorage.setItem(TOKEN_STORAGE_KEY, token)
    setUser(authenticatedUser)
    return authenticatedUser
  }, [])

  const login = useCallback(async (credentials) => {
    const { data } = await loginRequest(credentials)
    return completeAuthentication(data.token)
  }, [completeAuthentication])

  const register = useCallback(async (details) => {
    const { data } = await registerRequest(details)
    return completeAuthentication(data.token)
  }, [completeAuthentication])

  useEffect(() => {
    restoreSession()
    window.addEventListener('auth:unauthorized', logout)
    return () => window.removeEventListener('auth:unauthorized', logout)
  }, [logout, restoreSession])

  const value = useMemo(() => ({ user, isAuthenticated: Boolean(user), isLoading, login, register, logout, restoreSession }), [user, isLoading, login, register, logout, restoreSession])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
