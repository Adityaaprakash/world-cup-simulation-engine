import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './layouts/AppLayout'
import Dashboard from './pages/Dashboard'
import Login from './pages/Login'
import Register from './pages/Register'
import TeamSelection from './pages/TeamSelection'
import Squad from './pages/Squad'
import LineupBuilder from './pages/LineupBuilder'
import TournamentSelection from './pages/TournamentSelection'
import TournamentDashboard from './pages/TournamentDashboard'
import TournamentGroups from './pages/TournamentGroups'
import TournamentKnockout from './pages/TournamentKnockout'
import TournamentSummary from './pages/TournamentSummary'
import MatchCentre from './pages/MatchCentre'
import Statistics from './pages/Statistics'
import Historical from './pages/Historical'
import Saves from './pages/Saves'
import Profile from './pages/Profile'
import Settings from './pages/Settings'
import ProtectedRoute from './routes/ProtectedRoute'

export default function App() {
  return <Routes><Route path="/login" element={<Login />} /><Route path="/register" element={<Register />} /><Route element={<ProtectedRoute />}><Route element={<AppLayout />}><Route path="/dashboard" element={<Dashboard />} /><Route path="/teams" element={<TeamSelection />} /><Route path="/teams/:teamId/squad" element={<Squad />} /><Route path="/teams/:teamId/lineup" element={<LineupBuilder />} /><Route path="/tournaments" element={<TournamentSelection />} /><Route path="/tournaments/:tournamentId" element={<TournamentDashboard />} /><Route path="/tournaments/:tournamentId/groups" element={<TournamentGroups />} /><Route path="/tournaments/:tournamentId/knockout" element={<TournamentKnockout />} /><Route path="/tournaments/:tournamentId/summary" element={<TournamentSummary />} /><Route path="/matches/:matchId" element={<MatchCentre />} /><Route path="/statistics" element={<Statistics />} /><Route path="/history" element={<Historical />} /><Route path="/history/:section" element={<Historical />} /><Route path="/saves" element={<Saves />} /><Route path="/profile" element={<Profile />} /><Route path="/settings" element={<Settings />} /></Route></Route><Route path="*" element={<Navigate to="/dashboard" replace />} /></Routes>
}
