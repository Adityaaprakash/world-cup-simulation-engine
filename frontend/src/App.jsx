import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './layouts/AppLayout'
import Dashboard from './pages/Dashboard'
import Login from './pages/Login'
import Register from './pages/Register'
import TeamSelection from './pages/TeamSelection'
import Squad from './pages/Squad'
import LineupBuilder from './pages/LineupBuilder'
import ProtectedRoute from './routes/ProtectedRoute'

export default function App() {
  return <Routes><Route path="/login" element={<Login />} /><Route path="/register" element={<Register />} /><Route element={<ProtectedRoute />}><Route element={<AppLayout />}><Route path="/dashboard" element={<Dashboard />} /><Route path="/teams" element={<TeamSelection />} /><Route path="/teams/:teamId/squad" element={<Squad />} /><Route path="/teams/:teamId/lineup" element={<LineupBuilder />} /></Route></Route><Route path="*" element={<Navigate to="/dashboard" replace />} /></Routes>
}
