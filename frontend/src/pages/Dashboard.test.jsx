import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import '@testing-library/jest-dom'
import { MemoryRouter } from 'react-router-dom'
import Dashboard from './Dashboard'
import {
  getCurrentManager,
  getCareerHistory,
  getPendingEvents,
  getObjectives,
  getMyJobs
} from '../api/managerApi'
import { getMySquads, getSquadPlayers } from '../api/squadApi'
import axiosClient from '../api/axiosClient'

vi.mock('../api/managerApi', () => ({
  getCurrentManager: vi.fn(),
  getCareerHistory: vi.fn(),
  getPendingEvents: vi.fn(),
  getObjectives: vi.fn(),
  getMyJobs: vi.fn(),
}))

vi.mock('../api/squadApi', () => ({
  getMySquads: vi.fn(),
  getSquadPlayers: vi.fn(),
}))

vi.mock('../api/axiosClient', () => ({
  default: {
    get: vi.fn()
  }
}))

describe('Dashboard Component - Manager Hub', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    getCurrentManager.mockResolvedValue({
      data: { displayName: 'John Doe', nationality: 'Canada', level: 10, experiencePoints: 500 }
    })
    
    getPendingEvents.mockResolvedValue({
      data: [
        { id: 1, title: 'Press Conference', description: 'Address the media', status: 'PENDING' }
      ]
    })
    
    getObjectives.mockResolvedValue({
      data: [
        { id: 1, description: 'WIN_MATCH', status: 'ACTIVE', currentValue: 0, targetValue: 1, rewardAmount: 100 }
      ]
    })
    
    axiosClient.get.mockResolvedValue({
      data: { balance: 1000, trainingAllocation: 200, medicalAllocation: 100, scoutingAllocation: 100 }
    })
    
    getMyJobs.mockResolvedValue({
      data: [{ teamName: 'Canada', status: 'ACTIVE', boardConfidence: 85 }]
    })
    
    getMySquads.mockResolvedValue({
      data: [{ id: 101, teamName: 'Canada' }]
    })
    
    getSquadPlayers.mockResolvedValue({
      data: [
        { id: 1, name: 'Player A', injuryStatus: 'HEALTHY', fatigue: 20, workload: 30, fitness: 90 },
        { id: 2, name: 'Player B', injuryStatus: 'INJURED', fatigue: 80, workload: 80, fitness: 40 }
      ]
    })
    
    getCareerHistory.mockResolvedValue({
      data: [{ id: 1, tournamentName: 'World Cup 2026', finishingPosition: 4 }]
    })
  })

  test('renders Manager Hub with all sections successfully', async () => {
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    )

    // Wait for the components to load
    await waitFor(() => {
      // Identity
      expect(screen.getByText('John Doe')).toBeInTheDocument()
      expect(screen.getByText('Level 10')).toBeInTheDocument()
      
      // Events 
      expect(screen.getByText('Needs Your Attention')).toBeInTheDocument()
      expect(screen.getByText('Press Conference')).toBeInTheDocument()
      
      // Squad Health
      expect(screen.getByText('Squad Health')).toBeInTheDocument()
      expect(screen.getByText('Avg Fitness')).toBeInTheDocument()
      expect(screen.getByText('⚠️ Players require medical attention or rest.')).toBeInTheDocument()
      
      // Economy
      expect(screen.getByText('Federation Resources')).toBeInTheDocument()
      expect(screen.getByText('$1,000')).toBeInTheDocument()
      
      // Objectives
      expect(screen.getByText('Current Objectives')).toBeInTheDocument()
      expect(screen.getByText('WIN MATCH')).toBeInTheDocument()
      
      // Fixtures/Results/Career
      expect(screen.getByText('Recent Tournaments')).toBeInTheDocument()
      expect(screen.getByText('World Cup 2026')).toBeInTheDocument()
      expect(screen.getByText('Canada')).toBeInTheDocument() 
    })
  })
  
  test('renders empty states when there is no data', async () => {
    getPendingEvents.mockResolvedValue({ data: [] })
    getObjectives.mockResolvedValue({ data: [] })
    getCareerHistory.mockResolvedValue({ data: [] })
    
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    )
    
    await waitFor(() => {
      expect(screen.getByText('No decisions require your attention')).toBeInTheDocument()
      expect(screen.getByText('No active objectives')).toBeInTheDocument()
      expect(screen.getByText('No recently completed tournaments')).toBeInTheDocument()
    })
  })

  test('renders section-level error states', async () => {
    getMyJobs.mockRejectedValue(new Error('Job fetch failed'))
    axiosClient.get.mockRejectedValue(new Error('Economy fetch failed'))
    
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    )
    
    await waitFor(() => {
      expect(screen.getByText('Job fetch failed')).toBeInTheDocument()
      expect(screen.getByText('Economy data unavailable.')).toBeInTheDocument()
    })
  })
})
