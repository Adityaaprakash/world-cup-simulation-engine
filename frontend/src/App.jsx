import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './layouts/AppLayout'
import Dashboard from './pages/Dashboard'
import Login from './pages/Login'
import Register from './pages/Register'
import ProtectedRoute from './routes/ProtectedRoute'

export default function App() {
  return <Routes><Route path="/login" element={<Login />} /><Route path="/register" element={<Register />} /><Route element={<ProtectedRoute />}><Route element={<AppLayout />}><Route path="/dashboard" element={<Dashboard />} /></Route></Route><Route path="*" element={<Navigate to="/dashboard" replace />} /></Routes>
}
